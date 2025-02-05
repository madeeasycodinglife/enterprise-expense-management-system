package com.madeeasy.exception;

import lombok.Data;
import org.springframework.http.HttpStatus;

@Data
public class ClientException extends RuntimeException {
    private HttpStatus status;
    private String message;

    public ClientException(String message) {
        super(message);
    }

    public ClientException(String message, HttpStatus status) {
        this.message = message;
        this.status = status;
    }
}
