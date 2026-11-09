package br.com.raizesdonordeste.catalogo.api;
import jakarta.validation.constraints.*;
public record CriarProdutoRequest(@NotBlank @Size(max=50) String sku,@NotBlank @Size(max=150) String nome,@Size(max=500) String descricao){}
