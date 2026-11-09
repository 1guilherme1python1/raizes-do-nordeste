package br.com.raizesdonordeste.unidades.api;
import jakarta.validation.constraints.Size;
public record AtualizarUnidadeRequest(@Size(min=1,max=150) String nome, Boolean ativa) {}
