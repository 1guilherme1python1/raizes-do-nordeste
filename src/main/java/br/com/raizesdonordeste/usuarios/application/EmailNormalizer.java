package br.com.raizesdonordeste.usuarios.application;

import java.util.Locale;

import org.springframework.stereotype.Component;

@Component
public class EmailNormalizer {

    public String normalize(String email) {
        if (email == null) {
            throw new IllegalArgumentException("E-mail é obrigatório");
        }
        return email.strip().toLowerCase(Locale.ROOT);
    }
}
