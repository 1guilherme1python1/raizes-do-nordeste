package br.com.raizesdonordeste.estoque.api;
import java.util.UUID;
import br.com.raizesdonordeste.estoque.application.EstoqueService;
import br.com.raizesdonordeste.estoque.application.EstoqueService.*;
import br.com.raizesdonordeste.estoque.domain.TipoMovimentacaoEstoque;
import io.swagger.v3.oas.annotations.Operation; import io.swagger.v3.oas.annotations.Parameter; import io.swagger.v3.oas.annotations.security.SecurityRequirement; import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid; import jakarta.validation.constraints.Size;
import org.springframework.data.domain.*; import org.springframework.http.*; import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken; import org.springframework.validation.annotation.Validated; import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.responses.ApiResponse; import io.swagger.v3.oas.annotations.media.*; import io.swagger.v3.oas.annotations.media.ExampleObject; import br.com.raizesdonordeste.shared.api.ApiError;
@Validated @RestController @RequestMapping("/api/v1/unidades/{unidadeId}/estoque") @Tag(name="Estoque")
public class EstoqueController{
 private final EstoqueService service; public EstoqueController(EstoqueService service){this.service=service;}
 @GetMapping @Operation(summary="Consulta saldos da unidade",security=@SecurityRequirement(name="bearerAuth"))
 @ApiResponse(responseCode="200",description="Página de saldos") @ApiResponse(responseCode="401",description="JWT ausente ou inválido",content=@Content(schema=@Schema(implementation=ApiError.class))) @ApiResponse(responseCode="403",description="Gerente sem vínculo",content=@Content(schema=@Schema(implementation=ApiError.class)))
 public Page<SaldoEstoque> listar(@PathVariable UUID unidadeId,Pageable pageable,JwtAuthenticationToken auth){return service.listar(unidadeId,id(auth),role(auth,"ADMIN"),role(auth,"GERENTE"),pageable);}
 @PostMapping("/movimentacoes") @Operation(summary="Registra entrada ou saída idempotente",description="Exige Idempotency-Key; ADMIN ou GERENTE vinculado",security=@SecurityRequirement(name="bearerAuth"))
 @ApiResponse(responseCode="201",description="Movimentação registrada") @ApiResponse(responseCode="200",description="Repetição idempotente recuperada")
 @ApiResponse(responseCode="400",description="Idempotency-Key ausente",content=@Content(schema=@Schema(implementation=ApiError.class),examples=@ExampleObject(value="{\"error\":\"invalid_request\",\"message\":\"Cabeçalho obrigatório ausente\"}")))
 @ApiResponse(responseCode="403",description="Gerente sem vínculo",content=@Content(schema=@Schema(implementation=ApiError.class)))
 @ApiResponse(responseCode="409",description="Saldo, concorrência ou idempotência conflitante",content=@Content(schema=@Schema(implementation=ApiError.class),examples=@ExampleObject(value="{\"error\":\"conflict\",\"message\":\"Saldo disponível insuficiente\"}")))
 @ApiResponse(responseCode="422",description="Tipo ou quantidade inválida",content=@Content(schema=@Schema(implementation=ApiError.class)))
 public ResponseEntity<MovimentoRegistrado> movimentar(@PathVariable UUID unidadeId,
  @Parameter(required=true,description="Chave única no escopo da unidade") @RequestHeader("Idempotency-Key") @Size(max=100) String key,
  @Valid @RequestBody MovimentarEstoqueRequest request,JwtAuthenticationToken auth){
   var result=service.movimentar(unidadeId,id(auth),role(auth,"ADMIN"),role(auth,"GERENTE"),key,request.produtoId(),request.tipo(),request.quantidade(),request.motivo());
   return ResponseEntity.status(result.repetida()?HttpStatus.OK:HttpStatus.CREATED).body(result);}
 @GetMapping("/movimentacoes") @Operation(summary="Consulta histórico de movimentações",security=@SecurityRequirement(name="bearerAuth"))
 @ApiResponse(responseCode="200",description="Página de movimentos auditados") @ApiResponse(responseCode="401",description="JWT ausente ou inválido",content=@Content(schema=@Schema(implementation=ApiError.class))) @ApiResponse(responseCode="403",description="Gerente sem vínculo",content=@Content(schema=@Schema(implementation=ApiError.class)))
 public Page<MovimentoRegistrado> historico(@PathVariable UUID unidadeId,@RequestParam(required=false) TipoMovimentacaoEstoque tipo,Pageable pageable,JwtAuthenticationToken auth){return service.historico(unidadeId,id(auth),role(auth,"ADMIN"),role(auth,"GERENTE"),tipo,pageable);}
 private UUID id(JwtAuthenticationToken a){return UUID.fromString(a.getName());}
 private boolean role(JwtAuthenticationToken a,String r){return a.getAuthorities().stream().anyMatch(x->x.getAuthority().equals("ROLE_"+r));}
}
