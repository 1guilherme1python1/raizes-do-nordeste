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
import br.com.raizesdonordeste.pagamentos.application.PagamentoService;
import br.com.raizesdonordeste.pedidos.infrastructure.persistence.PedidoJpaRepository;
import br.com.raizesdonordeste.shared.application.ConflitoException;
import br.com.raizesdonordeste.unidades.application.*;
import br.com.raizesdonordeste.usuarios.application.*;
import br.com.raizesdonordeste.usuarios.domain.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;
import br.com.raizesdonordeste.pagamentos.infrastructure.MockGatewayPagamento;
import org.springframework.boot.test.mock.mockito.SpyBean;
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
 @Autowired PagamentoService pagamentos;
 @Autowired org.springframework.jdbc.core.JdbcTemplate jdbc;
 @Autowired org.springframework.transaction.PlatformTransactionManager transactionManager;
 @SpyBean MockGatewayPagamento gateway;
 @BeforeEach void limparChamadasGateway(){clearInvocations(gateway);}

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

 @Test void pagamentoAprovadoBaixaEReplay()throws Exception{var f=fixture(3);var pedido=id(criar(f,f.cliente,"pagar-aprovado",body(f,"APP",2)));var first=pagamentoId(pagar(pedido,f.cliente,"pag-key","APROVAR").andExpect(status().isCreated()).andExpect(jsonPath("$.statusPedido").value("CONFIRMADO")));verify(gateway,times(1)).processar(eq(pedido),any(),eq("BRL"),eq("APROVAR"));var replay=pagamentoId(pagar(pedido,f.cliente,"pag-key","APROVAR").andExpect(status().isOk()));assertThat(replay).isEqualTo(first);pagar(pedido,f.cliente,"pag-key","RECUSAR").andExpect(status().isConflict());pagar(pedido,f.cliente,"nova-chave","APROVAR").andExpect(status().isConflict());verify(gateway,times(1)).processar(eq(pedido),any(),eq("BRL"),eq("APROVAR"));assertThat(jdbc.queryForObject("select count(*) from app.pagamento where pedido_id=?",Integer.class,pedido)).isEqualTo(1);assertThat(jdbc.queryForObject("select count(*) from app.movimentacao_estoque where pedido_id=? and tipo='SAIDA'",Integer.class,pedido)).isEqualTo(1);assertThat(saldo(f).getQuantidadeFisica()).isEqualTo(1);assertThat(saldo(f).getQuantidadeReservada()).isZero();}
 @Test void pagamentoRecusadoLibera()throws Exception{var f=fixture(3);var pedido=id(criar(f,f.cliente,"pagar-recusado",body(f,"APP",2)));pagar(pedido,f.cliente,"pag-recusa","RECUSAR").andExpect(status().isCreated()).andExpect(jsonPath("$.statusPedido").value("CANCELADO"));assertThat(saldo(f).getQuantidadeFisica()).isEqualTo(3);assertThat(saldo(f).getQuantidadeReservada()).isZero();}
 @Test void pagamentoNoVencimentoPersisteExpiracao()throws Exception{var f=fixture(3);var pedido=id(criar(f,f.cliente,"vencimento",body(f,"APP",2)));clock.advance(Duration.ofMinutes(15));assertThat(clock.instant()).isEqualTo(pedidoRepo.findById(pedido).orElseThrow().getReservaExpiraEm());pagar(pedido,f.cliente,"pag-vencido","APROVAR").andExpect(status().isConflict());verify(gateway,never()).processar(eq(pedido),any(),any(),any());assertThat(pedidoRepo.findById(pedido).orElseThrow().getStatus()).isEqualTo(StatusPedido.EXPIRADO);assertThat(saldo(f).getQuantidadeFisica()).isEqualTo(3);assertThat(saldo(f).getQuantidadeReservada()).isZero();assertThat(jdbc.queryForObject("select count(*) from app.pagamento where pedido_id=?",Integer.class,pedido)).isZero();assertThat(jdbc.queryForObject("select count(*) from app.movimentacao_estoque where pedido_id=? and tipo='LIBERACAO'",Integer.class,pedido)).isEqualTo(1);}
 @Test void pedidoAvancaAteEntregue()throws Exception{var f=fixture(3);var pedido=id(criar(f,f.cliente,"fluxo",body(f,"APP",1)));pagar(pedido,f.cliente,"pag-fluxo","APROVAR").andExpect(status().isCreated());for(var estado:List.of("EM_PREPARO","PRONTO","ENTREGUE"))mvc.perform(patch("/api/v1/pedidos/{id}/status",pedido).header("Authorization",bearer(f.admin)).contentType(MediaType.APPLICATION_JSON).content("{\"status\":\""+estado+"\"}")).andExpect(status().isOk()).andExpect(jsonPath("$.status").value(estado));}
 @Test void pagamentosConcorrentesMesmaChave()throws Exception{var f=fixture(3);var pedido=id(criar(f,f.cliente,"concurrente",body(f,"APP",1)));var inicio=new CountDownLatch(1);try(var ex=Executors.newFixedThreadPool(2)){var a=ex.submit(()->{inicio.await();return pagar(pedido,f.cliente,"mesma","APROVAR");});var b=ex.submit(()->{inicio.await();return pagar(pedido,f.cliente,"mesma","APROVAR");});inicio.countDown();var ra=a.get(20,TimeUnit.SECONDS);var rb=b.get(20,TimeUnit.SECONDS);assertThat(Set.of(ra.andReturn().getResponse().getStatus(),rb.andReturn().getResponse().getStatus())).containsExactlyInAnyOrder(200,201);assertThat(pagamentoId(ra)).isEqualTo(pagamentoId(rb));}verify(gateway,times(1)).processar(eq(pedido),any(),eq("BRL"),eq("APROVAR"));assertThat(jdbc.queryForObject("select count(*) from app.pagamento where pedido_id=?",Integer.class,pedido)).isEqualTo(1);assertThat(jdbc.queryForObject("select count(*) from app.movimentacao_estoque where pedido_id=? and tipo='SAIDA'",Integer.class,pedido)).isEqualTo(1);assertThat(saldo(f).getQuantidadeFisica()).isEqualTo(2);assertThat(saldo(f).getQuantidadeReservada()).isZero();}
 @Test void pagamentosConcorrentesChavesDiferentes()throws Exception{var f=fixture(3);var pedido=id(criar(f,f.cliente,"concurrente2",body(f,"APP",1)));var ator=new AtorPedido(f.cliente.id,Set.of(Perfil.CLIENTE));var inicio=new CountDownLatch(1);var ok=new AtomicInteger();var conflitos=new AtomicInteger();try(var ex=Executors.newFixedThreadPool(2)){var futures=new ArrayList<Future<?>>();for(int i=0;i<2;i++){var chave="diferente"+i;futures.add(ex.submit(()->{try{inicio.await();pagamentos.solicitar(pedido,ator,chave,"APROVAR");ok.incrementAndGet();}catch(ConflitoException e){conflitos.incrementAndGet();}catch(InterruptedException e){Thread.currentThread().interrupt();throw new RuntimeException(e);}}));}inicio.countDown();for(var future:futures)future.get(20,TimeUnit.SECONDS);}assertThat(ok).hasValue(1);assertThat(conflitos).hasValue(1);assertThat(saldo(f).getQuantidadeFisica()).isEqualTo(2);assertThat(saldo(f).getQuantidadeReservada()).isZero();}
 @Test void pagamentoDisputaCancelamento()throws Exception{var f=fixture(3);var id=id(criar(f,f.cliente,"disputa-cancelamento",body(f,"APP",1)));var ator=new AtorPedido(f.cliente.id,Set.of(Perfil.CLIENTE));var inicio=new CountDownLatch(1);try(var ex=Executors.newFixedThreadPool(2)){var a=ex.submit(()->{inicio.await();try{pagamentos.solicitar(id,ator,"pag-disputa","APROVAR");}catch(ConflitoException ignored){}return null;});var b=ex.submit(()->{inicio.await();try{pedidos.cancelar(id,ator);}catch(ConflitoException ignored){}return null;});inicio.countDown();a.get(20,TimeUnit.SECONDS);b.get(20,TimeUnit.SECONDS);}var status=pedidoRepo.findById(id).orElseThrow().getStatus();assertThat(status).isIn(StatusPedido.CONFIRMADO,StatusPedido.CANCELADO);assertThat(saldo(f).getQuantidadeReservada()).isZero();assertThat(saldo(f).getQuantidadeFisica()).isEqualTo(status==StatusPedido.CONFIRMADO?2:3);}
 @Test void pagamentoDisputaExpiracao()throws Exception{var f=fixture(3);var id=id(criar(f,f.cliente,"disputa-expiracao",body(f,"APP",1)));var ator=new AtorPedido(f.cliente.id,Set.of(Perfil.CLIENTE));clock.advance(Duration.ofMinutes(15));var inicio=new CountDownLatch(1);try(var ex=Executors.newFixedThreadPool(2)){var a=ex.submit(()->{inicio.await();try{pagamentos.solicitar(id,ator,"pag-expira","APROVAR");}catch(ConflitoException ignored){}return null;});var b=ex.submit(()->{inicio.await();expiration.expirar(id);return null;});inicio.countDown();a.get(20,TimeUnit.SECONDS);b.get(20,TimeUnit.SECONDS);}assertThat(pedidoRepo.findById(id).orElseThrow().getStatus()).isEqualTo(StatusPedido.EXPIRADO);assertThat(saldo(f).getQuantidadeFisica()).isEqualTo(3);assertThat(saldo(f).getQuantidadeReservada()).isZero();assertThat(jdbc.queryForObject("select count(*) from app.movimentacao_estoque where pedido_id=? and tipo='LIBERACAO'",Integer.class,id)).isEqualTo(1);}
 @Test void falhaTecnicaRevertePagamentoETransicao()throws Exception{var f=fixture(3);var id=id(criar(f,f.cliente,"rollback-pagamento",body(f,"APP",1)));jdbc.update("update app.estoque set quantidade_reservada=0 where id=?",saldo(f).getId());var ator=new AtorPedido(f.cliente.id,Set.of(Perfil.CLIENTE));assertThatThrownBy(()->pagamentos.solicitar(id,ator,"falha","APROVAR")).isInstanceOf(IllegalArgumentException.class);assertThat(jdbc.queryForObject("select count(*) from app.pagamento where pedido_id=?",Integer.class,id)).isZero();assertThat(jdbc.queryForObject("select count(*) from app.movimentacao_estoque where pedido_id=? and tipo='SAIDA'",Integer.class,id)).isZero();assertThat(pedidoRepo.findById(id).orElseThrow().getStatus()).isEqualTo(StatusPedido.AGUARDANDO_PAGAMENTO);assertThat(saldo(f).getQuantidadeFisica()).isEqualTo(3);}
 @Test void pagamentoIsolaClienteUnidadeECozinha()throws Exception{var f=fixture(3);var id=id(criar(f,f.cliente,"isolamento-pagamento",body(f,"APP",1)));mvc.perform(post("/api/v1/pedidos/{id}/pagamentos",id).header("Idempotency-Key","x").contentType(MediaType.APPLICATION_JSON).content("{\"cenario\":\"APROVAR\"}")).andExpect(status().isUnauthorized());var outro=user(Perfil.CLIENTE);pagar(id,outro,"outro","APROVAR").andExpect(status().isForbidden());var gerente=user(Perfil.GERENTE);pagar(id,gerente,"gerente","APROVAR").andExpect(status().isForbidden());var cozinha=user(Perfil.COZINHA);acesso.associar(f.unidadeId,cozinha.id);pagar(id,cozinha,"cozinha","APROVAR").andExpect(status().isForbidden());}
 @Test void transicaoNaoPermiteSaltosNemCliente()throws Exception{var f=fixture(3);var id=id(criar(f,f.cliente,"transicoes-invalidas",body(f,"APP",1)));mvc.perform(patch("/api/v1/pedidos/{id}/status",id).header("Authorization",bearer(f.admin)).contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"PRONTO\"}")).andExpect(status().isConflict());mvc.perform(patch("/api/v1/pedidos/{id}/status",id).header("Authorization",bearer(f.cliente)).contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"CONFIRMADO\"}")).andExpect(status().isForbidden());}
 @Test void matrizPagamentoPorPerfilEUnidade()throws Exception{
  for(var perfil:List.of(Perfil.CLIENTE,Perfil.ATENDENTE,Perfil.GERENTE,Perfil.ADMIN)){
   var f=fixture(2);var pedido=id(criar(f,f.cliente,"matriz-"+perfil,body(f,"APP",1)));var ator=perfil==Perfil.CLIENTE?f.cliente:perfil==Perfil.ADMIN?f.admin:user(perfil);
   if(perfil==Perfil.ATENDENTE||perfil==Perfil.GERENTE)acesso.associar(f.unidadeId,ator.id);
   pagar(pedido,ator,"matriz-chave","APROVAR").andExpect(status().isCreated());
   mvc.perform(get("/api/v1/pedidos/{id}/pagamentos",pedido).header("Authorization",bearer(ator))).andExpect(status().isOk());
   var alheio=user(Perfil.CLIENTE);mvc.perform(get("/api/v1/pedidos/{id}/pagamentos",pedido).header("Authorization",bearer(alheio))).andExpect(status().isForbidden());
   var cozinha=user(Perfil.COZINHA);acesso.associar(f.unidadeId,cozinha.id);mvc.perform(get("/api/v1/pedidos/{id}/pagamentos",pedido).header("Authorization",bearer(cozinha))).andExpect(status().isForbidden());
   for(var foraPerfil:List.of(Perfil.ATENDENTE,Perfil.GERENTE)){var fora=user(foraPerfil);pagar(pedido,fora,"fora","APROVAR").andExpect(status().isForbidden());mvc.perform(get("/api/v1/pedidos/{id}/pagamentos",pedido).header("Authorization",bearer(fora))).andExpect(status().isForbidden());}
   mvc.perform(get("/api/v1/pedidos/{id}/pagamentos",pedido)).andExpect(status().isUnauthorized());
  }
 }
 @Test void matrizTransicoesOperacionais()throws Exception{
  for(var perfil:List.of(Perfil.COZINHA,Perfil.ATENDENTE,Perfil.GERENTE,Perfil.ADMIN)){
   var f=fixture(2);var pedido=id(criar(f,f.cliente,"operacional-"+perfil,body(f,"APP",1)));pagar(pedido,f.cliente,"operacional-pag","APROVAR").andExpect(status().isCreated());var funcionario=perfil==Perfil.ADMIN?f.admin:user(perfil);if(perfil!=Perfil.ADMIN)acesso.associar(f.unidadeId,funcionario.id);
   var fora=user(Perfil.GERENTE);mudarStatus(pedido,fora,"EM_PREPARO").andExpect(status().isForbidden());mvc.perform(patch("/api/v1/pedidos/{id}/status",pedido).contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"EM_PREPARO\"}")).andExpect(status().isUnauthorized());
   for(var estado:List.of("EM_PREPARO","PRONTO","ENTREGUE")){boolean permitido=perfil==Perfil.ADMIN||perfil==Perfil.GERENTE||(perfil==Perfil.COZINHA&&!estado.equals("ENTREGUE"))||(perfil==Perfil.ATENDENTE&&estado.equals("ENTREGUE"));if(permitido)mudarStatus(pedido,funcionario,estado).andExpect(status().isOk());else{mudarStatus(pedido,funcionario,estado).andExpect(status().isForbidden());mudarStatus(pedido,f.admin,estado).andExpect(status().isOk());}}
   mudarStatus(pedido,f.admin,"EM_PREPARO").andExpect(status().isConflict());
  }
 }
 @Test void openApiPublicadoDescreveContratoDePagamento()throws Exception{
  var json=new com.fasterxml.jackson.databind.ObjectMapper().readTree(mvc.perform(get("/v3/api-docs")).andExpect(status().isOk()).andReturn().getResponse().getContentAsString());
  var paths=json.path("paths");var pagamento=paths.path("/api/v1/pedidos/{id}/pagamentos");var operacao=paths.path("/api/v1/pedidos/{id}/status").path("patch");
  for(var op:List.of(pagamento.path("post"),pagamento.path("get"),operacao)){assertThat(op.path("security").toString()).contains("bearerAuth");for(var codigo:List.of("401","403","404"))assertThat(op.path("responses").has(codigo)).as(codigo).isTrue();}
  var post=pagamento.path("post");assertThat(post.path("parameters").toString()).contains("Idempotency-Key");for(var codigo:List.of("200","201","400","409","422"))assertThat(post.path("responses").has(codigo)).as(codigo).isTrue();assertThat(post.path("responses").path("409").toString()).contains("ApiError","RESERVA_EXPIRADA");assertThat(post.path("description").asText()).contains("Recusa é resultado de negócio");assertThat(pagamento.path("get").path("responses").path("403").toString()).contains("ApiError","Acesso negado");assertThat(operacao.path("responses").path("409").toString()).contains("ApiError","Transição de estado inválida");
 }
 @Test void auditoriaAdministrativaRegistraAtorAlvoUnidadeERejeitaRepeticaoOuNegacao()throws Exception{
  var f=fixture(2);var funcionario=user(Perfil.ATENDENTE);var url="/api/v1/unidades/{unidadeId}/usuarios/{usuarioId}";
  mvc.perform(put(url,f.unidadeId,funcionario.id).header("Authorization",bearer(f.cliente))).andExpect(status().isForbidden());
  assertThat(jdbc.queryForObject("select count(*) from app.auditoria_administrativa where unidade_id=? and usuario_alvo_id=?",Integer.class,f.unidadeId,funcionario.id)).isZero();
  mvc.perform(put(url,f.unidadeId,funcionario.id).header("Authorization",bearer(f.admin))).andExpect(status().isNoContent());
  mvc.perform(put(url,f.unidadeId,funcionario.id).header("Authorization",bearer(f.admin))).andExpect(status().isNoContent());
  var registro=jdbc.queryForMap("select ator_id,origem,acao,usuario_alvo_id,unidade_id,ocorrido_em from app.auditoria_administrativa where unidade_id=? and usuario_alvo_id=?",f.unidadeId,funcionario.id);
  assertThat(registro.get("ator_id")).isEqualTo(f.admin.id);assertThat(registro.get("origem")).isEqualTo("USUARIO");assertThat(registro.get("acao")).isEqualTo("ASSOCIAR_USUARIO_UNIDADE");assertThat(registro.get("usuario_alvo_id")).isEqualTo(funcionario.id);assertThat(registro.get("unidade_id")).isEqualTo(f.unidadeId);assertThat(registro.get("ocorrido_em")).isNotNull();
  mvc.perform(delete(url,f.unidadeId,funcionario.id).header("Authorization",bearer(f.admin))).andExpect(status().isNoContent());
  assertThat(jdbc.queryForObject("select count(*) from app.auditoria_administrativa where unidade_id=? and usuario_alvo_id=? and acao='DESASSOCIAR_USUARIO_UNIDADE'",Integer.class,f.unidadeId,funcionario.id)).isEqualTo(1);
 }
 @Test void auditoriaReverteComOperacao()throws Exception{
  var f=fixture(2);var funcionario=user(Perfil.GERENTE);var tx=new org.springframework.transaction.support.TransactionTemplate(transactionManager);
  assertThatThrownBy(()->tx.executeWithoutResult(s->{acesso.associar(f.unidadeId,funcionario.id,f.admin.id);throw new IllegalStateException("falha sintética");})).isInstanceOf(IllegalStateException.class);
  assertThat(acesso.possuiVinculo(f.unidadeId,funcionario.id)).isFalse();assertThat(jdbc.queryForObject("select count(*) from app.auditoria_administrativa where unidade_id=? and usuario_alvo_id=?",Integer.class,f.unidadeId,funcionario.id)).isZero();
 }
 @Test void cadastroInternoAuditadoNaoRetornaCredenciais()throws Exception{
  var f=fixture(1);var email="interno-"+UUID.randomUUID()+"@example.invalid";var conteudo="{\"nomeExibicao\":\"Pessoa sintética\",\"email\":\""+email+"\",\"senha\":\"senha-segura-123\",\"perfil\":\"ATENDENTE\"}";
  mvc.perform(post("/api/v1/usuarios").header("Authorization",bearer(f.cliente)).contentType(MediaType.APPLICATION_JSON).content(conteudo)).andExpect(status().isForbidden());
  var resposta=mvc.perform(post("/api/v1/usuarios").header("Authorization",bearer(f.admin)).contentType(MediaType.APPLICATION_JSON).content(conteudo)).andExpect(status().isCreated()).andExpect(jsonPath("$.senha").doesNotExist()).andExpect(jsonPath("$.senhaHash").doesNotExist());
  var alvo=id(resposta);var evento=jdbc.queryForMap("select ator_id,origem,acao,usuario_alvo_id,unidade_id,ocorrido_em from app.auditoria_administrativa where usuario_alvo_id=?",alvo);assertThat(evento.get("ator_id")).isEqualTo(f.admin.id);assertThat(evento.get("origem")).isEqualTo("USUARIO");assertThat(evento.get("acao")).isEqualTo("CRIAR_USUARIO_INTERNO");assertThat(evento.get("unidade_id")).isNull();assertThat(evento.get("ocorrido_em")).isNotNull();
  mvc.perform(get("/api/v1/usuarios/me").header("Authorization",bearer(f.cliente))).andExpect(status().isOk()).andExpect(jsonPath("$.senha").doesNotExist()).andExpect(jsonPath("$.senhaHash").doesNotExist());
 }
 private org.springframework.test.web.servlet.ResultActions mudarStatus(UUID id,User u,String valor)throws Exception{return mvc.perform(patch("/api/v1/pedidos/{id}/status",id).header("Authorization",bearer(u)).contentType(MediaType.APPLICATION_JSON).content("{\"status\":\""+valor+"\"}"));}
 private org.springframework.test.web.servlet.ResultActions pagar(UUID id,User u,String chave,String cenario)throws Exception{return mvc.perform(post("/api/v1/pedidos/{id}/pagamentos",id).header("Authorization",bearer(u)).header("Idempotency-Key",chave).contentType(MediaType.APPLICATION_JSON).content("{\"cenario\":\""+cenario+"\"}"));}

 private Fixture fixture(int qtd){var admin=user(Perfil.ADMIN);var cliente=user(Perfil.CLIENTE);var u=unidades.criar("UND-"+UUID.randomUUID(),"Teste");var p=catalogo.criarProduto("SKU-"+UUID.randomUUID(),"Produto",null);catalogo.configurarOferta(u.id(),p.id(),new BigDecimal("12.50"),true);if(qtd>0)estoque.movimentar(u.id(),admin.id,true,false,"CARGA-"+UUID.randomUUID(),p.id(),TipoMovimentacaoEstoque.ENTRADA,qtd,"Teste");return new Fixture(u.id(),p.id(),admin,cliente);}
 private UUID produto(Fixture f,int qtd,BigDecimal preco){var p=catalogo.criarProduto("SKU-"+UUID.randomUUID(),"Outro",null);catalogo.configurarOferta(f.unidadeId,p.id(),preco,true);if(qtd>0)estoque.movimentar(f.unidadeId,f.admin.id,true,false,"CARGA-"+UUID.randomUUID(),p.id(),TipoMovimentacaoEstoque.ENTRADA,qtd,"Teste");return p.id();}
 private EstoqueEntity saldo(Fixture f){return saldos.findByUnidadeId(f.unidadeId).stream().filter(e->catalogo.buscarProdutoPorOferta(e.getProdutoUnidadeId(),f.unidadeId).equals(f.produtoId)).findFirst().orElseThrow();}
 private CriarPedido cmd(Fixture f,int qtd){return new CriarPedido(f.unidadeId,null,CanalPedido.APP,FormaPagamento.MOCK,List.of(new ItemSolicitado(f.produtoId,qtd)));}
 private String body(Fixture f,String canal,int qtd){return body(f,f.produtoId,canal,qtd);} private String body(Fixture f,UUID p,String canal,int qtd){return "{\"unidadeId\":\""+f.unidadeId+"\",\"canalPedido\":\""+canal+"\",\"formaPagamento\":\"MOCK\",\"itens\":[{\"produtoId\":\""+p+"\",\"quantidade\":"+qtd+"}]}";}
 private org.springframework.test.web.servlet.ResultActions criar(Fixture f,User u,String k,String b)throws Exception{return mvc.perform(post("/api/v1/pedidos").header("Authorization",bearer(u)).header("Idempotency-Key",k).contentType(MediaType.APPLICATION_JSON).content(b));}
 private org.springframework.test.web.servlet.ResultActions cancelar(UUID id,User u)throws Exception{return mvc.perform(post("/api/v1/pedidos/{id}/cancelamento",id).header("Authorization",bearer(u)));}
 private UUID id(org.springframework.test.web.servlet.ResultActions r)throws Exception{return UUID.fromString(new com.fasterxml.jackson.databind.ObjectMapper().readTree(r.andReturn().getResponse().getContentAsString()).get("id").asText());}
 private UUID pagamentoId(org.springframework.test.web.servlet.ResultActions r)throws Exception{return UUID.fromString(new com.fasterxml.jackson.databind.ObjectMapper().readTree(r.andReturn().getResponse().getContentAsString()).get("pagamentoId").asText());}
 private User user(Perfil p){var email=p+"-"+UUID.randomUUID()+"@example.invalid";var u=usuarios.criarInterno(new CriarUsuarioCommand("Teste",email,"senha-segura-123",p));return new User(u.id(),auth.login(email,"senha-segura-123").accessToken().value());}
 private String bearer(User u){return "Bearer "+u.token;} private record User(UUID id,String token){} private record Fixture(UUID unidadeId,UUID produtoId,User admin,User cliente){}

 @TestConfiguration static class ClockConfig{@Bean @Primary MutableClock mutableClock(){return new MutableClock(Instant.parse("2026-01-01T00:00:00Z"));}}
 static class MutableClock extends Clock{private Instant value;MutableClock(Instant v){value=v;}void advance(Duration d){value=value.plus(d);}public ZoneId getZone(){return ZoneOffset.UTC;}public Clock withZone(ZoneId z){return this;}public Instant instant(){return value;}}
}
