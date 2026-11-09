package br.com.raizesdonordeste.catalogo.api;
import java.net.URI; import java.util.UUID;
import br.com.raizesdonordeste.catalogo.application.CatalogoService;
import io.swagger.v3.oas.annotations.Operation; import io.swagger.v3.oas.annotations.security.SecurityRequirement; import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid; import org.springframework.data.domain.*; import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize; import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.responses.ApiResponse; import io.swagger.v3.oas.annotations.media.*; import br.com.raizesdonordeste.shared.api.ApiError;
@RestController @RequestMapping("/api/v1/produtos") @Tag(name="Produtos")
public class ProdutoController{
 private final CatalogoService service; public ProdutoController(CatalogoService service){this.service=service;}
 @PostMapping @PreAuthorize("hasRole('ADMIN')") @Operation(summary="Cadastra produto global",security=@SecurityRequirement(name="bearerAuth"))
 @ApiResponse(responseCode="201",description="Produto cadastrado") @ApiResponse(responseCode="403",description="Somente ADMIN",content=@Content(schema=@Schema(implementation=ApiError.class))) @ApiResponse(responseCode="409",description="SKU duplicado",content=@Content(schema=@Schema(implementation=ApiError.class))) @ApiResponse(responseCode="422",description="Dados inválidos",content=@Content(schema=@Schema(implementation=ApiError.class)))
 public ResponseEntity<ProdutoResponse> criar(@Valid @RequestBody CriarProdutoRequest r){var p=service.criarProduto(r.sku(),r.nome(),r.descricao());return ResponseEntity.created(URI.create("/api/v1/produtos/"+p.id())).body(ProdutoResponse.from(p));}
 @GetMapping @Operation(summary="Lista produtos",security=@SecurityRequirement(name="bearerAuth"))
 @ApiResponse(responseCode="200",description="Página de produtos") @ApiResponse(responseCode="401",description="JWT ausente ou inválido",content=@Content(schema=@Schema(implementation=ApiError.class)))
 public Page<ProdutoResponse> listar(@RequestParam(required=false)Boolean ativo,@RequestParam(required=false)String termo,Pageable pageable){return service.listarProdutos(ativo,termo,pageable).map(ProdutoResponse::from);}
 @GetMapping("/{id}") @Operation(summary="Consulta produto",security=@SecurityRequirement(name="bearerAuth")) @ApiResponse(responseCode="200",description="Produto encontrado") @ApiResponse(responseCode="404",description="Produto inexistente",content=@Content(schema=@Schema(implementation=ApiError.class))) public ProdutoResponse buscar(@PathVariable UUID id){return ProdutoResponse.from(service.buscarProduto(id));}
 @PatchMapping("/{id}") @PreAuthorize("hasRole('ADMIN')") @Operation(summary="Atualiza produto global",security=@SecurityRequirement(name="bearerAuth"))
 @ApiResponse(responseCode="200",description="Produto atualizado") @ApiResponse(responseCode="403",description="Somente ADMIN",content=@Content(schema=@Schema(implementation=ApiError.class))) @ApiResponse(responseCode="404",description="Produto inexistente",content=@Content(schema=@Schema(implementation=ApiError.class)))
 public ProdutoResponse atualizar(@PathVariable UUID id,@Valid @RequestBody AtualizarProdutoRequest r){return ProdutoResponse.from(service.atualizarProduto(id,r.nome(),r.descricao(),r.ativo()));}
}
