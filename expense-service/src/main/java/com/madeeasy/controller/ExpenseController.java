package com.madeeasy.controller;

import com.madeeasy.dto.request.ExpenseRequestDTO;
import com.madeeasy.service.ExpenseService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(path = "/expense-service")
@RequiredArgsConstructor
public class ExpenseController {
    private final ExpenseService expenseService;

    @PostMapping(path = "/submit")
    public ResponseEntity<?> submitExpense(@Valid @RequestBody ExpenseRequestDTO request) {
        this.expenseService.submitExpense(request);
        return null;
    }
}
