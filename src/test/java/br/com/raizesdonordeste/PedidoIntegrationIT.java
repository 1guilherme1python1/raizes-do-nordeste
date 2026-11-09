package br.com.raizesdonordeste;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import br.com.raizesdonordeste.auth.application.AuthService;
import br.com.raizesdonordeste.catalogo.application.CatalogoService;
import br.com.raizesdonordeste.estoque.application.EstoqueService;
import br.com.raizesdonordeste.estoque.domain.TipoMovimentacaoEstoque;
import br.com.raizesdonordeste.estoque.infrastructure.persistence.*;
import br.com.raizesdonordeste.pedidos.application.*;
import br.com.raizesdonordeste.pedidos.application.PedidoService.*;
import br.com.raizesdonordeste.pedidos.domain.*;
import br.com.raizesdonordeste.pedidos.infrastructure.persistence.PedidoJpaRepository;
import br.com.raizesdonordeste.shared.application.ConflitoException;
import br.com.raizesdonordeste.unidades.application.*;
import br.com.raizesdonordeste.usuarios.application.*;
import br.com.raizesdonordeste.usuarios.domain.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.*;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.*;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.*;

@ActiveProfiles("test")
@SpringBootTest(properties={"app.security.jwt.secret=MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=","app.security.jwt.issuer=integration-test-issuer","app.security.jwt.audience=integration-test-audience","app.pedidos.expiration.enabled=false"})
@AutoConfigureMockMvc @Testcontainers @Import(PedidoIntegrationIT.ClockConfig.class)
class PedidoIntegrationIT {
 @Container @ServiceConnection static final PostgreSQLContainer<?> POSTGRESQL=new PostgreSQLContainer<>("postgres:16.10-alpine3.22");
 @Autowired MockMvc mvc; @Autowired PedidoService pedidos; @Autowired PedidoExpirationProcessor expiration;
 @Autowired UnidadeService unidades; @Autowired CatalogoService catalogo; @Autowired EstoqueService estoque;
 @Autowired EstoqueJpaRepository saldos; @Autowired MovimentacaoEstoqueJpaRepository movimentos; @Autowired PedidoJpaRepository pedidoRepo;
 @Autowired AcessoUnidadeService acesso; @Autowired UsuarioService usuarios; @Autowired AuthService auth; @Autowired MutableClock clock;

