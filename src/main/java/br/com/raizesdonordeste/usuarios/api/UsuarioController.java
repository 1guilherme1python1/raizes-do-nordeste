package br.com.raizesdonordeste.usuarios.api;

import java.net.URI;
import java.util.UUID;

import br.com.raizesdonordeste.shared.api.ApiError;
import br.com.raizesdonordeste.usuarios.application.CriarUsuarioCommand;
import br.com.raizesdonordeste.usuarios.application.UsuarioService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/usuarios")
@Tag(name = "Usuários")
public class UsuarioController {

    private final UsuarioService usuarioService;

    public UsuarioController(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    @PostMapping("/registro")
    @Operation(summary = "Cadastra um cliente", description = "Cadastro público; o perfil é sempre CLIENTE")
    @ApiResponse(responseCode = "201", description = "Cliente cadastrado")
    @ApiResponse(responseCode = "409", description = "E-mail já cadastrado",
            content = @Content(schema = @Schema(implementation = ApiError.class)))
    @ApiResponse(responseCode = "422", description = "Entrada inválida",
            content = @Content(schema = @Schema(implementation = ApiError.class), examples = @ExampleObject(
                    value = "{\"error\":\"validation_error\",\"message\":\"A requisição contém campos inválidos\"}")))
    public ResponseEntity<UsuarioResponse> registrar(@Valid @RequestBody RegistroClienteRequest request) {
        var usuario = usuarioService.registrarCliente(request.nomeExibicao(), request.email(), request.senha());
        return ResponseEntity.created(URI.create("/api/v1/usuarios/me"))
                .body(UsuarioResponse.from(usuario));
    }

    @GetMapping("/me")
    @Operation(summary = "Consulta o usuário autenticado", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponse(responseCode = "200", description = "Usuário autenticado")
    @ApiResponse(responseCode = "401", description = "Token ausente ou inválido",
            content = @Content(schema = @Schema(implementation = ApiError.class)))
    public UsuarioResponse me(JwtAuthenticationToken authentication) {
        return UsuarioResponse.from(usuarioService.buscarPorId(UUID.fromString(authentication.getName())));
    }

    @PostMapping
    @Operation(summary = "Cadastra usuário interno", description = "Exige perfil ADMIN",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponse(responseCode = "201", description = "Usuário interno cadastrado")
    @ApiResponse(responseCode = "401", description = "Token ausente ou inválido",
            content = @Content(schema = @Schema(implementation = ApiError.class)))
    @ApiResponse(responseCode = "403", description = "Perfil sem permissão",
            content = @Content(schema = @Schema(implementation = ApiError.class)))
    @ApiResponse(responseCode = "409", description = "E-mail já cadastrado",
            content = @Content(schema = @Schema(implementation = ApiError.class)))
    @ApiResponse(responseCode = "422", description = "Entrada inválida",
            content = @Content(schema = @Schema(implementation = ApiError.class)))
    public ResponseEntity<UsuarioResponse> criar(@Valid @RequestBody CriarUsuarioRequest request) {
        var usuario = usuarioService.criarInterno(new CriarUsuarioCommand(request.nomeExibicao(), request.email(),
                request.senha(), request.perfil()));
        return ResponseEntity.status(HttpStatus.CREATED).body(UsuarioResponse.from(usuario));
    }
}
