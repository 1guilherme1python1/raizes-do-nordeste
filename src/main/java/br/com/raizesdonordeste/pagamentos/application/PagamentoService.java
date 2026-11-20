package br.com.raizesdonordeste.pagamentos.application;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Clock;
import java.util.HexFormat;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import br.com.raizesdonordeste.estoque.application.EstoqueService;
import br.com.raizesdonordeste.estoque.application.EstoqueService.ReservaItem;
import br.com.raizesdonordeste.pedidos.application.AtorPedido;
import br.com.raizesdonordeste.pedidos.domain.StatusPedido;
import br.com.raizesdonordeste.pedidos.infrastructure.persistence.*;
import br.com.raizesdonordeste.shared.application.*;
import br.com.raizesdonordeste.unidades.application.AcessoUnidadeService;
import br.com.raizesdonordeste.usuarios.domain.Perfil;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PagamentoService {
    private final PedidoJpaRepository pedidos; private final EstoqueService estoque; private final AcessoUnidadeService acesso;
    private final GatewayPagamento gateway; private final JdbcTemplate jdbc; private final Clock clock;
    public PagamentoService(PedidoJpaRepository pedidos, EstoqueService estoque, AcessoUnidadeService acesso,
            GatewayPagamento gateway, JdbcTemplate jdbc, Clock clock) {
        this.pedidos=pedidos; this.estoque=estoque; this.acesso=acesso; this.gateway=gateway; this.jdbc=jdbc; this.clock=clock;
    }
    @Transactional
    public Resultado solicitar(UUID id, AtorPedido ator, String chave, String cenario) {
        if (chave==null || chave.isBlank() || chave.length()>100) throw new IllegalArgumentException("Idempotency-Key inválida");
        if (!List.of("APROVAR","RECUSAR").contains(cenario)) throw new IllegalArgumentException("Cenário inválido");
        var p=pedidos.findForUpdateById(id).orElseThrow(()->new RecursoNaoEncontradoException("Pedido"));
        autorizar(p,ator);
        var hash=hash(cenario);
        var registros=jdbc.query("select id,status,motivo_resultado,requisicao_hash from app.pagamento where pedido_id=? and solicitante_id=? and idempotency_key=?",
                (rs,n)->new Registro(rs.getObject(1,UUID.class),rs.getString(2),rs.getString(3),rs.getString(4)), id,ator.usuarioId(),chave);
        if(!registros.isEmpty()) {
            var anterior=registros.getFirst();
            if(!hash.equals(anterior.hash())) throw new ConflitoException("Idempotency-Key já utilizada com conteúdo diferente");
            return new Resultado(anterior.id(),anterior.status(),p.getStatus().name(),anterior.motivo(),true,false);
        }
        if(p.getStatus()!=StatusPedido.AGUARDANDO_PAGAMENTO || p.getReservaExpiraEm()==null) throw new ConflitoException("Pedido não elegível para pagamento");
        var itens=p.getItens().stream().map(i->new ReservaItem(i.getId(),i.getProdutoId(),i.getQuantidade())).toList();
        var agora=clock.instant();
        if(!agora.isBefore(p.getReservaExpiraEm())) {
            estoque.liberarPedido(p.getUnidadeId(),null,p.getId(),itens);
            transicao(p,null,StatusPedido.EXPIRADO,"SISTEMA","RESERVA_EXPIRADA");
            return new Resultado(null,null,"EXPIRADO","RESERVA_EXPIRADA",false,true);
        }
        var resposta=gateway.processar(id,p.getValorTotal(),p.getMoeda(),cenario);
        var pagamentoId=UUID.randomUUID();
        jdbc.update("insert into app.pagamento (id,pedido_id,numero_tentativa,status,transacao_id,solicitado_em,resultado_em,motivo_resultado,criado_em,atualizado_em,solicitante_id,idempotency_key,requisicao_hash,valor,moeda) values (?,?,1,?,?,?,?,?,?,?,?,?,?,?,?)",
                pagamentoId,id,resposta.aprovado()?"APROVADO":"RECUSADO",resposta.transacaoId(),java.sql.Timestamp.from(agora),java.sql.Timestamp.from(agora),resposta.motivo(),java.sql.Timestamp.from(agora),java.sql.Timestamp.from(agora),ator.usuarioId(),chave,hash,p.getValorTotal(),p.getMoeda());
        if(resposta.aprovado()) estoque.confirmarPedido(p.getUnidadeId(),ator.usuarioId(),id,itens);
        else estoque.liberarPedido(p.getUnidadeId(),ator.usuarioId(),id,itens);
        transicao(p,ator.usuarioId(),resposta.aprovado()?StatusPedido.CONFIRMADO:StatusPedido.CANCELADO,"USUARIO",resposta.motivo());
        return new Resultado(pagamentoId,resposta.aprovado()?"APROVADO":"RECUSADO",p.getStatus().name(),resposta.motivo(),false,false);
    }
    @Transactional(readOnly=true)
    public List<Resultado> consultar(UUID id,AtorPedido ator){
        var p=pedidos.findById(id).orElseThrow(()->new RecursoNaoEncontradoException("Pedido")); autorizar(p,ator);
        return jdbc.query("select id,status,motivo_resultado from app.pagamento where pedido_id=? order by numero_tentativa",(rs,n)->new Resultado(rs.getObject(1,UUID.class),rs.getString(2),p.getStatus().name(),rs.getString(3),false,false),id);
    }
    @Transactional
    public StatusPedido avancar(UUID id,AtorPedido ator,StatusPedido novo){
        var p=pedidos.findForUpdateById(id).orElseThrow(()->new RecursoNaoEncontradoException("Pedido"));
        boolean admin=ator.possui(Perfil.ADMIN), gerente=ator.possui(Perfil.GERENTE), cozinha=ator.possui(Perfil.COZINHA), atendente=ator.possui(Perfil.ATENDENTE);
        if(!admin && (!(gerente||cozinha||atendente)||!acesso.possuiVinculo(p.getUnidadeId(),ator.usuarioId()))) throw new AcessoNegadoException();
        if(!admin&&!gerente && !((cozinha&&(novo==StatusPedido.EM_PREPARO||novo==StatusPedido.PRONTO))||(atendente&&novo==StatusPedido.ENTREGUE)))throw new AcessoNegadoException();
        boolean valido=(p.getStatus()==StatusPedido.CONFIRMADO&&novo==StatusPedido.EM_PREPARO)||(p.getStatus()==StatusPedido.EM_PREPARO&&novo==StatusPedido.PRONTO)||(p.getStatus()==StatusPedido.PRONTO&&novo==StatusPedido.ENTREGUE);
        if(!valido)throw new ConflitoException("Transição de estado inválida");
        transicao(p,ator.usuarioId(),novo,"USUARIO",null); return novo;
    }
    private void transicao(PedidoEntity p,UUID ator,StatusPedido novo,String origem,String motivo){
        jdbc.update("insert into app.pedido_transicao(id,pedido_id,ator_id,origem,estado_anterior,estado_novo,motivo,ocorrido_em) values (?,?,?,?,?,?,?,?)",UUID.randomUUID(),p.getId(),ator,origem,p.getStatus().name(),novo.name(),motivo,java.sql.Timestamp.from(clock.instant()));
        p.alterarStatus(novo); pedidos.saveAndFlush(p);
    }
    private void autorizar(PedidoEntity p,AtorPedido a){
        if(a.possui(Perfil.ADMIN))return;
        if(a.possui(Perfil.CLIENTE)&&Objects.equals(p.getClienteId(),a.usuarioId()))return;
        if((a.possui(Perfil.ATENDENTE)||a.possui(Perfil.GERENTE))&&acesso.possuiVinculo(p.getUnidadeId(),a.usuarioId()))return;
        throw new AcessoNegadoException();
    }
    private String hash(String c){try{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(c.getBytes(StandardCharsets.UTF_8)));}catch(Exception e){throw new IllegalStateException(e);}}
    private record Registro(UUID id,String status,String motivo,String hash){}
    public record Resultado(UUID pagamentoId,String statusPagamento,String statusPedido,String motivo,boolean repetida,boolean reservaExpirada){}
}
