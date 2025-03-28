package com.madeeasy.exception.handler;

import com.madeeasy.exception.ClientException;
import com.madeeasy.exception.ResourceException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    @ExceptionHandler(ResourceException.class)
    public ResponseEntity<?> handleResourceException(ResourceException e) {
        Map<String, String> response = new HashMap<>();
        response.put("status", "SERVICE_UNAVAILABLE");
        response.put("message", e.getMessage());  // You can pass the exception message or any custom message

        // Log the exception (optional)
        log.error("Error: {}", e.getMessage());

        return ResponseEntity
                .status(HttpStatus.SERVICE_UNAVAILABLE)  // 503 Service Unavailable
                .body(response);  // Return the response map which Spring will convert to JSON
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

}
