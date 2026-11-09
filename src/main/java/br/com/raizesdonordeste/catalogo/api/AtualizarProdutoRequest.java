package br.com.raizesdonordeste.catalogo.api;
import jakarta.validation.constraints.Size;
public record AtualizarProdutoRequest(@Size(min=1,max=150) String nome,@Size(max=500) String descricao,Boolean ativo){}
