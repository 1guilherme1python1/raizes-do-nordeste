package br.com.raizesdonordeste.shared.application;

public class AcessoNegadoException extends RuntimeException {
    public AcessoNegadoException() { super("Acesso negado"); }
}
