package br.com.raizesdonordeste.pedidos.application;

import java.util.Set;
import java.util.UUID;
import br.com.raizesdonordeste.usuarios.domain.Perfil;

public record AtorPedido(UUID usuarioId, Set<Perfil> perfis) {
    public boolean possui(Perfil perfil) { return perfis.contains(perfil); }
}
