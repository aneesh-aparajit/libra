package dev.tsukuyomi.libra.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/**
 * Maps failures to RFC 9457 problem details. Standard Spring MVC errors (validation, malformed body,
 * type mismatch, ...) are handled by {@link ResponseEntityExceptionHandler}; everything else, including
 * exceptions surfacing through {@code CompletableFuture} wrappers, is resolved by walking the cause chain.
 */
@RestControllerAdvice
public class ApiExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(ApiExceptionHandler.class);

    private static final int MAX_CAUSE_DEPTH = 16;

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ProblemDetail> handle(Exception ex) {
        Throwable cause = ex;
        for (int depth = 0; cause != null && depth < MAX_CAUSE_DEPTH; depth++, cause = cause.getCause()) {
            if (cause instanceof ApiException apiEx) {
                return problem(apiEx.getStatus(), apiEx.getMessage());
            }
            if (cause instanceof DuplicateKeyException) {
                return problem(HttpStatus.CONFLICT, "resource already exists");
            }
            if (cause instanceof DataIntegrityViolationException) {
                return problem(HttpStatus.BAD_REQUEST, "request violates a data constraint (check referenced ids)");
            }
        }
        logger.error("unhandled error serving request", ex);
        return problem(HttpStatus.INTERNAL_SERVER_ERROR, "internal server error");
    }

    private static ResponseEntity<ProblemDetail> problem(HttpStatus status, String detail) {
        return ResponseEntity.status(status).body(ProblemDetail.forStatusAndDetail(status, detail));
    }

}