 @Test void t01ClienteCriaPedidoValido()throws Exception{var f=fixture(10);criar(f,f.cliente,"k1",body(f,"APP",1)).andExpect(status().isCreated()).andExpect(jsonPath("$.status").value("AGUARDANDO_PAGAMENTO"));}
 @Test void t02RegistraCanal()throws Exception{var f=fixture(10);criar(f,f.cliente,"k2",body(f,"PICKUP",1)).andExpect(jsonPath("$.canalPedido").value("PICKUP"));}
 @Test void t03MultiplosItensCalculamTotal()throws Exception{var f=fixture(10);var p=produto(f,5,new BigDecimal("7.25"));criar(f,f.cliente,"k3","{\"unidadeId\":\""+f.unidadeId+"\",\"canalPedido\":\"APP\",\"formaPagamento\":\"MOCK\",\"itens\":[{\"produtoId\":\""+f.produtoId+"\",\"quantidade\":2},{\"produtoId\":\""+p+"\",\"quantidade\":3}]}").andExpect(jsonPath("$.valorTotal").value(46.75));}
 @Test void t04SnapshotDePreco()throws Exception{var f=fixture(10);criar(f,f.cliente,"k4",body(f,"APP",2)).andExpect(jsonPath("$.itens[0].precoUnitario").value(12.5));}
 @Test void t05ProdutoInexistente404()throws Exception{var f=fixture(10);criar(f,f.cliente,"k5",body(f,UUID.randomUUID(),"APP",1)).andExpect(status().isNotFound());}
 @Test void t06ProdutoIndisponivel409()throws Exception{var f=fixture(10);catalogo.configurarOferta(f.unidadeId,f.produtoId,new BigDecimal("12.50"),false);criar(f,f.cliente,"k6",body(f,"APP",1)).andExpect(status().isConflict());}
 @Test void t07SaldoInsuficiente409()throws Exception{var f=fixture(1);criar(f,f.cliente,"k7",body(f,"APP",2)).andExpect(status().isConflict());}
 @Test void t08CanalAusente422()throws Exception{var f=fixture(1);criar(f,f.cliente,"k8","{\"unidadeId\":\""+f.unidadeId+"\",\"formaPagamento\":\"MOCK\",\"itens\":[{\"produtoId\":\""+f.produtoId+"\",\"quantidade\":1}]}").andExpect(status().isUnprocessableEntity());}
 @Test void t09CanalInvalido400()throws Exception{var f=fixture(1);criar(f,f.cliente,"k9",body(f,"FAX",1)).andExpect(status().isBadRequest());}
 @Test void t10SemToken401()throws Exception{var f=fixture(1);mvc.perform(post("/api/v1/pedidos").header("Idempotency-Key","k10").contentType(MediaType.APPLICATION_JSON).content(body(f,"APP",1))).andExpect(status().isUnauthorized());}
 @Test void t11ClienteNaoAcessaPedidoAlheio()throws Exception{var f=fixture(2);var id=id(criar(f,f.cliente,"k11",body(f,"APP",1)));var outro=user(Perfil.CLIENTE);mvc.perform(get("/api/v1/pedidos/{id}",id).header("Authorization",bearer(outro))).andExpect(status().isForbidden());}
 @Test void t12GerenteForaDaUnidade403()throws Exception{var f=fixture(2);var id=id(criar(f,f.cliente,"k12",body(f,"APP",1)));var gerente=user(Perfil.GERENTE);var outra=unidades.criar("UND-"+UUID.randomUUID(),"Outra");acesso.associar(outra.id(),gerente.id);mvc.perform(get("/api/v1/pedidos/{id}",id).header("Authorization",bearer(gerente))).andExpect(status().isForbidden());}
 @Test void t13CriacaoReservaEstoque()throws Exception{var f=fixture(3);criar(f,f.cliente,"k13",body(f,"APP",2)).andExpect(status().isCreated());assertThat(saldo(f).getQuantidadeReservada()).isEqualTo(2);}
 @Test void t14FalhaNaoDeixaReservaParcial()throws Exception{var f=fixture(3);var p=produto(f,0,new BigDecimal("5"));criar(f,f.cliente,"k14","{\"unidadeId\":\""+f.unidadeId+"\",\"canalPedido\":\"APP\",\"formaPagamento\":\"MOCK\",\"itens\":[{\"produtoId\":\""+f.produtoId+"\",\"quantidade\":1},{\"produtoId\":\""+p+"\",\"quantidade\":1}]}").andExpect(status().isConflict());assertThat(saldos.findByUnidadeId(f.unidadeId)).allMatch(e->e.getQuantidadeReservada()==0);}
 @Test void t15RepeticaoIdempotente()throws Exception{var f=fixture(3);var a=id(criar(f,f.cliente,"k15",body(f,"APP",1)));var b=id(criar(f,f.cliente,"k15",body(f,"APP",1)));assertThat(b).isEqualTo(a);assertThat(pedidoRepo.findByCriadoPorUsuarioIdAndUnidadeIdAndIdempotencyKey(f.cliente.id,f.unidadeId,"k15")).isPresent();assertThat(saldo(f).getQuantidadeReservada()).isEqualTo(1);}
 @Test void t16ChaveComConteudoDiferente409()throws Exception{var f=fixture(3);criar(f,f.cliente,"k16",body(f,"APP",1));criar(f,f.cliente,"k16",body(f,"APP",2)).andExpect(status().isConflict());}
 @Test void t17CancelamentoLibera()throws Exception{var f=fixture(2);var id=id(criar(f,f.cliente,"k17",body(f,"APP",1)));cancelar(id,f.cliente).andExpect(status().isOk()).andExpect(jsonPath("$.status").value("CANCELADO"));assertThat(saldo(f).getQuantidadeReservada()).isZero();}
 @Test void t18CancelamentoRepetidoNaoDuplica()throws Exception{var f=fixture(2);var id=id(criar(f,f.cliente,"k18",body(f,"APP",1)));cancelar(id,f.cliente);cancelar(id,f.cliente).andExpect(status().isOk());assertThat(movimentos.findByUnidadeId(f.unidadeId,PageRequest.of(0,20)).stream().filter(m->m.getTipo()==TipoMovimentacaoEstoque.LIBERACAO).count()).isEqualTo(1);}
 @Test void t19ExpiracaoSemEsperaLibera(){var f=fixture(2);var ator=new AtorPedido(f.cliente.id,Set.of(Perfil.CLIENTE));var r=pedidos.criar(cmd(f,1),ator,"k19");clock.advance(Duration.ofMinutes(16));expiration.expirar(r.pedido().id());assertThat(pedidoRepo.findById(r.pedido().id()).orElseThrow().getStatus()).isEqualTo(StatusPedido.EXPIRADO);assertThat(saldo(f).getQuantidadeReservada()).isZero();}
 @Test void t20ConcorrenciaNaoExcedeDisponivel()throws Exception{var f=fixture(1);var ator=new AtorPedido(f.cliente.id,Set.of(Perfil.CLIENTE));var ok=new AtomicInteger();var inicio=new CountDownLatch(1);try(var ex=Executors.newFixedThreadPool(2)){for(int i=0;i<2;i++){var k="k20-"+i;ex.submit(()->{try{inicio.await();pedidos.criar(cmd(f,1),ator,k);ok.incrementAndGet();}catch(Exception ignored){}});}inicio.countDown();ex.shutdown();assertThat(ex.awaitTermination(20,TimeUnit.SECONDS)).isTrue();}assertThat(ok).hasValue(1);assertThat(saldo(f).getQuantidadeReservada()).isEqualTo(1);}
 @Test void t21FiltroCanal(){var f=fixture(3);var ator=new AtorPedido(f.cliente.id,Set.of(Perfil.CLIENTE));pedidos.criar(cmd(f,1),ator,"k21a");pedidos.criar(new CriarPedido(f.unidadeId,null,CanalPedido.WEB,FormaPagamento.MOCK,List.of(new ItemSolicitado(f.produtoId,1))),ator,"k21b");assertThat(pedidos.listar(null,null,CanalPedido.WEB,ator,PageRequest.of(0,10))).hasSize(1);}
 @Test void t22FiltroStatus(){var f=fixture(3);var ator=new AtorPedido(f.cliente.id,Set.of(Perfil.CLIENTE));var p=pedidos.criar(cmd(f,1),ator,"k22").pedido();pedidos.cancelar(p.id(),ator);assertThat(pedidos.listar(null,StatusPedido.CANCELADO,null,ator,PageRequest.of(0,10))).hasSize(1);}
 @Test void t23MesmaChaveConcorrenteUmPedido()throws Exception{var f=fixture(3);var ator=new AtorPedido(f.cliente.id,Set.of(Perfil.CLIENTE));var inicio=new CountDownLatch(1);try(var ex=Executors.newFixedThreadPool(2)){for(int i=0;i<2;i++)ex.submit(()->{try{inicio.await();pedidos.criar(cmd(f,1),ator,"k23");}catch(Exception e){throw new RuntimeException(e);}});inicio.countDown();ex.shutdown();assertThat(ex.awaitTermination(20,TimeUnit.SECONDS)).isTrue();}assertThat(pedidoRepo.findByCriadoPorUsuarioIdAndUnidadeIdAndIdempotencyKey(f.cliente.id,f.unidadeId,"k23")).isPresent();assertThat(saldo(f).getQuantidadeReservada()).isEqualTo(1);}
 @Test void t24PrecoDoClienteIgnorado()throws Exception{var f=fixture(2);var json=body(f,"APP",1).replace("\"quantidade\":1","\"quantidade\":1,\"precoUnitario\":0.01,\"subtotal\":0.01").replace("\"itens\"","\"valorTotal\":0.01,\"itens\"");criar(f,f.cliente,"k24",json).andExpect(jsonPath("$.valorTotal").value(12.5));}

