package br.com.raizesdonordeste.catalogo.api;
import java.math.BigDecimal; import java.util.UUID;
import br.com.raizesdonordeste.catalogo.domain.ProdutoUnidade;
public record OfertaResponse(UUID id,UUID produtoId,UUID unidadeId,BigDecimal preco,boolean disponivel){
 public static OfertaResponse from(ProdutoUnidade o){return new OfertaResponse(o.id(),o.produtoId(),o.unidadeId(),o.preco(),o.disponivel());}}
