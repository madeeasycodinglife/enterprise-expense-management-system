package com.madeeasy.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/fallback")
public class GatewayController {

    @RequestMapping(path = "/approval-service")
    public Mono<ResponseEntity<Map<String, Object>>> approvalServiceFallBack() {
        Map<String, Object> response = new HashMap<>();
        response.put("status", HttpStatus.SERVICE_UNAVAILABLE.value());
        response.put("message", "The Approval Service is currently unavailable. Please try again later. For urgent issues, contact support at support@madeeasy.com.");
        return Mono.just(ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(response));
    }

    @RequestMapping(path = "/auth-service")
    public Mono<ResponseEntity<Map<String, Object>>> authServiceFallBack() {
        Map<String, Object> response = new HashMap<>();
        response.put("status", HttpStatus.SERVICE_UNAVAILABLE.value());
        response.put("message", "The Authentication Service is currently unavailable. Please try again later. For further assistance, reach out to support@madeeasy.com.");
        return Mono.just(ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(response));
    }

    @RequestMapping(path = "/company-service")
    public Mono<ResponseEntity<Map<String, Object>>> companyServiceFallBack() {
        Map<String, Object> response = new HashMap<>();
        response.put("status", HttpStatus.SERVICE_UNAVAILABLE.value());
        response.put("message", "The Company Service is currently unavailable. Please try again later. If the problem persists, please contact our support team at support@madeeasy.com.");
        return Mono.just(ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(response));
    }

    @RequestMapping(path = "/expense-service")
    public Mono<ResponseEntity<Map<String, Object>>> expenseServiceFallBack() {
        Map<String, Object> response = new HashMap<>();
        response.put("status", HttpStatus.SERVICE_UNAVAILABLE.value());
        response.put("message", "The Expense Service is currently unavailable. Please try again later. If you need immediate help, please contact support at support@madeeasy.com.");
        return Mono.just(ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(response));
    }

    @RequestMapping(path = "/notification-service")
    public Mono<ResponseEntity<Map<String, Object>>> notificationServiceFallBack() {
        Map<String, Object> response = new HashMap<>();
        response.put("status", HttpStatus.SERVICE_UNAVAILABLE.value());
        response.put("message", "The Notification Service is currently unavailable. Please try again later. If you need immediate help, please contact support at support@madeeasy.com.");
        return Mono.just(ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(response));
    }
}
