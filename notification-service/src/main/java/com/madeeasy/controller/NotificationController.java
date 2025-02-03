package com.madeeasy.controller;

import com.madeeasy.request.ApprovalRequestDTO;
import com.madeeasy.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;

@RestController
@RequiredArgsConstructor
@RequestMapping(path = "/notification-service")
public class NotificationController {

    private final NotificationService notificationService;

    // Endpoint to receive the request body (including expense details, approve/reject links)
    @PostMapping(path = "/")
    public ResponseEntity<?> approveExpenseTest(@RequestBody ApprovalRequestDTO approvalRequestDTO) {
        // Extract the expense details (the full string or as individual fields)
        String expenseDetails = approvalRequestDTO.getExpenseDetails();

        // Optionally: Parse the expenseDetails into individual fields if necessary
        String[] details = expenseDetails.split("&");
        Long expenseId = null;
        String title = null;
        String description = null;
        BigDecimal amount = null;
        String category = null;
        String expenseDate = null;
        String emailId = null;

        // Loop through and parse individual fields from the expenseDetails
        for (String detail : details) {
            if (detail.contains("expenseId")) {
                expenseId = Long.parseLong(detail.split("=")[1]);
            }
            if (detail.contains("title")) {
                title = detail.split("=")[1];
                title = java.net.URLDecoder.decode(title, StandardCharsets.UTF_8); // Decode the title
            }
            if (detail.contains("description")) {
                description = detail.split("=")[1];
                description = java.net.URLDecoder.decode(description, StandardCharsets.UTF_8); // Decode the description
            }
            if (detail.contains("amount")) {
                amount = new BigDecimal(detail.split("=")[1]);
            }
            if (detail.contains("category")) {
                category = detail.split("=")[1];
                category = java.net.URLDecoder.decode(category, StandardCharsets.UTF_8); // Decode the category
            }
            if (detail.contains("expenseDate")) {
                expenseDate = detail.split("=")[1];
                expenseDate = java.net.URLDecoder.decode(expenseDate, StandardCharsets.UTF_8); // Decode the expenseDate
            }
            if (detail.contains("emailId")) {
                emailId = detail.split("=")[1];
                emailId = java.net.URLDecoder.decode(emailId, StandardCharsets.UTF_8); // Decode the emailId
            }
        }

        // Log the decoded expense details
        System.out.println("Expense details: " + expenseId + ", " + title + ", " + description + ", " + amount + ", " + category + ", " + expenseDate + ", " + emailId);

        // Process the approval/rejection links
        String approveLink = approvalRequestDTO.getApproveLink();
        String rejectLink = approvalRequestDTO.getRejectLink();

        System.out.println("approvalLink: " + approveLink + ", rejectLink: " + rejectLink);

        // Call the service to send the approval/rejection email or notification
        notificationService.sendApprovalNotification(expenseId, title, description, amount, category, expenseDate, emailId, approveLink, rejectLink);

        return ResponseEntity.ok().build();
    }
}
