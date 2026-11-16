package org.capstone.controller.validation;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.capstone.dto.response.ApiErrorResponse;
import org.jspecify.annotations.NonNull;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler  extends ResponseEntityExceptionHandler {

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex,
            @NonNull HttpHeaders headers,
            @NonNull HttpStatusCode status,
            @NonNull WebRequest request) {

        Map<String, String> errors = new HashMap<>();
        for (FieldError fieldError : ex.getBindingResult().getFieldErrors()) {
            errors.put(fieldError.getField(), fieldError.getDefaultMessage());
        }

        log.warn(ex.getClass().toString());

        String path = ((ServletWebRequest) request).getRequest().getRequestURI();

        ApiErrorResponse errorResponse = new ApiErrorResponse(
                status.value(),
                HttpStatus.valueOf(status.value()).getReasonPhrase(),
                "Validation failed for one or more fields",
                path,
                errors
        );

        return new ResponseEntity<>(errorResponse, headers, status);
    }


    @ExceptionHandler(ConstraintViolationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    ResponseEntity<Object> handleConstraintViolationException(
            ConstraintViolationException ex,
            @NonNull HttpHeaders headers,
            @NonNull HttpStatusCode status,
            @NonNull WebRequest request) {

        Map<String, String> errors = new HashMap<>();
        for (ConstraintViolation violation : ex.getConstraintViolations()) {
            errors.put(violation.getPropertyPath().toString(), violation.getMessage());
        }
        log.warn(ex.getClass().toString());

        String path = ((ServletWebRequest) request).getRequest().getRequestURI();

        ApiErrorResponse errorResponse = new ApiErrorResponse(
                status.value(),
                HttpStatus.valueOf(status.value()).getReasonPhrase(),
                "Validation failed for one or more fields",
                path,
                errors
        );

        return new ResponseEntity<>(errorResponse, headers, status);
    }

    // 2. Customizing HTTP Method Not Supported (e.g., sending POST to a GET endpoint)
    @Override
    protected ResponseEntity<Object> handleHttpRequestMethodNotSupported(
            HttpRequestMethodNotSupportedException ex,
            HttpHeaders headers,
            HttpStatusCode status,
            WebRequest request) {

        String path = ((ServletWebRequest) request).getRequest().getRequestURI();

        ApiErrorResponse errorResponse = new ApiErrorResponse(
                status.value(),
                HttpStatus.valueOf(status.value()).getReasonPhrase(),
                ex.getMessage(),
                path
        );

        return new ResponseEntity<>(errorResponse, headers, status);
    }

    // 3. Handling custom business exception (ResourceNotFoundException)
   /* @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleResourceNotFoundException(
            ResourceNotFoundException ex,
            WebRequest request) {

        String path = ((ServletWebRequest) request).getRequest().getRequestURI();
        HttpStatus status = HttpStatus.NOT_FOUND;

        ApiErrorResponse errorResponse = new ApiErrorResponse(
                status.value(),
                status.getReasonPhrase(),
                ex.getMessage(),
                path
        );

        return new ResponseEntity<>(errorResponse, status);
    }*/

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleAllUncaughtException(
            Exception ex,
            WebRequest request) {

        String path = ((ServletWebRequest) request).getRequest().getRequestURI();
        HttpStatus status = HttpStatus.INTERNAL_SERVER_ERROR;

        ApiErrorResponse errorResponse = new ApiErrorResponse(
                status.value(),
                status.getReasonPhrase(),
                "An unexpected internal server error occurred",
                path
        );

        return new ResponseEntity<>(errorResponse, status);
    }
}
