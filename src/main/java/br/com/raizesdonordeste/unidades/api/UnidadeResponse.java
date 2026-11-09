package br.com.raizesdonordeste.unidades.api;
import java.time.Instant;
import java.util.UUID;
import br.com.raizesdonordeste.unidades.domain.Unidade;
public record UnidadeResponse(UUID id, String codigo, String nome, boolean ativa, Instant criadoEm, Instant atualizadoEm) {
    public static UnidadeResponse from(Unidade u) { return new UnidadeResponse(u.id(),u.codigo(),u.nome(),u.ativa(),u.criadoEm(),u.atualizadoEm()); }
}
