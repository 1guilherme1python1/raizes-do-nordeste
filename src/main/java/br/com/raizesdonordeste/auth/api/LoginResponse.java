package br.com.raizesdonordeste.auth.api;

import java.time.Instant;

import br.com.raizesdonordeste.auth.application.LoginResult;
import br.com.raizesdonordeste.usuarios.api.UsuarioResponse;

public record LoginResponse(
        String accessToken,
        String tokenType,
        Instant expiresAt,
        UsuarioResponse usuario) {

    public static LoginResponse from(LoginResult result) {
        return new LoginResponse(result.accessToken().value(), "Bearer", result.accessToken().expiresAt(),
                UsuarioResponse.from(result.usuario()));
    }
}
