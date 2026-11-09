package br.com.raizesdonordeste.auth.application;

import br.com.raizesdonordeste.usuarios.domain.Usuario;

public record LoginResult(AccessToken accessToken, Usuario usuario) {
}
