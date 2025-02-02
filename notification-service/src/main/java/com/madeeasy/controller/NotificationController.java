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

@RestController
@RequiredArgsConstructor
@RequestMapping(path = "/notification-service")
public class NotificationController {

    private final NotificationService notificationService;

    // Endpoint to receive the request body (including expense details, approve/reject links)
    @PostMapping
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
            }
            if (detail.contains("description")) {
                description = detail.split("=")[1];
            }
            if (detail.contains("amount")) {
                amount = new BigDecimal(detail.split("=")[1]);
            }
            if (detail.contains("category")) {
                category = detail.split("=")[1];
            }
            if (detail.contains("expenseDate")) {
                expenseDate = detail.split("=")[1];
            }
            if (detail.contains("emailId")) {
                emailId = detail.split("=")[1];
            }
        }

        // Process the approval/rejection links
        String approveLink = approvalRequestDTO.getApproveLink();
        String rejectLink = approvalRequestDTO.getRejectLink();

        // Call the service to send the approval/rejection email or notification
        notificationService.sendApprovalNotification(expenseId, title, description, amount, category, expenseDate,emailId, approveLink, rejectLink);

        return ResponseEntity.ok().build();
    }
}
