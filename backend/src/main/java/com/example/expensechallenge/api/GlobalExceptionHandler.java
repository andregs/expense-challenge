package com.example.expensechallenge.api;

import com.example.expensechallenge.service.exception.UnconvertibleException;
import jakarta.validation.ConstraintViolationException;
import java.util.Map;
import java.util.NoSuchElementException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
        MethodArgumentNotValidException ex,
        HttpHeaders headers,
        HttpStatusCode status,
        WebRequest request
    ) {
        ProblemDetail body = ex.getBody();
        body.setProperty("errors", ex.getBindingResult().getFieldErrors().stream()
            .map(fe -> Map.of("field", fe.getField(), "message",
                fe.getDefaultMessage() != null ? fe.getDefaultMessage() : "invalid"))
            .toList());
        return handleExceptionInternal(ex, body, headers, status, request);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    ResponseEntity<ProblemDetail> handleConstraintViolation(ConstraintViolationException ex) {
        var body = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Validation failed");
        body.setProperty("errors", ex.getConstraintViolations().stream()
            .map(cv -> {
                String path = cv.getPropertyPath().toString();
                String field = path.contains(".") ? path.substring(path.lastIndexOf('.') + 1) : path;
                return Map.of("field", field, "message", cv.getMessage());
            })
            .toList());
        return ResponseEntity.badRequest().body(body);
    }

    @ExceptionHandler(NoSuchElementException.class)
    ResponseEntity<ProblemDetail> handleNotFound(NoSuchElementException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
            .body(ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage()));
    }

    @ExceptionHandler(UnconvertibleException.class)
    ResponseEntity<ProblemDetail> handleUnconvertible(UnconvertibleException ex) {
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_CONTENT)
            .body(ProblemDetail.forStatusAndDetail(HttpStatus.UNPROCESSABLE_CONTENT, ex.getMessage()));
    }
}
