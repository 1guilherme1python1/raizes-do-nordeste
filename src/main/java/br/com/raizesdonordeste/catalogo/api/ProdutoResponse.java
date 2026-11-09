package br.com.raizesdonordeste.catalogo.api;
import java.time.Instant; import java.util.UUID;
import br.com.raizesdonordeste.catalogo.domain.Produto;
public record ProdutoResponse(UUID id,String sku,String nome,String descricao,boolean ativo,Instant criadoEm,Instant atualizadoEm){
 public static ProdutoResponse from(Produto p){return new ProdutoResponse(p.id(),p.sku(),p.nome(),p.descricao(),p.ativo(),p.criadoEm(),p.atualizadoEm());}}
