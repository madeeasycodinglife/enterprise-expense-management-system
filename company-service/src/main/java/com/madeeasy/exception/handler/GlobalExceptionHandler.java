package com.madeeasy.exception.handler;

import com.madeeasy.exception.ClientException;
import com.madeeasy.exception.ResourceNotFoundException;
import com.madeeasy.exception.UnauthorizedAccessException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.*;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.time.LocalDateTime;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        List<String> errorMessages = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .collect(Collectors.toList());

        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("timestamp", LocalDateTime.now());
        errorResponse.put("status", HttpStatus.BAD_REQUEST);
        errorResponse.put("error", "Validation Failed");
        errorResponse.put("errors", errorMessages);

        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }

    @Override
    protected ResponseEntity<Object> handleNoHandlerFoundException(NoHandlerFoundException ex,
                                                                   HttpHeaders headers,
                                                                   HttpStatusCode status,
                                                                   WebRequest request) {

        log.error("handleNoHandlerFoundException: {}", ex.getMessage());
        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("status", HttpStatus.NOT_FOUND);
        errorResponse.put("error", "Not Found");
        errorResponse.put("message", "The requested URL was not found on this server.");
        errorResponse.put("path", request.getDescription(false).substring(4)); // Removing the 'uri=' prefix
        errorResponse.put("timestamp", LocalDateTime.now().toString());

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
    }

    @Override
    protected ResponseEntity<Object> handleHttpRequestMethodNotSupported(HttpRequestMethodNotSupportedException ex,
                                                                         HttpHeaders headers,
                                                                         HttpStatusCode status,
                                                                         WebRequest request) {
        log.error("handleHttpRequestMethodNotSupported: {}", ex.getMessage());
        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("status", HttpStatus.METHOD_NOT_ALLOWED);
        errorResponse.put("error", "Method Not Allowed");
        errorResponse.put("message", "The HTTP method is not supported for this endpoint.");

        // Convert HttpMethod set to a list of strings
        List<String> supportedMethods = Objects.requireNonNull(ex.getSupportedHttpMethods()).stream()
                .map(HttpMethod::name)
                .collect(Collectors.toList());
        errorResponse.put("supported_methods", supportedMethods);

        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED).body(errorResponse);
    }


    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Map<String, Object>> handleDataIntegrityViolationException(DataIntegrityViolationException ex) {
        Map<String, Object> response = new HashMap<>();
        response.put("status", HttpStatus.BAD_REQUEST);

        String fieldDetails = extractFieldDetails(ex.getMessage());
        response.put("message", fieldDetails != null ? fieldDetails : "Data integrity violation error occurred.");

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(response);
    }

    private static String extractFieldDetails(String message) {
        if (message != null) {
            // Regex to capture field names within parentheses after the constraint name
            String regex = "ON [^\\s]+\\(([^)]+?)\\s*(NULLS FIRST)?\\)";
            Pattern pattern = Pattern.compile(regex, Pattern.CASE_INSENSITIVE);
            Matcher matcher = pattern.matcher(message);

            if (matcher.find()) {
                // Extract field names and format them
                String fields = matcher.group(1);
                return Arrays.stream(fields.split(","))
                        .map(String::trim)
                        .map(field -> field + " must be unique.")
                        .collect(Collectors.joining(" "));
            }
        }
        return "Data integrity violation error occurred.";
    }


    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<Map<String, Object>> handleMethodArgumentTypeMismatchException(
            MethodArgumentTypeMismatchException ex) {

        log.warn("Invalid value '{}' for parameter '{}'. Expected type is '{}'.",
                ex.getValue(), ex.getName(), ex.getRequiredType().getSimpleName());

        Map<String, Object> errorResponse = Map.of(
                "status", HttpStatus.BAD_REQUEST,
                "message", String.format("Invalid value '%s' for parameter '%s'. Expected type is '%s'.",
                        ex.getValue(), ex.getName(), ex.getRequiredType().getSimpleName())
        );

        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(ClientException.class)
    public ResponseEntity<?> handleClientException(ClientException e) {
        Map<String, String> response = new HashMap<>();
        response.put("status", e.getStatus().toString());
        response.put("message", e.getMessage());

        // Log the exception (optional)
        log.error("Error occurred: {}", e.getMessage());

        return ResponseEntity.status(e.getStatus()).body(response);
    }

    // Handle UnauthorizedAccessException
    @ExceptionHandler(UnauthorizedAccessException.class)
    public ResponseEntity<Map<String, Object>> handleUnauthorizedAccessException(UnauthorizedAccessException ex) {
        // Log the error (optional)
        // Example: log.error("Unauthorized access: {}", ex.getMessage());

        // Build the error response as a Map
        Map<String, Object> response = new HashMap<>();
        response.put("status", ex.getStatus().value());  // Numeric value of HTTP status code
        response.put("message", ex.getMessage());
        response.put("timestamp", System.currentTimeMillis());  // Timestamp of the error

        return new ResponseEntity<>(response, ex.getStatus());
    }

    // Handle ResourceNotFoundException
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleResourceNotFoundException(ResourceNotFoundException ex) {
        // Log the error (optional)
        // Example: log.error("Resource not found: {}", ex.getMessage());

        // Build the error response as a Map
        Map<String, Object> response = new HashMap<>();
        response.put("status", ex.getStatus().value());  // Numeric value of HTTP status code
        response.put("message", ex.getMessage());
        response.put("timestamp", System.currentTimeMillis());  // Timestamp of the error

        return new ResponseEntity<>(response, ex.getStatus());
    }

    // Handle any other uncaught exceptions
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGeneralException(Exception ex) {
        // Log the error (optional)
        // Example: log.error("Unexpected error: {}", ex.getMessage());

        // Build a generic error response in case of unexpected errors
        Map<String, Object> response = new HashMap<>();
        response.put("status", HttpStatus.INTERNAL_SERVER_ERROR.value());  // 500 status code
        response.put("message", "An unexpected error occurred. Please try again later.");
        response.put("timestamp", System.currentTimeMillis());

        return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
