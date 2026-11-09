package br.com.raizesdonordeste.usuarios.application;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class EmailNormalizerTest {

    private final EmailNormalizer normalizer = new EmailNormalizer();

    @Test
    void removeEspacosExternosEConverteParaMinusculas() {
        assertThat(normalizer.normalize("  Maria.SILVA@Example.COM  "))
                .isEqualTo("maria.silva@example.com");
    }
}
