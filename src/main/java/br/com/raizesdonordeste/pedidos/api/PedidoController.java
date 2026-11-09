package br.com.raizesdonordeste.pedidos.api;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;
import br.com.raizesdonordeste.pedidos.application.*;
import br.com.raizesdonordeste.pedidos.application.PedidoService.*;
import br.com.raizesdonordeste.pedidos.domain.*;
import br.com.raizesdonordeste.usuarios.domain.Perfil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.*;
import io.swagger.v3.oas.annotations.responses.*;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.data.domain.*;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.*;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.*;

@RestController @RequestMapping("/api/v1/pedidos") @SecurityRequirement(name="bearerAuth")
public class PedidoController {
    private final PedidoService service;
    public PedidoController(PedidoService service){this.service=service;}

    @PostMapping @Operation(summary="Cria pedido e reserva seu estoque atomicamente",description="CLIENTE: APP/WEB/PICKUP/TOTEM; ATENDENTE vinculado: BALCAO/TOTEM; GERENTE vinculado ou ADMIN. Repetição idêntica retorna 200.")
    @ApiResponses({@ApiResponse(responseCode="201",description="Pedido criado"),@ApiResponse(responseCode="200",description="Repetição idempotente"),@ApiResponse(responseCode="401",description="JWT ausente/inválido"),@ApiResponse(responseCode="403",description="Canal ou unidade não autorizados"),@ApiResponse(responseCode="404",description="Unidade/produto/oferta inexistente"),@ApiResponse(responseCode="409",description="Estoque insuficiente ou conflito de idempotência"),@ApiResponse(responseCode="422",description="Dados inválidos")})
    public ResponseEntity<PedidoResponse> criar(@Parameter(description="Chave idempotente de até 100 caracteres",required=true,example="checkout-8dc1d71f") @RequestHeader("Idempotency-Key") String chave,
            @Valid @RequestBody CriarPedidoRequest request,JwtAuthenticationToken auth){
        var r=service.criar(new CriarPedido(request.unidadeId(),request.clienteId(),request.canalPedido(),request.formaPagamento(),request.itens().stream().map(i->new ItemSolicitado(i.produtoId(),i.quantidade())).toList()),ator(auth),chave);
        return ResponseEntity.status(r.repetida()?HttpStatus.OK:HttpStatus.CREATED).body(response(r.pedido()));
    }
    @GetMapping @Operation(summary="Lista pedidos respeitando o escopo do usuário",description="CLIENTE vê os próprios; funcionários e COZINHA veem unidades vinculadas; ADMIN possui escopo global.")
    public Page<PedidoResponse> listar(@RequestParam(required=false)UUID unidadeId,@RequestParam(required=false)StatusPedido status,@RequestParam(required=false)CanalPedido canalPedido,@PageableDefault(size=20)Pageable pageable,JwtAuthenticationToken auth){return service.listar(unidadeId,status,canalPedido,ator(auth),pageable).map(this::response);}
    @GetMapping("/{id}") @Operation(summary="Consulta um pedido") @ApiResponse(responseCode="403",description="Fora do escopo") @ApiResponse(responseCode="404",description="Pedido inexistente")
    public PedidoResponse buscar(@PathVariable UUID id,JwtAuthenticationToken auth){return response(service.buscar(id,ator(auth)));}
    @PostMapping("/{id}/cancelamento") @Operation(summary="Cancela pedido pendente e libera a reserva",description="Idempotente quando o pedido já está CANCELADO; outros estados terminais retornam 409.")
    public PedidoResponse cancelar(@PathVariable UUID id,JwtAuthenticationToken auth){return response(service.cancelar(id,ator(auth)));}

    private AtorPedido ator(JwtAuthenticationToken a){var perfis=new HashSet<Perfil>();a.getAuthorities().forEach(x->{try{perfis.add(Perfil.valueOf(x.getAuthority().replace("ROLE_","")));}catch(Exception ignored){}});return new AtorPedido(UUID.fromString(a.getToken().getSubject()),Set.copyOf(perfis));}
    private PedidoResponse response(Pedido p){return new PedidoResponse(p.id(),p.unidadeId(),p.clienteId(),p.criadoPorUsuarioId(),p.canalPedido(),p.status(),p.formaPagamento(),p.valorTotal(),p.moeda(),p.reservaExpiraEm(),p.itens().stream().map(i->new ItemResponse(i.id(),i.produtoId(),i.quantidade(),i.precoUnitario(),i.subtotal())).toList(),p.criadoEm(),p.atualizadoEm());}
    @Schema(example="{\"unidadeId\":\"2e5d26e7-2442-4f81-b528-6aa595165f9e\",\"canalPedido\":\"APP\",\"formaPagamento\":\"MOCK\",\"itens\":[{\"produtoId\":\"6bf17a62-830a-4c78-9347-bcf52e38468d\",\"quantidade\":2}]}")
    public record CriarPedidoRequest(@NotNull UUID unidadeId,UUID clienteId,@NotNull @Schema(description="APP, TOTEM, BALCAO, PICKUP ou WEB") CanalPedido canalPedido,@NotNull FormaPagamento formaPagamento,@NotEmpty List<@Valid CriarItemRequest> itens){}
    public record CriarItemRequest(@NotNull UUID produtoId,@Positive int quantidade){}
    @Schema(example="{\"id\":\"fd3e6998-8eb4-46a7-92e8-c740b42cb7db\",\"unidadeId\":\"2e5d26e7-2442-4f81-b528-6aa595165f9e\",\"canalPedido\":\"APP\",\"status\":\"AGUARDANDO_PAGAMENTO\",\"formaPagamento\":\"MOCK\",\"valorTotal\":25.00,\"moeda\":\"BRL\",\"reservaExpiraEm\":\"2026-11-09T00:15:00Z\",\"itens\":[{\"produtoId\":\"6bf17a62-830a-4c78-9347-bcf52e38468d\",\"quantidade\":2,\"precoUnitario\":12.50,\"subtotal\":25.00}]}")
    public record PedidoResponse(UUID id,UUID unidadeId,UUID clienteId,UUID criadoPorUsuarioId,CanalPedido canalPedido,StatusPedido status,FormaPagamento formaPagamento,BigDecimal valorTotal,String moeda,Instant reservaExpiraEm,List<ItemResponse> itens,Instant criadoEm,Instant atualizadoEm){}
    public record ItemResponse(UUID id,UUID produtoId,int quantidade,BigDecimal precoUnitario,BigDecimal subtotal){}
}
