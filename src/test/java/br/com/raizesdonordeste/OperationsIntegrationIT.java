package br.com.raizesdonordeste;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.math.BigDecimal;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import br.com.raizesdonordeste.auth.application.AuthService;
import br.com.raizesdonordeste.catalogo.application.CatalogoService;
import br.com.raizesdonordeste.estoque.application.EstoqueService;
import br.com.raizesdonordeste.estoque.domain.TipoMovimentacaoEstoque;
import br.com.raizesdonordeste.estoque.infrastructure.persistence.EstoqueJpaRepository;
import br.com.raizesdonordeste.estoque.infrastructure.persistence.MovimentacaoEstoqueJpaRepository;
import br.com.raizesdonordeste.shared.application.ConflitoException;
import br.com.raizesdonordeste.unidades.application.AcessoUnidadeService;
import br.com.raizesdonordeste.unidades.application.UnidadeService;
import br.com.raizesdonordeste.usuarios.application.*;
import br.com.raizesdonordeste.usuarios.domain.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.MediaType;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.*;

@ActiveProfiles("test")
@SpringBootTest(properties={"app.security.jwt.secret=MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=","app.security.jwt.issuer=integration-test-issuer","app.security.jwt.audience=integration-test-audience"})
@AutoConfigureMockMvc @Testcontainers
class OperationsIntegrationIT {
 @Container @ServiceConnection static final PostgreSQLContainer<?> POSTGRESQL=new PostgreSQLContainer<>("postgres:16.10-alpine3.22");
 @Autowired MockMvc mvc; @Autowired UnidadeService unidades; @Autowired CatalogoService catalogo;
 @Autowired EstoqueService estoque; @Autowired AcessoUnidadeService acesso; @Autowired UsuarioService usuarios;
 @Autowired AuthService auth; @Autowired EstoqueJpaRepository estoques; @Autowired MovimentacaoEstoqueJpaRepository movimentos;

