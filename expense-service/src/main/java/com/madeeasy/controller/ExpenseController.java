package com.madeeasy.controller;

import com.madeeasy.dto.request.ExpensePartialRequestDTO;
import com.madeeasy.dto.request.ExpenseRequestDTO;
import com.madeeasy.service.ExpenseService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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
    // get expense by id
    @GetMapping(path = "/get/{id}")
    public ResponseEntity<?> getExpenseById(@PathVariable("id") Long id) {
        return ResponseEntity.ok().body(this.expenseService.getExpenseById(id));
    }
    // get-all expenses
    @GetMapping(path = "/get-all-expenses")
    public ResponseEntity<?> getAllExpenses() {
        return ResponseEntity.ok().body(this.expenseService.getAllExpenses());
    }
    // delete expense by expense id
    @DeleteMapping(path = "/delete/{id}")
    public ResponseEntity<?> deleteExpense(@PathVariable("id") Long id) {
        this.expenseService.deleteExpense(id);
        return null;
    }
    // partial-update by expense id
    @PatchMapping(path = "/update/{id}")
    public ResponseEntity<?> updateExpense(@PathVariable("id") Long id, @Valid @RequestBody ExpensePartialRequestDTO expensePartialRequestDTO) {
        return ResponseEntity.ok().body(this.expenseService.updateExpense(id, expensePartialRequestDTO));
    }
}
