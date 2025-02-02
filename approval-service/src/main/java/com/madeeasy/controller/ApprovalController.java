package com.madeeasy.controller;


import com.madeeasy.dto.request.ExpenseRequestDTO;
import com.madeeasy.service.ApprovalService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

@RestController
@RequiredArgsConstructor
@RequestMapping(path = "/approval-service")
public class ApprovalController {

    private final ApprovalService approvalService;

    @PostMapping(path = "/ask-for-approve")
    public ResponseEntity<?> askForApproval(@RequestBody ExpenseRequestDTO expenseRequestDTO) {
        this.approvalService.askForApproval(expenseRequestDTO);
        return null;
    }

    @GetMapping(path = "/approve")
    public String approveExpense(@RequestParam Long expenseId, @RequestParam String title, @RequestParam String description,
                                 @RequestParam BigDecimal amount, @RequestParam String category, @RequestParam String expenseDate,
                                 @RequestParam String emailId, @RequestParam String role) {
        System.out.println("expenseId " + expenseId + "title " + title + " description " + description + " amount " + amount + " category " + category + " expenseDate " + expenseDate + " email " + emailId + " role " + role);
        try {
            approvalService.approveExpenseFromEmail(expenseId, title, description, amount, category, expenseDate, emailId, role);  // Call service to approve expense
//            model.addAttribute("result", result);  // Pass the result to the view
//            model.addAttribute("action", "approved");
        } catch (Exception e) {
//            model.addAttribute("result", "An error occurred while processing the approval.");
//            model.addAttribute("action", "error");
        }
        return "approvalResult";  // Thymeleaf template for approval result
    }

    @GetMapping(path = "/reject")
    public String rejectExpense(@RequestParam Long expenseId, @RequestParam String title, @RequestParam String description,
                                @RequestParam BigDecimal amount, @RequestParam String category, @RequestParam String expenseDate,
                                @RequestParam String emailId, @RequestParam String role) {
        System.out.println("expenseId " + expenseId + "title " + title + " description " + description + " amount " + amount + " category " + category + " expenseDate " + expenseDate + " email " + emailId + " role " + role);
        try {
//            String result = expenseService.rejectExpense(expenseId, email);  // Call service to reject expense
//            model.addAttribute("result", result);  // Pass the result to the view
//            model.addAttribute("action", "rejected");
        } catch (Exception e) {
//            model.addAttribute("result", "An error occurred while processing the rejection.");
//            model.addAttribute("action", "error");
        }
        return "approvalResult";  // Thymeleaf template for rejection result
    }
}
