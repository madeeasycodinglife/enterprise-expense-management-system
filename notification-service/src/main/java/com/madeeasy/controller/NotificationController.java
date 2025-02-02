package com.madeeasy.controller;


import com.madeeasy.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping(path = "/notification-service")
public class NotificationController {

    private final NotificationService notificationService;

    @PostMapping(path = "/approve/{expenseId}")
    public ResponseEntity<?> approveExpenseTest(@PathVariable Long expenseId) {
        this.notificationService.sendApprovalNotification(expenseId);
        return null;
    }

    /**
     * The below two are only for test  purposes
     */
    // Endpoint to handle approval
    @GetMapping(path = "/approve")
    public String approveExpense(@RequestParam Long expenseId) {
        // Simulate approval action (could involve updating the expense status, etc.)
        return "Expense " + expenseId + " has been approved.";
    }

    // Endpoint to handle rejection
    @GetMapping(path = "/reject")
    public String rejectExpense(@RequestParam Long expenseId) {
        // Simulate rejection action
        return "Expense " + expenseId + " has been rejected.";
    }

}
