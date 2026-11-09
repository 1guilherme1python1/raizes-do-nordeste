package br.com.raizesdonordeste.shared.api;

import java.time.Instant;
import java.util.List;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import br.com.raizesdonordeste.shared.application.AcessoNegadoException;
import br.com.raizesdonordeste.shared.application.ConflitoException;
import br.com.raizesdonordeste.shared.application.RecursoNaoEncontradoException;
import org.springframework.web.bind.MissingRequestHeaderException;
import jakarta.validation.ConstraintViolationException;

@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ApiError> validation(MethodArgumentNotValidException exception, HttpServletRequest request) {
        var details = exception.getBindingResult().getFieldErrors().stream()
                .map(this::formatFieldError)
                .sorted()
                .toList();
        return response(HttpStatus.UNPROCESSABLE_ENTITY, "validation_error",
                "A requisição contém campos inválidos", details, request);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    ResponseEntity<ApiError> unreadable(HttpMessageNotReadableException exception, HttpServletRequest request) {
        return response(HttpStatus.BAD_REQUEST, "invalid_request", "O corpo da requisição é inválido",
                List.of(), request);
    }

    @ExceptionHandler(MissingRequestHeaderException.class)
    ResponseEntity<ApiError> missingHeader(MissingRequestHeaderException exception, HttpServletRequest request) {
        return response(HttpStatus.BAD_REQUEST, "invalid_request", "Cabeçalho obrigatório ausente",
                List.of(exception.getHeaderName()), request);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    ResponseEntity<ApiError> constraint(ConstraintViolationException exception, HttpServletRequest request) {
        var details = exception.getConstraintViolations().stream()
                .map(v -> v.getPropertyPath() + ": " + v.getMessage()).sorted().toList();
        return response(HttpStatus.UNPROCESSABLE_ENTITY, "validation_error",
                "A requisição contém campos inválidos", details, request);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    ResponseEntity<ApiError> illegalArgument(IllegalArgumentException exception, HttpServletRequest request) {
        return response(HttpStatus.UNPROCESSABLE_ENTITY, "validation_error", exception.getMessage(),
                List.of(), request);
    }

    @ExceptionHandler(RecursoNaoEncontradoException.class)
    ResponseEntity<ApiError> notFound(RecursoNaoEncontradoException exception, HttpServletRequest request) {
        return response(HttpStatus.NOT_FOUND, "not_found", exception.getMessage(), List.of(), request);
    }

    @ExceptionHandler(AcessoNegadoException.class)
    ResponseEntity<ApiError> forbidden(AcessoNegadoException exception, HttpServletRequest request) {
        return response(HttpStatus.FORBIDDEN, "forbidden", exception.getMessage(), List.of(), request);
    }

    @ExceptionHandler({ConflitoException.class, ObjectOptimisticLockingFailureException.class,
            DataIntegrityViolationException.class})
    ResponseEntity<ApiError> conflict(Exception exception, HttpServletRequest request) {
        var message = exception instanceof ConflitoException ? exception.getMessage()
                : "A operação conflitou com o estado atual do recurso";
        return response(HttpStatus.CONFLICT, "conflict", message, List.of(), request);
    }

    private String formatFieldError(FieldError error) {
        return error.getField() + ": " + error.getDefaultMessage();
    }

    private static ResponseEntity<ApiError> response(HttpStatus status, String error, String message,
            List<String> details, HttpServletRequest request) {
        var requestId = request.getHeader("X-Request-Id");
        return ResponseEntity.status(status).body(new ApiError(error, message, details, Instant.now(),
                request.getRequestURI(), requestId));
    }
}
