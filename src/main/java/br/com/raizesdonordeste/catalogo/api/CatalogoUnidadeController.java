package br.com.raizesdonordeste.catalogo.api;
import java.util.UUID;
import br.com.raizesdonordeste.catalogo.application.CatalogoService; import br.com.raizesdonordeste.unidades.application.AcessoUnidadeService;
import io.swagger.v3.oas.annotations.Operation; import io.swagger.v3.oas.annotations.security.SecurityRequirement; import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid; import org.springframework.data.domain.*; import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken; import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.responses.ApiResponse; import io.swagger.v3.oas.annotations.media.*; import br.com.raizesdonordeste.shared.api.ApiError;
@RestController @RequestMapping("/api/v1/unidades/{unidadeId}") @Tag(name="Catálogo por unidade")
public class CatalogoUnidadeController{
 private final CatalogoService service; private final AcessoUnidadeService acesso;
 public CatalogoUnidadeController(CatalogoService service,AcessoUnidadeService acesso){this.service=service;this.acesso=acesso;}
 @PutMapping("/produtos/{produtoId}") @Operation(summary="Configura oferta na unidade",security=@SecurityRequirement(name="bearerAuth"))
 @ApiResponse(responseCode="200",description="Oferta criada ou atualizada") @ApiResponse(responseCode="403",description="Gerente sem vínculo",content=@Content(schema=@Schema(implementation=ApiError.class))) @ApiResponse(responseCode="404",description="Unidade ou produto inexistente",content=@Content(schema=@Schema(implementation=ApiError.class))) @ApiResponse(responseCode="422",description="Preço inválido",content=@Content(schema=@Schema(implementation=ApiError.class)))
 public OfertaResponse configurar(@PathVariable UUID unidadeId,@PathVariable UUID produtoId,@Valid @RequestBody ConfigurarOfertaRequest r,JwtAuthenticationToken auth){
  acesso.validarOperacao(unidadeId,UUID.fromString(auth.getName()),role(auth,"ADMIN"),role(auth,"GERENTE"));return OfertaResponse.from(service.configurarOferta(unidadeId,produtoId,r.preco(),r.disponivel()));}
 @GetMapping("/cardapio") @Operation(summary="Consulta cardápio público disponível")
 @ApiResponse(responseCode="200",description="Página somente com ofertas comercialmente disponíveis") @ApiResponse(responseCode="404",description="Unidade inexistente",content=@Content(schema=@Schema(implementation=ApiError.class)))
 public Page<CardapioItemResponse> cardapio(@PathVariable UUID unidadeId,Pageable pageable){return service.cardapio(unidadeId,pageable).map(CardapioItemResponse::from);}
 private boolean role(JwtAuthenticationToken a,String r){return a.getAuthorities().stream().anyMatch(x->x.getAuthority().equals("ROLE_"+r));}
}
