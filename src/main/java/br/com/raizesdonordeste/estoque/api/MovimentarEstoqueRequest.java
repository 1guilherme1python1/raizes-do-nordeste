package br.com.raizesdonordeste.estoque.api;
import java.util.UUID;
import br.com.raizesdonordeste.estoque.domain.TipoMovimentacaoEstoque;
import jakarta.validation.constraints.*;
public record MovimentarEstoqueRequest(@NotNull UUID produtoId,@NotNull TipoMovimentacaoEstoque tipo,
        @Positive int quantidade,@NotBlank @Size(max=250) String motivo){}
