package br.com.raizesdonordeste.unidades.infrastructure.persistence;

import java.io.Serializable;
import java.util.UUID;

public record UsuarioUnidadeId(UUID usuarioId, UUID unidadeId) implements Serializable {
}
