package br.com.raizesdonordeste.estoque.domain;

import static org.assertj.core.api.Assertions.*;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class EstoqueTest {
    private Estoque estoque(int fisica,int reservada){return new Estoque(UUID.randomUUID(),UUID.randomUUID(),UUID.randomUUID(),fisica,reservada,0,null,null);}
    @Test void entradaAumentaQuantidadeFisica(){assertThat(estoque(10,2).aplicar(TipoMovimentacaoEstoque.ENTRADA,5).quantidadeFisica()).isEqualTo(15);}
    @Test void saidaUsaSomenteSaldoNaoReservado(){assertThat(estoque(10,4).aplicar(TipoMovimentacaoEstoque.SAIDA,6).quantidadeFisica()).isEqualTo(4);}
    @Test void rejeitaSaidaQueConsumiriaReserva(){assertThatThrownBy(()->estoque(10,4).aplicar(TipoMovimentacaoEstoque.SAIDA,7)).isInstanceOf(IllegalArgumentException.class).hasMessageContaining("insuficiente");}
    @Test void rejeitaQuantidadeNaoPositiva(){assertThatThrownBy(()->estoque(10,0).aplicar(TipoMovimentacaoEstoque.ENTRADA,0)).isInstanceOf(IllegalArgumentException.class);}
}
