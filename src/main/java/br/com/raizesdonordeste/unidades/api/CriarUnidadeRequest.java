package br.com.raizesdonordeste.unidades.api;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
public record CriarUnidadeRequest(@NotBlank @Size(max=50) String codigo, @NotBlank @Size(max=150) String nome) {}
