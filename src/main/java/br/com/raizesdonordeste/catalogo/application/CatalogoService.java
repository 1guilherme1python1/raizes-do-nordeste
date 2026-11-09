package br.com.raizesdonordeste.catalogo.application;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import br.com.raizesdonordeste.catalogo.domain.Produto;
import br.com.raizesdonordeste.catalogo.domain.ProdutoUnidade;
import br.com.raizesdonordeste.catalogo.infrastructure.persistence.*;
import br.com.raizesdonordeste.estoque.domain.Estoque;
import br.com.raizesdonordeste.estoque.infrastructure.persistence.EstoqueEntity;
import br.com.raizesdonordeste.estoque.infrastructure.persistence.EstoqueJpaRepository;
import br.com.raizesdonordeste.shared.application.ConflitoException;
import br.com.raizesdonordeste.shared.application.RecursoNaoEncontradoException;
import br.com.raizesdonordeste.unidades.application.UnidadeService;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CatalogoService {
    private final ProdutoJpaRepository produtos; private final ProdutoUnidadeJpaRepository ofertas;
    private final EstoqueJpaRepository estoques; private final UnidadeService unidades;
    public CatalogoService(ProdutoJpaRepository produtos, ProdutoUnidadeJpaRepository ofertas,
            EstoqueJpaRepository estoques, UnidadeService unidades) {
        this.produtos=produtos; this.ofertas=ofertas; this.estoques=estoques; this.unidades=unidades;
    }
    @Transactional
    public Produto criarProduto(String sku,String nome,String descricao) {
        if(sku==null||sku.isBlank()||nome==null||nome.isBlank()) throw new IllegalArgumentException("SKU e nome são obrigatórios");
        var normalized=sku.strip().toUpperCase();
        if(produtos.existsBySku(normalized)) throw new ConflitoException("SKU já cadastrado");
        return produtos.saveAndFlush(new ProdutoEntity(UUID.randomUUID(),normalized,nome.strip(),limpar(descricao),true,null,null)).toDomain();
    }
    @Transactional(readOnly=true)
    public Page<Produto> listarProdutos(Boolean ativo,String termo,Pageable pageable) {
        Specification<ProdutoEntity> spec=(r,q,c)->c.conjunction();
        if(ativo!=null) spec=spec.and((r,q,c)->c.equal(r.get("ativo"),ativo));
        if(termo!=null&&!termo.isBlank()){var p="%"+termo.strip().toLowerCase()+"%";
            spec=spec.and((r,q,c)->c.or(c.like(c.lower(r.get("nome")),p),c.like(c.lower(r.get("sku")),p)));}
        return produtos.findAll(spec,pageable).map(ProdutoEntity::toDomain);
    }
    @Transactional(readOnly=true) public Produto buscarProduto(UUID id){return produtoEntity(id).toDomain();}
    @Transactional
    public Produto atualizarProduto(UUID id,String nome,String descricao,Boolean ativo){
        if(nome!=null&&nome.isBlank()) throw new IllegalArgumentException("Nome do produto não pode ser vazio");
        var e=produtoEntity(id); e.atualizar(nome==null?e.getNome():nome.strip(),descricao==null?e.getDescricao():limpar(descricao),ativo==null?e.isAtivo():ativo);
        return produtos.saveAndFlush(e).toDomain();
    }
    @Transactional
    public ProdutoUnidade configurarOferta(UUID unidadeId,UUID produtoId,BigDecimal preco,boolean disponivel){
        if(preco==null||preco.signum()<=0||preco.scale()>2) throw new IllegalArgumentException("Preço deve ser positivo e possuir no máximo duas casas decimais");
        unidades.buscar(unidadeId); produtoEntity(produtoId);
        var oferta=ofertas.findByProdutoIdAndUnidadeId(produtoId,unidadeId).orElse(null);
        if(oferta==null){
            oferta=ofertas.saveAndFlush(new ProdutoUnidadeEntity(UUID.randomUUID(),produtoId,unidadeId,preco,disponivel,null,null));
            estoques.saveAndFlush(EstoqueEntity.fromDomain(new Estoque(UUID.randomUUID(),oferta.getId(),unidadeId,0,0,0,null,null)));
        } else { oferta.atualizar(preco,disponivel); oferta=ofertas.saveAndFlush(oferta); }
        return oferta.toDomain();
    }
    @Transactional(readOnly=true)
    public ProdutoUnidade buscarOferta(UUID unidadeId,UUID produtoId){
        return ofertas.findByProdutoIdAndUnidadeId(produtoId,unidadeId).map(ProdutoUnidadeEntity::toDomain)
                .orElseThrow(()->new RecursoNaoEncontradoException("Oferta do produto na unidade"));
    }
    @Transactional(readOnly=true)
    public ProdutoUnidade ofertaDisponivelParaPedido(UUID unidadeId, UUID produtoId) {
        var unidade = unidades.buscar(unidadeId);
        if (!unidade.ativa()) throw new ConflitoException("Unidade inativa");
        var produto = produtoEntity(produtoId);
        if (!produto.isAtivo()) throw new ConflitoException("Produto inativo");
        var oferta = buscarOferta(unidadeId, produtoId);
        if (!oferta.disponivel()) throw new ConflitoException("Produto indisponível na unidade");
        return oferta;
    }
    @Transactional(readOnly=true)
    public UUID buscarProdutoPorOferta(UUID ofertaId,UUID unidadeId){
        var oferta=ofertas.findById(ofertaId).filter(o->o.getUnidadeId().equals(unidadeId))
                .orElseThrow(()->new RecursoNaoEncontradoException("Oferta do produto na unidade"));
        return oferta.getProdutoId();
    }
    @Transactional(readOnly=true)
    public Page<ItemCardapio> cardapio(UUID unidadeId,Pageable pageable){
        var unidade=unidades.buscar(unidadeId); if(!unidade.ativa()) return Page.empty(pageable);
        List<ItemCardapio> itens=ofertas.findByUnidadeId(unidadeId).stream().filter(ProdutoUnidadeEntity::isDisponivel)
            .map(o->{var p=produtos.findById(o.getProdutoId()).orElse(null); var e=estoques.findByProdutoUnidadeId(o.getId()).orElse(null);
                if(p==null||!p.isAtivo()||e==null||e.getQuantidadeFisica()-e.getQuantidadeReservada()<=0)return null;
                return new ItemCardapio(p.getId(),p.getSku(),p.getNome(),p.getDescricao(),o.getPreco(),e.getQuantidadeFisica()-e.getQuantidadeReservada());})
            .filter(java.util.Objects::nonNull).sorted(java.util.Comparator.comparing(ItemCardapio::nome)).toList();
        int start=(int)Math.min(pageable.getOffset(),itens.size()); int end=Math.min(start+pageable.getPageSize(),itens.size());
        return new PageImpl<>(itens.subList(start,end),pageable,itens.size());
    }
    private ProdutoEntity produtoEntity(UUID id){return produtos.findById(id).orElseThrow(()->new RecursoNaoEncontradoException("Produto"));}
    private String limpar(String value){return value==null||value.isBlank()?null:value.strip();}
    public record ItemCardapio(UUID produtoId,String sku,String nome,String descricao,BigDecimal preco,int quantidadeDisponivel){}
}
