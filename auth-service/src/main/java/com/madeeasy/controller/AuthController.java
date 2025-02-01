package com.madeeasy.controller;

import com.madeeasy.dto.request.AuthRequest;
import com.madeeasy.dto.response.AuthResponse;
import com.madeeasy.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping(path = "/auth-service")
public class AuthController {

    private final AuthService authService;

    @PostMapping(path = "/sign-up")
    public ResponseEntity<?> singUp(@Valid @RequestBody AuthRequest authRequest) {
        AuthResponse authResponse = authService.singUp(authRequest);
        // Return the appropriate HTTP status based on the response status in AuthResponse
        if (authResponse.getStatus() == HttpStatus.CONFLICT) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(authResponse);
        } else if (authResponse.getStatus() == HttpStatus.BAD_REQUEST) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(authResponse);
        } else if (authResponse.getStatus() == HttpStatus.SERVICE_UNAVAILABLE) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(authResponse);
        }
        return ResponseEntity.ok().body(authResponse);
    }

}
