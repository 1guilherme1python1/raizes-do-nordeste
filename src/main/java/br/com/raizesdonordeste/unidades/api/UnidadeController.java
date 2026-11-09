package br.com.raizesdonordeste.unidades.api;

import java.net.URI;
import java.util.UUID;
import br.com.raizesdonordeste.unidades.application.AcessoUnidadeService;
import br.com.raizesdonordeste.unidades.application.UnidadeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.media.*;
import br.com.raizesdonordeste.shared.api.ApiError;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController @RequestMapping("/api/v1/unidades") @Tag(name="Unidades")
public class UnidadeController {
    private final UnidadeService service; private final AcessoUnidadeService acesso;
    public UnidadeController(UnidadeService service, AcessoUnidadeService acesso) { this.service=service; this.acesso=acesso; }

    @PostMapping @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary="Cadastra unidade", security=@SecurityRequirement(name="bearerAuth"))
    @ApiResponse(responseCode="201",description="Unidade cadastrada") @ApiResponse(responseCode="403",description="Somente ADMIN",content=@Content(schema=@Schema(implementation=ApiError.class)))
    @ApiResponse(responseCode="409",description="Código duplicado",content=@Content(schema=@Schema(implementation=ApiError.class))) @ApiResponse(responseCode="422",description="Dados inválidos",content=@Content(schema=@Schema(implementation=ApiError.class)))
    public ResponseEntity<UnidadeResponse> criar(@Valid @RequestBody CriarUnidadeRequest request) {
        var u=service.criar(request.codigo(),request.nome());
        return ResponseEntity.created(URI.create("/api/v1/unidades/"+u.id())).body(UnidadeResponse.from(u));
    }
    @GetMapping @Operation(summary="Lista unidades", security=@SecurityRequirement(name="bearerAuth"))
    @ApiResponse(responseCode="200",description="Página de unidades") @ApiResponse(responseCode="401",description="JWT ausente ou inválido",content=@Content(schema=@Schema(implementation=ApiError.class)))
    public Page<UnidadeResponse> listar(@RequestParam(required=false) Boolean ativa,
            @RequestParam(required=false) String nome, Pageable pageable) {
        return service.listar(ativa,nome,pageable).map(UnidadeResponse::from);
    }
    @GetMapping("/{id}") @Operation(summary="Consulta unidade", security=@SecurityRequirement(name="bearerAuth"))
    @ApiResponse(responseCode="200",description="Unidade encontrada") @ApiResponse(responseCode="404",description="Unidade inexistente",content=@Content(schema=@Schema(implementation=ApiError.class)))
    public UnidadeResponse buscar(@PathVariable UUID id) { return UnidadeResponse.from(service.buscar(id)); }
    @PatchMapping("/{id}") @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary="Atualiza ou desativa unidade", security=@SecurityRequirement(name="bearerAuth"))
    @ApiResponse(responseCode="200",description="Unidade atualizada") @ApiResponse(responseCode="403",description="Somente ADMIN",content=@Content(schema=@Schema(implementation=ApiError.class))) @ApiResponse(responseCode="404",description="Unidade inexistente",content=@Content(schema=@Schema(implementation=ApiError.class)))
    public UnidadeResponse atualizar(@PathVariable UUID id,@Valid @RequestBody AtualizarUnidadeRequest request) {
        return UnidadeResponse.from(service.atualizar(id,request.nome(),request.ativa()));
    }
    @PutMapping("/{unidadeId}/usuarios/{usuarioId}") @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary="Associa funcionário à unidade", security=@SecurityRequirement(name="bearerAuth"))
    @ApiResponse(responseCode="204",description="Vínculo criado") @ApiResponse(responseCode="403",description="Somente ADMIN",content=@Content(schema=@Schema(implementation=ApiError.class))) @ApiResponse(responseCode="409",description="Perfil não operacional",content=@Content(schema=@Schema(implementation=ApiError.class)))
    public ResponseEntity<Void> associar(@PathVariable UUID unidadeId,@PathVariable UUID usuarioId) {
        acesso.associar(unidadeId,usuarioId); return ResponseEntity.noContent().build();
    }
    @DeleteMapping("/{unidadeId}/usuarios/{usuarioId}") @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary="Desassocia funcionário da unidade", security=@SecurityRequirement(name="bearerAuth"))
    @ApiResponse(responseCode="204",description="Vínculo removido") @ApiResponse(responseCode="403",description="Somente ADMIN",content=@Content(schema=@Schema(implementation=ApiError.class))) @ApiResponse(responseCode="404",description="Vínculo inexistente",content=@Content(schema=@Schema(implementation=ApiError.class)))
    public ResponseEntity<Void> desassociar(@PathVariable UUID unidadeId,@PathVariable UUID usuarioId) {
        acesso.desassociar(unidadeId,usuarioId); return ResponseEntity.noContent().build();
    }
}
