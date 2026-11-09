package br.com.raizesdonordeste.estoque.application;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.UUID;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import br.com.raizesdonordeste.catalogo.application.CatalogoService;
import br.com.raizesdonordeste.estoque.domain.*;
import br.com.raizesdonordeste.estoque.infrastructure.persistence.*;
import br.com.raizesdonordeste.shared.application.ConflitoException;
import br.com.raizesdonordeste.shared.application.RecursoNaoEncontradoException;
import br.com.raizesdonordeste.unidades.application.AcessoUnidadeService;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EstoqueService {
    private final EstoqueJpaRepository estoques; private final MovimentacaoEstoqueJpaRepository movimentos;
    private final CatalogoService catalogo; private final AcessoUnidadeService acesso;
    public EstoqueService(EstoqueJpaRepository estoques,MovimentacaoEstoqueJpaRepository movimentos,
            CatalogoService catalogo,AcessoUnidadeService acesso){this.estoques=estoques;this.movimentos=movimentos;this.catalogo=catalogo;this.acesso=acesso;}

    @Transactional(readOnly=true)
    public Page<SaldoEstoque> listar(UUID unidadeId,UUID usuarioId,boolean admin,boolean gerente,Pageable pageable){
        acesso.validarOperacao(unidadeId,usuarioId,admin,gerente);
        return estoques.findByUnidadeId(unidadeId,pageable).map(e->{var p=catalogo.buscarProduto(
                catalogo.buscarProdutoPorOferta(e.getProdutoUnidadeId(),unidadeId));
            return new SaldoEstoque(e.getId(),p.id(),p.sku(),p.nome(),e.getQuantidadeFisica(),e.getQuantidadeReservada(),
                    e.getQuantidadeFisica()-e.getQuantidadeReservada(),e.getVersao());});
    }

    @Transactional
    public MovimentoRegistrado movimentar(UUID unidadeId,UUID usuarioId,boolean admin,boolean gerente,
            String chave,UUID produtoId,TipoMovimentacaoEstoque tipo,int quantidade,String motivo){
        acesso.validarOperacao(unidadeId,usuarioId,admin,gerente);
        if(chave==null||chave.isBlank()||chave.length()>100) throw new IllegalArgumentException("Idempotency-Key é obrigatório e deve possuir até 100 caracteres");
        if(tipo!=TipoMovimentacaoEstoque.ENTRADA&&tipo!=TipoMovimentacaoEstoque.SAIDA) throw new IllegalArgumentException("Tipo deve ser ENTRADA ou SAIDA");
        if(quantidade<=0) throw new IllegalArgumentException("A quantidade deve ser positiva");
        if(motivo==null||motivo.isBlank()) throw new IllegalArgumentException("O motivo é obrigatório");
        var justificativa=motivo.strip(); var hash=hash(unidadeId+"|"+usuarioId+"|"+produtoId+"|"+tipo+"|"+quantidade+"|"+justificativa);
        var anterior=movimentos.findByUnidadeIdAndIdempotencyKey(unidadeId,chave);
        if(anterior.isPresent()){
            if(!anterior.get().getRequisicaoHash().equals(hash)) throw new ConflitoException("Idempotency-Key já utilizada com conteúdo diferente");
            return resultado(anterior.get(),true);
        }
        var oferta=catalogo.buscarOferta(unidadeId,produtoId);
        var estoque=estoques.findByProdutoUnidadeId(oferta.id()).orElseThrow(()->new RecursoNaoEncontradoException("Estoque"));
        var disponivel=estoque.getQuantidadeFisica()-estoque.getQuantidadeReservada();
        if(tipo==TipoMovimentacaoEstoque.SAIDA&&quantidade>disponivel) throw new ConflitoException("Saldo disponível insuficiente");
        int novo=tipo==TipoMovimentacaoEstoque.ENTRADA?Math.addExact(estoque.getQuantidadeFisica(),quantidade):estoque.getQuantidadeFisica()-quantidade;
        estoque.movimentar(novo); estoques.saveAndFlush(estoque);
        var movimento=new MovimentacaoEstoque(UUID.randomUUID(),estoque.getId(),unidadeId,usuarioId,tipo,quantidade,
                novo,estoque.getQuantidadeReservada(),justificativa,chave,hash,java.time.Instant.now());
        return resultado(movimentos.saveAndFlush(MovimentacaoEstoqueEntity.fromDomain(movimento)),false);
    }

    public void reservarPedido(UUID unidadeId, UUID responsavelId, UUID pedidoId, List<ReservaItem> itens) {
        itens.stream().sorted(Comparator.comparing(ReservaItem::produtoId)).forEach(item -> {
            var oferta = catalogo.buscarOferta(unidadeId, item.produtoId());
            var estoque = estoques.findByProdutoUnidadeId(oferta.id())
                    .orElseThrow(() -> new RecursoNaoEncontradoException("Estoque"));
            try { estoque.reservar(item.quantidade()); }
            catch (IllegalArgumentException e) { throw new ConflitoException("Saldo disponível insuficiente"); }
            estoques.saveAndFlush(estoque);
            var chave = "PED:" + pedidoId + ":ITEM:" + item.itemPedidoId() + ":R";
            var movimento = new MovimentacaoEstoque(UUID.randomUUID(), estoque.getId(), unidadeId, responsavelId,
                    TipoMovimentacaoEstoque.RESERVA, item.quantidade(), estoque.getQuantidadeFisica(),
                    estoque.getQuantidadeReservada(), "Reserva do pedido", chave, hash(chave), pedidoId,
                    item.itemPedidoId(), Instant.now());
            movimentos.saveAndFlush(MovimentacaoEstoqueEntity.fromDomain(movimento));
        });
    }

    public void liberarPedido(UUID unidadeId, UUID responsavelId, UUID pedidoId, List<ReservaItem> itens) {
        itens.stream().sorted(Comparator.comparing(ReservaItem::produtoId)).forEach(item -> {
            var oferta = catalogo.buscarOferta(unidadeId, item.produtoId());
            var estoque = estoques.findByProdutoUnidadeId(oferta.id())
                    .orElseThrow(() -> new RecursoNaoEncontradoException("Estoque"));
            estoque.liberar(item.quantidade());
            estoques.saveAndFlush(estoque);
            var chave = "PED:" + pedidoId + ":ITEM:" + item.itemPedidoId() + ":L";
            var movimento = new MovimentacaoEstoque(UUID.randomUUID(), estoque.getId(), unidadeId, responsavelId,
                    TipoMovimentacaoEstoque.LIBERACAO, item.quantidade(), estoque.getQuantidadeFisica(),
                    estoque.getQuantidadeReservada(), "Liberação da reserva do pedido", chave, hash(chave), pedidoId,
                    item.itemPedidoId(), Instant.now());
            movimentos.saveAndFlush(MovimentacaoEstoqueEntity.fromDomain(movimento));
        });
    }

    @Transactional(readOnly=true)
    public Page<MovimentoRegistrado> historico(UUID unidadeId,UUID usuarioId,boolean admin,boolean gerente,
            TipoMovimentacaoEstoque tipo,Pageable pageable){
        acesso.validarOperacao(unidadeId,usuarioId,admin,gerente);
        var page=tipo==null?movimentos.findByUnidadeId(unidadeId,pageable):movimentos.findByUnidadeIdAndTipo(unidadeId,tipo,pageable);
        return page.map(e->resultado(e,false));
    }
    private MovimentoRegistrado resultado(MovimentacaoEstoqueEntity e,boolean repetida){return new MovimentoRegistrado(e.getId(),e.getEstoqueId(),e.getUnidadeId(),e.getResponsavelUsuarioId(),e.getTipo(),e.getQuantidade(),e.getQuantidadeFisicaResultante(),e.getQuantidadeReservadaResultante(),e.getQuantidadeFisicaResultante()-e.getQuantidadeReservadaResultante(),e.getMotivo(),e.getCriadoEm(),repetida);}
    private String hash(String value){try{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));}catch(NoSuchAlgorithmException e){throw new IllegalStateException(e);}}
    public record SaldoEstoque(UUID estoqueId,UUID produtoId,String sku,String produto,int quantidadeFisica,int quantidadeReservada,int quantidadeDisponivel,long versao){}
    public record MovimentoRegistrado(UUID id,UUID estoqueId,UUID unidadeId,UUID responsavelUsuarioId,TipoMovimentacaoEstoque tipo,int quantidade,int quantidadeFisicaResultante,int quantidadeReservadaResultante,int quantidadeDisponivelResultante,String motivo,java.time.Instant criadoEm,boolean repetida){}
    public record ReservaItem(UUID itemPedidoId, UUID produtoId, int quantidade) {}
}
