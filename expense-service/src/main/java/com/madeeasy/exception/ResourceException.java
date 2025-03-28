package com.madeeasy.exception;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.springframework.http.HttpStatus;

@Data
@AllArgsConstructor
public class ResourceException extends RuntimeException {

    private HttpStatus status;
    private String message;
}
