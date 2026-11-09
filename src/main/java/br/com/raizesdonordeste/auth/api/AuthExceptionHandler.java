package br.com.raizesdonordeste.auth.api;

import java.time.Instant;
import java.util.List;

import br.com.raizesdonordeste.auth.application.CredenciaisInvalidasException;
import br.com.raizesdonordeste.shared.api.ApiError;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(assignableTypes = AuthController.class)
public class AuthExceptionHandler {

    @ExceptionHandler(CredenciaisInvalidasException.class)
    ResponseEntity<ApiError> invalidCredentials(CredenciaisInvalidasException exception,
            HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new ApiError(
                "invalid_credentials", "Credenciais inválidas", List.of(), Instant.now(),
                request.getRequestURI(), request.getHeader("X-Request-Id")));
    }
}
