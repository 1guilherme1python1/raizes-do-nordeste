package br.com.raizesdonordeste.auth.api;

import br.com.raizesdonordeste.auth.application.AuthService;
import br.com.raizesdonordeste.shared.api.ApiError;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Autenticação")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    @Operation(summary = "Autentica por e-mail e senha")
    @ApiResponse(responseCode = "200", description = "JWT emitido")
    @ApiResponse(responseCode = "401", description = "Credenciais inválidas",
            content = @Content(schema = @Schema(implementation = ApiError.class), examples = @ExampleObject(
                    value = "{\"error\":\"invalid_credentials\",\"message\":\"Credenciais inválidas\"}")))
    @ApiResponse(responseCode = "422", description = "Entrada inválida",
            content = @Content(schema = @Schema(implementation = ApiError.class)))
    public LoginResponse login(@Valid @RequestBody LoginRequest request) {
        return LoginResponse.from(authService.login(request.email(), request.senha()));
    }
}