 private Fixture fixture(int qtd){var admin=user(Perfil.ADMIN);var cliente=user(Perfil.CLIENTE);var u=unidades.criar("UND-"+UUID.randomUUID(),"Teste");var p=catalogo.criarProduto("SKU-"+UUID.randomUUID(),"Produto",null);catalogo.configurarOferta(u.id(),p.id(),new BigDecimal("12.50"),true);if(qtd>0)estoque.movimentar(u.id(),admin.id,true,false,"CARGA-"+UUID.randomUUID(),p.id(),TipoMovimentacaoEstoque.ENTRADA,qtd,"Teste");return new Fixture(u.id(),p.id(),admin,cliente);}
 private UUID produto(Fixture f,int qtd,BigDecimal preco){var p=catalogo.criarProduto("SKU-"+UUID.randomUUID(),"Outro",null);catalogo.configurarOferta(f.unidadeId,p.id(),preco,true);if(qtd>0)estoque.movimentar(f.unidadeId,f.admin.id,true,false,"CARGA-"+UUID.randomUUID(),p.id(),TipoMovimentacaoEstoque.ENTRADA,qtd,"Teste");return p.id();}
 private EstoqueEntity saldo(Fixture f){return saldos.findByUnidadeId(f.unidadeId).stream().filter(e->catalogo.buscarProdutoPorOferta(e.getProdutoUnidadeId(),f.unidadeId).equals(f.produtoId)).findFirst().orElseThrow();}
 private CriarPedido cmd(Fixture f,int qtd){return new CriarPedido(f.unidadeId,null,CanalPedido.APP,FormaPagamento.MOCK,List.of(new ItemSolicitado(f.produtoId,qtd)));}
 private String body(Fixture f,String canal,int qtd){return body(f,f.produtoId,canal,qtd);} private String body(Fixture f,UUID p,String canal,int qtd){return "{\"unidadeId\":\""+f.unidadeId+"\",\"canalPedido\":\""+canal+"\",\"formaPagamento\":\"MOCK\",\"itens\":[{\"produtoId\":\""+p+"\",\"quantidade\":"+qtd+"}]}";}
 private org.springframework.test.web.servlet.ResultActions criar(Fixture f,User u,String k,String b)throws Exception{return mvc.perform(post("/api/v1/pedidos").header("Authorization",bearer(u)).header("Idempotency-Key",k).contentType(MediaType.APPLICATION_JSON).content(b));}
 private org.springframework.test.web.servlet.ResultActions cancelar(UUID id,User u)throws Exception{return mvc.perform(post("/api/v1/pedidos/{id}/cancelamento",id).header("Authorization",bearer(u)));}
 private UUID id(org.springframework.test.web.servlet.ResultActions r)throws Exception{return UUID.fromString(new com.fasterxml.jackson.databind.ObjectMapper().readTree(r.andReturn().getResponse().getContentAsString()).get("id").asText());}
 private User user(Perfil p){var email=p+"-"+UUID.randomUUID()+"@example.invalid";var u=usuarios.criarInterno(new CriarUsuarioCommand("Teste",email,"senha-segura-123",p));return new User(u.id(),auth.login(email,"senha-segura-123").accessToken().value());}
 private String bearer(User u){return "Bearer "+u.token;} private record User(UUID id,String token){} private record Fixture(UUID unidadeId,UUID produtoId,User admin,User cliente){}

 @TestConfiguration static class ClockConfig{@Bean @Primary MutableClock mutableClock(){return new MutableClock(Instant.parse("2026-01-01T00:00:00Z"));}}
 static class MutableClock extends Clock{private Instant value;MutableClock(Instant v){value=v;}void advance(Duration d){value=value.plus(d);}public ZoneId getZone(){return ZoneOffset.UTC;}public Clock withZone(ZoneId z){return this;}public Instant instant(){return value;}}
}
