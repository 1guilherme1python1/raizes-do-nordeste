package br.com.raizesdonordeste.catalogo.api;
import java.math.BigDecimal;
import jakarta.validation.constraints.*;
public record ConfigurarOfertaRequest(@NotNull @DecimalMin("0.01") @Digits(integer=10,fraction=2) BigDecimal preco,@NotNull Boolean disponivel){}
