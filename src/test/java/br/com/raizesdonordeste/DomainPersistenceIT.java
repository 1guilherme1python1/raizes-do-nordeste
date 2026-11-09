package br.com.raizesdonordeste;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import javax.sql.DataSource;

import br.com.raizesdonordeste.catalogo.domain.Produto;
import br.com.raizesdonordeste.catalogo.domain.ProdutoUnidade;
import br.com.raizesdonordeste.catalogo.infrastructure.persistence.ProdutoEntity;
import br.com.raizesdonordeste.catalogo.infrastructure.persistence.ProdutoJpaRepository;
import br.com.raizesdonordeste.catalogo.infrastructure.persistence.ProdutoUnidadeEntity;
import br.com.raizesdonordeste.catalogo.infrastructure.persistence.ProdutoUnidadeJpaRepository;
import br.com.raizesdonordeste.estoque.domain.Estoque;
import br.com.raizesdonordeste.estoque.domain.MovimentacaoEstoque;
import br.com.raizesdonordeste.estoque.domain.TipoMovimentacaoEstoque;
import br.com.raizesdonordeste.estoque.infrastructure.persistence.EstoqueEntity;
import br.com.raizesdonordeste.estoque.infrastructure.persistence.EstoqueJpaRepository;
import br.com.raizesdonordeste.estoque.infrastructure.persistence.MovimentacaoEstoqueEntity;
import br.com.raizesdonordeste.estoque.infrastructure.persistence.MovimentacaoEstoqueJpaRepository;
import br.com.raizesdonordeste.pagamentos.domain.Pagamento;
import br.com.raizesdonordeste.pagamentos.domain.StatusPagamento;
import br.com.raizesdonordeste.pagamentos.infrastructure.persistence.PagamentoEntity;
import br.com.raizesdonordeste.pagamentos.infrastructure.persistence.PagamentoJpaRepository;
import br.com.raizesdonordeste.pedidos.domain.CanalPedido;
import br.com.raizesdonordeste.pedidos.domain.ItemPedido;
import br.com.raizesdonordeste.pedidos.domain.Pedido;
import br.com.raizesdonordeste.pedidos.domain.StatusPedido;
import br.com.raizesdonordeste.pedidos.infrastructure.persistence.PedidoEntity;
import br.com.raizesdonordeste.pedidos.infrastructure.persistence.PedidoJpaRepository;
import br.com.raizesdonordeste.unidades.domain.Unidade;
import br.com.raizesdonordeste.unidades.infrastructure.persistence.UnidadeEntity;
import br.com.raizesdonordeste.unidades.infrastructure.persistence.UnidadeJpaRepository;
import br.com.raizesdonordeste.usuarios.domain.Usuario;
import br.com.raizesdonordeste.usuarios.infrastructure.persistence.UsuarioEntity;
import br.com.raizesdonordeste.usuarios.infrastructure.persistence.UsuarioJpaRepository;
import jakarta.persistence.EntityManager;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@ActiveProfiles("test")
@SpringBootTest
@Testcontainers
@Transactional
class DomainPersistenceIT {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRESQL =
            new PostgreSQLContainer<>("postgres:16.10-alpine3.22");

    @Autowired
    private DataSource dataSource;

    @Autowired
    private Flyway flyway;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private UnidadeJpaRepository unidadeRepository;

    @Autowired
    private UsuarioJpaRepository usuarioRepository;

    @Autowired
    private ProdutoJpaRepository produtoRepository;

    @Autowired
    private ProdutoUnidadeJpaRepository produtoUnidadeRepository;

    @Autowired
    private EstoqueJpaRepository estoqueRepository;

    @Autowired
    private MovimentacaoEstoqueJpaRepository movimentacaoRepository;

    @Autowired
    private PedidoJpaRepository pedidoRepository;

    @Autowired
    private PagamentoJpaRepository pagamentoRepository;

