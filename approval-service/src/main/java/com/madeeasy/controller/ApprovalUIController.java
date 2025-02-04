package com.madeeasy.controller;

import com.madeeasy.service.ApprovalService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.math.BigDecimal;

@Controller
@RequiredArgsConstructor
@RequestMapping(path = "/approval-service")
public class ApprovalUIController {

    private final ApprovalService approvalService;

    @GetMapping(path = "/approve")
    public String approveExpense(@RequestParam Long expenseId, @RequestParam String title, @RequestParam String description,
                                 @RequestParam BigDecimal amount, @RequestParam String category, @RequestParam String expenseDate,
                                 @RequestParam String accessToken, @RequestParam String emailId, @RequestParam String role, Model model) {
        try {
            String[] email = emailId.split(",");
            approvalService.approveExpenseFromEmail(expenseId, title, description, amount, category, expenseDate, accessToken, email[0], role);

            // ✅ Pass attributes to Thymeleaf template for Approval UI
            model.addAttribute("action", "approved");
            model.addAttribute("expenseTitle", title);
            model.addAttribute("amount", amount);
        } catch (Exception e) {
            model.addAttribute("action", "error");
        }
        return "approvalResult";  // Redirect to Thymeleaf UI
    }

    @GetMapping(path = "/reject")
    public String rejectExpense(@RequestParam Long expenseId, @RequestParam String title, @RequestParam String description,
                                @RequestParam BigDecimal amount, @RequestParam String category, @RequestParam String expenseDate,
                                @RequestParam String emailId, @RequestParam String role, Model model) {
        try {
            String[] email = emailId.split(",");
            approvalService.rejectExpenseFromEmail(expenseId, title, description, amount, category, expenseDate, email[0], role);

            // ✅ Ensure attributes are passed for Rejection UI
            model.addAttribute("action", "rejected");
            model.addAttribute("expenseTitle", title);
            model.addAttribute("amount", amount);
        } catch (Exception e) {
            model.addAttribute("action", "error");
        }
        return "approvalResult";  // Redirect to Thymeleaf UI
    }
}
