package br.com.raizesdonordeste.usuarios.application;

import br.com.raizesdonordeste.usuarios.domain.Perfil;

public record CriarUsuarioCommand(
        String nomeExibicao,
        String email,
        String senha,
        Perfil perfil) {
}
