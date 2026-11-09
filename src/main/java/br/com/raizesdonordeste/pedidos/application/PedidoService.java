package br.com.raizesdonordeste.pedidos.application;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.*;

import br.com.raizesdonordeste.catalogo.application.CatalogoService;
import br.com.raizesdonordeste.estoque.application.EstoqueService;
import br.com.raizesdonordeste.estoque.application.EstoqueService.ReservaItem;
import br.com.raizesdonordeste.pedidos.domain.*;
import br.com.raizesdonordeste.pedidos.infrastructure.persistence.*;
import br.com.raizesdonordeste.shared.application.*;
import br.com.raizesdonordeste.unidades.application.AcessoUnidadeService;
import br.com.raizesdonordeste.usuarios.application.UsuarioService;
import br.com.raizesdonordeste.usuarios.domain.Perfil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PedidoService {
    private final PedidoJpaRepository pedidos;
    private final CatalogoService catalogo;
    private final EstoqueService estoque;
    private final AcessoUnidadeService acesso;
    private final UsuarioService usuarios;
    private final JdbcTemplate jdbc;
    private final Clock clock;
    private final Duration duracaoReserva;

    public PedidoService(PedidoJpaRepository pedidos, CatalogoService catalogo, EstoqueService estoque,
            AcessoUnidadeService acesso, UsuarioService usuarios, JdbcTemplate jdbc, Clock clock,
            @Value("${app.pedidos.reservation-duration:PT15M}") Duration duracaoReserva) {
        this.pedidos=pedidos; this.catalogo=catalogo; this.estoque=estoque; this.acesso=acesso;
        this.usuarios=usuarios; this.jdbc=jdbc; this.clock=clock; this.duracaoReserva=duracaoReserva;
    }

    @Transactional
    public CriacaoResultado criar(CriarPedido comando, AtorPedido ator, String chave) {
        validarChave(chave);
        autorizarCriacao(comando, ator);
        var clienteId = ator.possui(Perfil.CLIENTE) ? ator.usuarioId() : comando.clienteId();
        if (clienteId != null && usuarios.buscarPorId(clienteId).perfil() != Perfil.CLIENTE)
            throw new IllegalArgumentException("O cliente informado não possui perfil CLIENTE");
        validarItens(comando.itens());
        var hash = hashCanonico(comando, clienteId);
        jdbc.queryForObject("select pg_advisory_xact_lock(?)", Object.class,
                chaveLock(ator.usuarioId(), comando.unidadeId(), chave));
        var anterior = pedidos.findByCriadoPorUsuarioIdAndUnidadeIdAndIdempotencyKey(
                ator.usuarioId(), comando.unidadeId(), chave);
        if (anterior.isPresent()) {
            if (!hash.equals(anterior.get().getRequisicaoHash()))
                throw new ConflitoException("Idempotency-Key já utilizada com conteúdo diferente");
            return new CriacaoResultado(anterior.get().toDomain(), true);
        }
        var itens = new ArrayList<ItemPedido>();
        var reservas = new ArrayList<ReservaItem>();
        BigDecimal total = BigDecimal.ZERO.setScale(2);
        for (var solicitado : comando.itens().stream().sorted(Comparator.comparing(ItemSolicitado::produtoId)).toList()) {
            var oferta = catalogo.ofertaDisponivelParaPedido(comando.unidadeId(), solicitado.produtoId());
            var preco = oferta.preco().setScale(2, RoundingMode.HALF_EVEN);
            var subtotal = preco.multiply(BigDecimal.valueOf(solicitado.quantidade())).setScale(2, RoundingMode.HALF_EVEN);
            var itemId = UUID.randomUUID();
            itens.add(new ItemPedido(itemId, solicitado.produtoId(), oferta.id(), solicitado.quantidade(), preco, subtotal, null, null));
            reservas.add(new ReservaItem(itemId, solicitado.produtoId(), solicitado.quantidade()));
            total = total.add(subtotal);
        }
        var pedido = new Pedido(UUID.randomUUID(), comando.unidadeId(), clienteId, ator.usuarioId(),
                comando.canal(), StatusPedido.AGUARDANDO_PAGAMENTO, comando.formaPagamento(), total, "BRL",
                Instant.now(clock).plus(duracaoReserva), chave, itens, 0, null, null);
        var entity = PedidoEntity.fromDomain(pedido); entity.configurarRequisicao(hash);
        pedidos.saveAndFlush(entity);
        estoque.reservarPedido(comando.unidadeId(), ator.usuarioId(), pedido.id(), reservas);
        return new CriacaoResultado(entity.toDomain(), false);
    }

    @Transactional(readOnly=true)
    public Pedido buscar(UUID id, AtorPedido ator) {
        var p = pedidos.findWithItensById(id).orElseThrow(() -> new RecursoNaoEncontradoException("Pedido"));
        autorizarLeitura(p, ator); return p.toDomain();
    }

    @Transactional(readOnly=true)
    public Page<Pedido> listar(UUID unidadeId, StatusPedido status, CanalPedido canal, AtorPedido ator, Pageable pageable) {
        Specification<PedidoEntity> spec=(r,q,c)->c.conjunction();
        if (unidadeId!=null) spec=spec.and((r,q,c)->c.equal(r.get("unidadeId"),unidadeId));
        if (status!=null) spec=spec.and((r,q,c)->c.equal(r.get("status"),status));
        if (canal!=null) spec=spec.and((r,q,c)->c.equal(r.get("canalPedido"),canal));
        if (ator.possui(Perfil.CLIENTE)) spec=spec.and((r,q,c)->c.equal(r.get("clienteId"),ator.usuarioId()));
        else if (!ator.possui(Perfil.ADMIN)) {
            var unidades=acesso.unidadesDoUsuario(ator.usuarioId());
            if(unidades.isEmpty()) return Page.empty(pageable);
            spec=spec.and((r,q,c)->r.get("unidadeId").in(unidades));
        }
        return pedidos.findAll(spec,pageable).map(PedidoEntity::toDomain);
    }

    @Transactional
    public Pedido cancelar(UUID id, AtorPedido ator) {
        var p=pedidos.findForUpdateById(id).orElseThrow(()->new RecursoNaoEncontradoException("Pedido"));
        autorizarCancelamento(p,ator);
        if(p.getStatus()==StatusPedido.CANCELADO) return p.toDomain();
        if(p.getStatus()!=StatusPedido.AGUARDANDO_PAGAMENTO) throw new ConflitoException("Pedido não pode ser cancelado no estado atual");
        estoque.liberarPedido(p.getUnidadeId(),ator.usuarioId(),p.getId(),reservas(p));
        p.cancelar(); return pedidos.saveAndFlush(p).toDomain();
    }

    private List<ReservaItem> reservas(PedidoEntity p){return p.getItens().stream().map(i->new ReservaItem(i.getId(),i.getProdutoId(),i.getQuantidade())).toList();}
    private void validarChave(String c){if(c==null||c.isBlank()||c.length()>100)throw new IllegalArgumentException("Idempotency-Key é obrigatório e deve possuir até 100 caracteres");}
    private void validarItens(List<ItemSolicitado> itens){if(itens==null||itens.isEmpty())throw new IllegalArgumentException("O pedido deve possuir itens");var ids=new HashSet<UUID>();for(var i:itens){if(i.produtoId()==null||i.quantidade()<=0)throw new IllegalArgumentException("Item do pedido inválido");if(!ids.add(i.produtoId()))throw new IllegalArgumentException("Produto duplicado no pedido");}}
    private void autorizarCriacao(CriarPedido c,AtorPedido a){
        if(a.possui(Perfil.ADMIN))return;
        if(a.possui(Perfil.CLIENTE)){if(!Set.of(CanalPedido.APP,CanalPedido.WEB,CanalPedido.PICKUP,CanalPedido.TOTEM).contains(c.canal()))throw new AcessoNegadoException();return;}
        if(!acesso.possuiVinculo(c.unidadeId(),a.usuarioId()))throw new AcessoNegadoException();
        if(a.possui(Perfil.ATENDENTE)&&!Set.of(CanalPedido.BALCAO,CanalPedido.TOTEM).contains(c.canal()))throw new AcessoNegadoException();
        if(!a.possui(Perfil.ATENDENTE)&&!a.possui(Perfil.GERENTE))throw new AcessoNegadoException();
    }
    private void autorizarLeitura(PedidoEntity p,AtorPedido a){if(a.possui(Perfil.ADMIN))return;if(a.possui(Perfil.CLIENTE)&&Objects.equals(p.getClienteId(),a.usuarioId()))return;if(!a.possui(Perfil.CLIENTE)&&acesso.possuiVinculo(p.getUnidadeId(),a.usuarioId()))return;throw new AcessoNegadoException();}
    private void autorizarCancelamento(PedidoEntity p,AtorPedido a){if(a.possui(Perfil.ADMIN))return;if(a.possui(Perfil.CLIENTE)&&Objects.equals(p.getClienteId(),a.usuarioId()))return;if((a.possui(Perfil.ATENDENTE)||a.possui(Perfil.GERENTE))&&acesso.possuiVinculo(p.getUnidadeId(),a.usuarioId()))return;throw new AcessoNegadoException();}
    private String hashCanonico(CriarPedido c,UUID cliente){var itens=c.itens().stream().sorted(Comparator.comparing(ItemSolicitado::produtoId)).map(i->i.produtoId()+":"+i.quantidade()).toList();return hex(c.unidadeId()+"|"+cliente+"|"+c.canal()+"|"+c.formaPagamento()+"|"+itens);}
    private long chaveLock(UUID ator,UUID unidade,String chave){return java.nio.ByteBuffer.wrap(digest(ator+"|"+unidade+"|"+chave)).getLong();}
    private String hex(String s){return HexFormat.of().formatHex(digest(s));}
    private byte[] digest(String s){try{return MessageDigest.getInstance("SHA-256").digest(s.getBytes(StandardCharsets.UTF_8));}catch(Exception e){throw new IllegalStateException(e);}}

    public record CriarPedido(UUID unidadeId, UUID clienteId, CanalPedido canal, FormaPagamento formaPagamento,List<ItemSolicitado> itens){}
    public record ItemSolicitado(UUID produtoId,int quantidade){}
    public record CriacaoResultado(Pedido pedido,boolean repetida){}
}
