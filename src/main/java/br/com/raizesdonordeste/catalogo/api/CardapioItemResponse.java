package br.com.raizesdonordeste.catalogo.api;
import java.math.BigDecimal; import java.util.UUID;
import br.com.raizesdonordeste.catalogo.application.CatalogoService.ItemCardapio;
public record CardapioItemResponse(UUID produtoId,String sku,String nome,String descricao,BigDecimal preco,boolean disponivel){
 public static CardapioItemResponse from(ItemCardapio i){return new CardapioItemResponse(i.produtoId(),i.sku(),i.nome(),i.descricao(),i.preco(),true);}}
