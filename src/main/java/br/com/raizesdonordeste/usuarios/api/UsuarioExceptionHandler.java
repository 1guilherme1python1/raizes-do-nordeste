package br.com.raizesdonordeste.usuarios.api;

import java.time.Instant;
import java.util.List;

import br.com.raizesdonordeste.shared.api.ApiError;
import br.com.raizesdonordeste.usuarios.application.EmailJaCadastradoException;
import br.com.raizesdonordeste.usuarios.application.UsuarioNaoEncontradoException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(assignableTypes = UsuarioController.class)
public class UsuarioExceptionHandler {

    @ExceptionHandler(EmailJaCadastradoException.class)
    ResponseEntity<ApiError> duplicateEmail(EmailJaCadastradoException exception, HttpServletRequest request) {
        return response(HttpStatus.CONFLICT, "email_already_registered", "E-mail já cadastrado", request);
    }

    @ExceptionHandler(UsuarioNaoEncontradoException.class)
    ResponseEntity<ApiError> userNotFound(UsuarioNaoEncontradoException exception, HttpServletRequest request) {
        return response(HttpStatus.UNAUTHORIZED, "invalid_user",
                "Usuário autenticado não está disponível", request);
    }

    private ResponseEntity<ApiError> response(HttpStatus status, String error, String message,
            HttpServletRequest request) {
        return ResponseEntity.status(status).body(new ApiError(error, message, List.of(), Instant.now(),
                request.getRequestURI(), request.getHeader("X-Request-Id")));
    }
}
