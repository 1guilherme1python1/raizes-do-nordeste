package br.com.raizesdonordeste.pagamentos.api;

import java.time.Instant;
import java.util.*;
import br.com.raizesdonordeste.pagamentos.application.PagamentoService;
import br.com.raizesdonordeste.pedidos.application.AtorPedido;
import br.com.raizesdonordeste.pedidos.domain.StatusPedido;
import br.com.raizesdonordeste.shared.api.ApiError;
import br.com.raizesdonordeste.usuarios.domain.Perfil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.*;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.*;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@RestController @RequestMapping("/api/v1/pedidos/{id}") @SecurityRequirement(name="bearerAuth")
public class PagamentoController {
    private final PagamentoService service;
    public PagamentoController(PagamentoService service){this.service=service;}
    @PostMapping("/pagamentos") @Operation(summary="Solicita pagamento mock",description="CLIENTE no próprio pedido, ATENDENTE/GERENTE vinculados ou ADMIN. Recusa é resultado de negócio com HTTP 201; replay retorna 200. Pedido fora do escopo retorna 403.")
    @ApiResponses({@ApiResponse(responseCode="201",description="Resultado simulado criado, aprovado ou recusado",content=@Content(schema=@Schema(implementation=PagamentoService.Resultado.class))),@ApiResponse(responseCode="200",description="Replay idempotente",content=@Content(schema=@Schema(implementation=PagamentoService.Resultado.class))),@ApiResponse(responseCode="400",description="Cabeçalho ausente ou corpo inválido",content=@Content(schema=@Schema(implementation=ApiError.class),examples=@ExampleObject(value="{\"error\":\"invalid_request\",\"message\":\"Cabeçalho obrigatório ausente\"}"))),@ApiResponse(responseCode="401",description="Autenticação ausente ou inválida",content=@Content(schema=@Schema(implementation=ApiError.class),examples=@ExampleObject(value="{\"error\":\"unauthorized\",\"message\":\"Autenticação necessária ou token inválido\"}"))),@ApiResponse(responseCode="403",description="Acesso fora do escopo",content=@Content(schema=@Schema(implementation=ApiError.class),examples=@ExampleObject(value="{\"error\":\"forbidden\",\"message\":\"Acesso negado\"}"))),@ApiResponse(responseCode="404",description="Pedido inexistente",content=@Content(schema=@Schema(implementation=ApiError.class))),@ApiResponse(responseCode="409",description="Pedido inelegível, reserva expirada ou conflito de idempotência",content=@Content(schema=@Schema(implementation=ApiError.class),examples=@ExampleObject(value="{\"error\":\"conflict\",\"message\":\"Reserva expirada\",\"details\":[\"RESERVA_EXPIRADA\"]}"))),@ApiResponse(responseCode="422",description="Dados inválidos",content=@Content(schema=@Schema(implementation=ApiError.class)))})
    @Transactional(propagation=Propagation.NEVER)
    public ResponseEntity<?> pagar(@PathVariable UUID id,@Parameter(required=true,description="Chave idempotente do solicitante e pedido, com até 100 caracteres") @RequestHeader("Idempotency-Key")String chave,
            @Valid @RequestBody PagamentoRequest body,JwtAuthenticationToken auth,HttpServletRequest request){
        var resultado=service.solicitar(id,ator(auth),chave,body.cenario().name());
        if(resultado.reservaExpirada())return ResponseEntity.status(409).body(new ApiError("conflict","Reserva expirada",List.of("RESERVA_EXPIRADA"),Instant.now(),request.getRequestURI(),request.getHeader("X-Request-Id")));
        return ResponseEntity.status(resultado.repetida()?200:201).body(resultado);
    }
    @GetMapping("/pagamentos") @Operation(summary="Consulta o resultado do pagamento",description="CLIENTE proprietário, ATENDENTE/GERENTE vinculados ou ADMIN; fora do escopo retorna 403.")
    @ApiResponses({@ApiResponse(responseCode="200",description="Resultados"),@ApiResponse(responseCode="401",description="Autenticação necessária",content=@Content(schema=@Schema(implementation=ApiError.class))),@ApiResponse(responseCode="403",description="Acesso fora do escopo",content=@Content(schema=@Schema(implementation=ApiError.class),examples=@ExampleObject(value="{\"error\":\"forbidden\",\"message\":\"Acesso negado\"}"))),@ApiResponse(responseCode="404",description="Pedido inexistente",content=@Content(schema=@Schema(implementation=ApiError.class)))})
    public List<PagamentoService.Resultado> consultar(@PathVariable UUID id,JwtAuthenticationToken auth){return service.consultar(id,ator(auth));}
    @PatchMapping("/status") @Operation(summary="Avança o estado operacional",description="COZINHA vinculada: EM_PREPARO e PRONTO; ATENDENTE vinculado: ENTREGUE; GERENTE vinculado ou ADMIN: as três transições. Fora do escopo retorna 403.")
    @ApiResponses({@ApiResponse(responseCode="200",description="Estado atualizado"),@ApiResponse(responseCode="400",description="Corpo inválido",content=@Content(schema=@Schema(implementation=ApiError.class))),@ApiResponse(responseCode="401",description="Autenticação necessária",content=@Content(schema=@Schema(implementation=ApiError.class))),@ApiResponse(responseCode="403",description="Perfil ou unidade não autorizados",content=@Content(schema=@Schema(implementation=ApiError.class))),@ApiResponse(responseCode="404",description="Pedido inexistente",content=@Content(schema=@Schema(implementation=ApiError.class))),@ApiResponse(responseCode="409",description="Transição inválida",content=@Content(schema=@Schema(implementation=ApiError.class),examples=@ExampleObject(value="{\"error\":\"conflict\",\"message\":\"Transição de estado inválida\"}"))),@ApiResponse(responseCode="422",description="Dados inválidos",content=@Content(schema=@Schema(implementation=ApiError.class)))})
    public Map<String,StatusPedido> avancar(@PathVariable UUID id,@Valid @RequestBody StatusRequest body,JwtAuthenticationToken auth){return Map.of("status",service.avancar(id,ator(auth),body.status()));}
    private AtorPedido ator(JwtAuthenticationToken a){var perfis=new HashSet<Perfil>();a.getAuthorities().forEach(x->{try{perfis.add(Perfil.valueOf(x.getAuthority().replace("ROLE_","")));}catch(IllegalArgumentException ignored){}});return new AtorPedido(UUID.fromString(a.getToken().getSubject()),Set.copyOf(perfis));}
    public enum Cenario { APROVAR, RECUSAR }
    public record PagamentoRequest(@NotNull Cenario cenario){}
    public record StatusRequest(@NotNull StatusPedido status){}
}