 @Test void t01AdminCadastraUnidade() throws Exception {var admin=user(Perfil.ADMIN);mvc.perform(post("/api/v1/unidades").header("Authorization",bearer(admin)).contentType(MediaType.APPLICATION_JSON).content("{\"codigo\":\"REC-"+UUID.randomUUID()+"\",\"nome\":\"Recife\"}" )).andExpect(status().isCreated()).andExpect(jsonPath("$.ativa").value(true));}
 @Test void t02AdminCadastraProduto() throws Exception {var admin=user(Perfil.ADMIN);mvc.perform(post("/api/v1/produtos").header("Authorization",bearer(admin)).contentType(MediaType.APPLICATION_JSON).content("{\"sku\":\"SKU-"+UUID.randomUUID()+"\",\"nome\":\"Cuscuz\",\"descricao\":\"Regional\"}" )).andExpect(status().isCreated()).andExpect(jsonPath("$.nome").value("Cuscuz"));}
 @Test void t03ProdutoAssociadoComPrecoEspecifico() throws Exception {var f=fixture();mvc.perform(put("/api/v1/unidades/{u}/produtos/{p}",f.unidadeId,f.produtoId).header("Authorization",bearer(f.admin)).contentType(MediaType.APPLICATION_JSON).content("{\"preco\":12.50,\"disponivel\":true}" )).andExpect(status().isOk()).andExpect(jsonPath("$.preco").value(12.5));}
 @Test void t04CardapioRetornaSomenteDisponiveis() throws Exception {var f=fixtureComOferta();estoque.movimentar(f.unidadeId,f.admin.id(),true,false,"entrada-cardapio",f.produtoId,TipoMovimentacaoEstoque.ENTRADA,3,"Carga");var indisponivel=catalogo.criarProduto("SKU-"+UUID.randomUUID(),"Oculto",null);catalogo.configurarOferta(f.unidadeId,indisponivel.id(),new BigDecimal("5.00"),false);mvc.perform(get("/api/v1/unidades/{u}/cardapio",f.unidadeId)).andExpect(status().isOk()).andExpect(jsonPath("$.content.length()").value(1)).andExpect(jsonPath("$.content[0].produtoId").value(f.produtoId.toString()));}
 @Test void t05EntradaAumentaSaldo() throws Exception {var f=fixtureComOferta();movimentar(f,f.admin,"k-entrada",TipoMovimentacaoEstoque.ENTRADA,10).andExpect(status().isCreated()).andExpect(jsonPath("$.quantidadeFisicaResultante").value(10));}
 @Test void t06SaidaValidaDiminuiSaldo() throws Exception {var f=fixtureComOferta();estoque.movimentar(f.unidadeId,f.admin.id(),true,false,"pre-entrada",f.produtoId,TipoMovimentacaoEstoque.ENTRADA,10,"Carga");movimentar(f,f.admin,"k-saida",TipoMovimentacaoEstoque.SAIDA,4).andExpect(status().isCreated()).andExpect(jsonPath("$.quantidadeFisicaResultante").value(6));}
 @Test void t07SaidaSuperiorRetornaConflito() throws Exception {var f=fixtureComOferta();movimentar(f,f.admin,"sem-saldo",TipoMovimentacaoEstoque.SAIDA,1).andExpect(status().isConflict()).andExpect(jsonPath("$.error").value("conflict"));}
 @Test void t08GerenteVinculadoMovimentaSuaUnidade() throws Exception {var f=fixtureComOferta();var gerente=user(Perfil.GERENTE);acesso.associar(f.unidadeId,gerente.id());movimentar(f,gerente,"gerente-ok",TipoMovimentacaoEstoque.ENTRADA,2).andExpect(status().isCreated());}
 @Test void t09GerenteDeOutraUnidadeRecebe403() throws Exception {var f=fixtureComOferta();var gerente=user(Perfil.GERENTE);var outra=unidades.criar("UND-"+UUID.randomUUID(),"Outra");acesso.associar(outra.id(),gerente.id());movimentar(f,gerente,"gerente-fora",TipoMovimentacaoEstoque.ENTRADA,2).andExpect(status().isForbidden());}
 @Test void t10ClienteNaoModificaEstoque() throws Exception {var f=fixtureComOferta();var cliente=user(Perfil.CLIENTE);movimentar(f,cliente,"cliente-nao",TipoMovimentacaoEstoque.ENTRADA,2).andExpect(status().isForbidden());}
 @Test void t11ConsultaSemJwtRetorna401() throws Exception {var f=fixtureComOferta();mvc.perform(get("/api/v1/unidades/{u}/estoque",f.unidadeId)).andExpect(status().isUnauthorized());}
 @Test void t12MesmaChaveNaoDuplicaSaldo() throws Exception {var f=fixtureComOferta();movimentar(f,f.admin,"mesma-chave",TipoMovimentacaoEstoque.ENTRADA,5).andExpect(status().isCreated());movimentar(f,f.admin,"mesma-chave",TipoMovimentacaoEstoque.ENTRADA,5).andExpect(status().isOk()).andExpect(jsonPath("$.repetida").value(true));assertThat(estoques.findByUnidadeId(f.unidadeId).getFirst().getQuantidadeFisica()).isEqualTo(5);assertThat(movimentos.findByUnidadeIdAndIdempotencyKey(f.unidadeId,"mesma-chave")).isPresent();}
 @Test void t13MesmaChaveConteudoDiferenteRetorna409() throws Exception {var f=fixtureComOferta();movimentar(f,f.admin,"chave-conflito",TipoMovimentacaoEstoque.ENTRADA,5).andExpect(status().isCreated());movimentar(f,f.admin,"chave-conflito",TipoMovimentacaoEstoque.ENTRADA,6).andExpect(status().isConflict());}
 @Test void t14DuasSaidasConcorrentesNaoNegativamSaldo() throws Exception {var f=fixtureComOferta();estoque.movimentar(f.unidadeId,f.admin.id(),true,false,"carga-concorrente",f.produtoId,TipoMovimentacaoEstoque.ENTRADA,10,"Carga");var inicio=new CountDownLatch(1);var ok=new AtomicInteger();var conflitos=new AtomicInteger();try(var executor=Executors.newFixedThreadPool(2)){for(int i=0;i<2;i++){var key="saida-concorrente-"+i;executor.submit(()->{try{inicio.await();estoque.movimentar(f.unidadeId,f.admin.id(),true,false,key,f.produtoId,TipoMovimentacaoEstoque.SAIDA,7,"Concorrência");ok.incrementAndGet();}catch(ConflitoException|ObjectOptimisticLockingFailureException e){conflitos.incrementAndGet();}catch(Exception e){throw new RuntimeException(e);}});}inicio.countDown();executor.shutdown();assertThat(executor.awaitTermination(20,TimeUnit.SECONDS)).isTrue();}assertThat(ok).hasValue(1);assertThat(conflitos).hasValue(1);assertThat(estoques.findByUnidadeId(f.unidadeId).getFirst().getQuantidadeFisica()).isEqualTo(3);}
 @Test void t15ProdutoInexistenteRetorna404() throws Exception {var f=fixture();mvc.perform(put("/api/v1/unidades/{u}/produtos/{p}",f.unidadeId,UUID.randomUUID()).header("Authorization",bearer(f.admin)).contentType(MediaType.APPLICATION_JSON).content("{\"preco\":10.00,\"disponivel\":true}" )).andExpect(status().isNotFound());}
 @Test void t16PrecoOuQuantidadeInvalidosRetornam422() throws Exception {var f=fixture();mvc.perform(put("/api/v1/unidades/{u}/produtos/{p}",f.unidadeId,f.produtoId).header("Authorization",bearer(f.admin)).contentType(MediaType.APPLICATION_JSON).content("{\"preco\":0,\"disponivel\":true}" )).andExpect(status().isUnprocessableEntity());var o=catalogo.configurarOferta(f.unidadeId,f.produtoId,new BigDecimal("10"),true);mvc.perform(post("/api/v1/unidades/{u}/estoque/movimentacoes",f.unidadeId).header("Authorization",bearer(f.admin)).header("Idempotency-Key","q-invalida").contentType(MediaType.APPLICATION_JSON).content("{\"produtoId\":\""+f.produtoId+"\",\"tipo\":\"ENTRADA\",\"quantidade\":0,\"motivo\":\"Teste\"}" )).andExpect(status().isUnprocessableEntity());}