    @Test
    void iniciaJpaComPostgreSqlEAmbasAsMigrations() throws Exception {
        try (var connection = dataSource.getConnection()) {
            assertThat(connection.getMetaData().getDatabaseProductName()).isEqualTo("PostgreSQL");
        }

        assertThat(flyway.info().current().getVersion().getVersion()).isEqualTo("5");
        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM app.flyway_schema_history WHERE success AND version IS NOT NULL",
                Integer.class)).isEqualTo(5);
        assertThat(jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM information_schema.tables
                WHERE table_schema = 'app'
                  AND table_name IN ('usuario', 'unidade', 'produto', 'produto_unidade', 'estoque',
                                     'movimentacao_estoque', 'pedido', 'item_pedido', 'pagamento', 'usuario_unidade')
                """, Integer.class)).isEqualTo(10);
    }

    @Test
    void persisteUnidadeProdutoEOfertaEspecificaDaUnidade() {
        var fixture = criarCatalogoBase();
        entityManager.clear();

        var unidade = unidadeRepository.findByCodigo(fixture.unidade().codigo()).orElseThrow().toDomain();
        var produto = produtoRepository.findBySku(fixture.produto().sku()).orElseThrow().toDomain();
        var oferta = produtoUnidadeRepository
                .findByProdutoIdAndUnidadeId(produto.id(), unidade.id()).orElseThrow().toDomain();

        assertThat(unidade.nome()).isEqualTo("Unidade Acadêmica");
        assertThat(produto.nome()).isEqualTo("Produto Acadêmico");
        assertThat(oferta.preco()).isEqualByComparingTo("12.50");
        assertThat(oferta.disponivel()).isTrue();
    }

    @Test
    void persisteEstoquePorUnidadeEHistoricoDeMovimentacao() {
        var fixture = criarCatalogoBase();
        var estoque = new Estoque(UUID.randomUUID(), fixture.oferta().id(), fixture.unidade().id(),
                20, 4, 0, null, null);
        estoqueRepository.save(EstoqueEntity.fromDomain(estoque));

        var movimento = new MovimentacaoEstoque(UUID.randomUUID(), estoque.id(), fixture.unidade().id(), null,
                TipoMovimentacaoEstoque.ENTRADA, 20, 20, 4, "Carga inicial de teste", null, null, Instant.now());
        movimentacaoRepository.save(MovimentacaoEstoqueEntity.fromDomain(movimento));
        entityManager.flush();
        entityManager.clear();

        var persistido = estoqueRepository.findByProdutoUnidadeId(fixture.oferta().id()).orElseThrow().toDomain();
        var movimentos = movimentacaoRepository.findByEstoqueIdOrderByCriadoEmAsc(estoque.id());

        assertThat(persistido.quantidadeDisponivel()).isEqualTo(16);
        assertThat(movimentos).singleElement().satisfies(item ->
                assertThat(item.toDomain().tipo()).isEqualTo(TipoMovimentacaoEstoque.ENTRADA));
    }

    @Test
    void persistePedidoComItensEVariasTentativasDePagamento() {
        var fixture = criarCatalogoBase();
        var usuario = new Usuario(UUID.randomUUID(), codigo("USR"), "Cliente de Teste", true, null, null);
        usuarioRepository.save(UsuarioEntity.fromDomain(usuario));

        var item = new ItemPedido(UUID.randomUUID(), fixture.produto().id(), fixture.oferta().id(), 2,
                new BigDecimal("12.50"), new BigDecimal("25.00"), null, null);
        var pedido = new Pedido(UUID.randomUUID(), fixture.unidade().id(), usuario.id(), CanalPedido.APP,
                StatusPedido.AGUARDANDO_PAGAMENTO, new BigDecimal("25.00"), "BRL", List.of(item), null, null);
        pedidoRepository.save(PedidoEntity.fromDomain(pedido));

        var solicitadoEm = Instant.now();
        var tentativa1 = new Pagamento(UUID.randomUUID(), pedido.id(), 1, StatusPagamento.RECUSADO,
                codigo("TX"), solicitadoEm, solicitadoEm.plusSeconds(1), "SIMULACAO_RECUSA", null, null);
        var tentativa2 = new Pagamento(UUID.randomUUID(), pedido.id(), 2, StatusPagamento.PENDENTE,
                codigo("TX"), solicitadoEm.plusSeconds(2), null, null, null, null);
        pagamentoRepository.saveAll(List.of(
                PagamentoEntity.fromDomain(tentativa1), PagamentoEntity.fromDomain(tentativa2)));
        entityManager.flush();
        entityManager.clear();

        var persistido = pedidoRepository.findWithItensById(pedido.id()).orElseThrow().toDomain();
        var pagamentos = pagamentoRepository.findByPedidoIdOrderByNumeroTentativaAsc(pedido.id());

        assertThat(persistido.canalPedido()).isEqualTo(CanalPedido.APP);
        assertThat(persistido.itens()).singleElement().satisfies(itemPersistido -> {
            assertThat(itemPersistido.precoUnitario()).isEqualByComparingTo("12.50");
            assertThat(itemPersistido.subtotal()).isEqualByComparingTo("25.00");
        });
        assertThat(pedidoRepository.findByCanalPedido(CanalPedido.APP)).hasSize(1);
        assertThat(pagamentos).extracting(itemPagamento -> itemPagamento.toDomain().status())
                .containsExactly(StatusPagamento.RECUSADO, StatusPagamento.PENDENTE);
    }

    @Test
    void rejeitaCodigoDuplicadoDeUnidade() {
        var codigo = codigo("UND");
        unidadeRepository.save(UnidadeEntity.fromDomain(
                new Unidade(UUID.randomUUID(), codigo, "Primeira", true, null, null)));
        unidadeRepository.save(UnidadeEntity.fromDomain(
                new Unidade(UUID.randomUUID(), codigo, "Segunda", true, null, null)));

        assertThatThrownBy(unidadeRepository::flush)
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasStackTraceContaining("uk_unidade_codigo");
    }

    @Test
    void rejeitaReservaSuperiorAoSaldoFisico() {
        var fixture = criarCatalogoBase();

        assertThatThrownBy(() -> jdbcTemplate.update("""
                INSERT INTO app.estoque
                    (id, produto_unidade_id, unidade_id, quantidade_fisica, quantidade_reservada, versao)
                VALUES (?, ?, ?, 2, 3, 0)
                """, UUID.randomUUID(), fixture.oferta().id(), fixture.unidade().id()))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasStackTraceContaining("ck_estoque_reserva_saldo");
    }

    @Test
    void rejeitaPagamentoParaPedidoInexistente() {
        assertThatThrownBy(() -> jdbcTemplate.update("""
                INSERT INTO app.pagamento
                    (id, pedido_id, numero_tentativa, status, transacao_id, solicitado_em)
                VALUES (?, ?, 1, 'PENDENTE', ?, CURRENT_TIMESTAMP)
                """, UUID.randomUUID(), UUID.randomUUID(), codigo("TX")))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasStackTraceContaining("fk_pagamento_pedido");
    }

    private CatalogoFixture criarCatalogoBase() {
        var unidade = new Unidade(UUID.randomUUID(), codigo("UND"), "Unidade Acadêmica", true, null, null);
        var produto = new Produto(UUID.randomUUID(), codigo("SKU"), "Produto Acadêmico",
                "Registro sintético para teste", true, null, null);
        var oferta = new ProdutoUnidade(UUID.randomUUID(), produto.id(), unidade.id(),
                new BigDecimal("12.50"), true, null, null);

        unidadeRepository.save(UnidadeEntity.fromDomain(unidade));
        produtoRepository.save(ProdutoEntity.fromDomain(produto));
        produtoUnidadeRepository.save(ProdutoUnidadeEntity.fromDomain(oferta));
        entityManager.flush();
        return new CatalogoFixture(unidade, produto, oferta);
    }

    private String codigo(String prefixo) {
        return prefixo + "-" + UUID.randomUUID();
    }

    private record CatalogoFixture(Unidade unidade, Produto produto, ProdutoUnidade oferta) {
    }
}
