package com.madeeasy.exception;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.springframework.http.HttpStatus;

@Data
@AllArgsConstructor
public class UnauthorizedAccessException extends RuntimeException {
    private HttpStatus status;
    private String message;

    public UnauthorizedAccessException(String message) {
        super(message);
    }
}