 private org.springframework.test.web.servlet.ResultActions movimentar(Fixture f,User user,String key,TipoMovimentacaoEstoque tipo,int qtd)throws Exception{return mvc.perform(post("/api/v1/unidades/{u}/estoque/movimentacoes",f.unidadeId).header("Authorization",bearer(user)).header("Idempotency-Key",key).contentType(MediaType.APPLICATION_JSON).content("{\"produtoId\":\""+f.produtoId+"\",\"tipo\":\""+tipo+"\",\"quantidade\":"+qtd+",\"motivo\":\"Teste operacional\"}"));}
 private Fixture fixture(){var admin=user(Perfil.ADMIN);var u=unidades.criar("UND-"+UUID.randomUUID(),"Unidade Teste");var p=catalogo.criarProduto("SKU-"+UUID.randomUUID(),"Produto Teste","Sintético");return new Fixture(u.id(),p.id(),admin);}
 private Fixture fixtureComOferta(){var f=fixture();catalogo.configurarOferta(f.unidadeId,f.produtoId,new BigDecimal("12.50"),true);return f;}
 private User user(Perfil perfil){var email=perfil.name().toLowerCase()+"-"+UUID.randomUUID()+"@example.invalid";var u=usuarios.criarInterno(new CriarUsuarioCommand("Teste "+perfil,email,"senha-segura-123",perfil));return new User(u.id(),auth.login(email,"senha-segura-123").accessToken().value());}
 private String bearer(User u){return "Bearer "+u.token;}
 private record User(UUID id,String token){} private record Fixture(UUID unidadeId,UUID produtoId,User admin){}
}
