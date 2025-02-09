package com.madeeasy.controller;

import com.madeeasy.request.ApprovalRequestDTO;
import com.madeeasy.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;

@RestController
@RequiredArgsConstructor
@RequestMapping(path = "/notification-service")
@Tag(
        name = "Notification Service",
        description = "API for sending notifications related to expense approval requests, including approve and reject links."
)
public class NotificationController {

    private final NotificationService notificationService;

    @Operation(
            summary = "Send Approval Notification",
            description = "Receives an expense approval request and sends an email notification with approve/reject links.",
            security = @SecurityRequirement(name = "Bearer Authentication"),
            responses = {
                    @ApiResponse(responseCode = "200", description = "Notification sent successfully"),
                    @ApiResponse(responseCode = "400", description = "Invalid request format", content = @Content),
                    @ApiResponse(responseCode = "500", description = "Internal server error", content = @Content)
            }
    )
    @PostMapping(path = "/")
    public ResponseEntity<?> approveExpenseTest(@Valid @RequestBody ApprovalRequestDTO approvalRequestDTO) {
        String expenseDetails = approvalRequestDTO.getExpenseDetails();

        // Extract expense details
        String[] details = expenseDetails.split("&");
        Long expenseId = null;
        String title = null;
        String description = null;
        BigDecimal amount = null;
        String category = null;
        String expenseDate = null;
        String emailId = null;

        for (String detail : details) {
            if (detail.contains("expenseId")) {
                expenseId = Long.parseLong(detail.split("=")[1]);
            }
            if (detail.contains("title")) {
                title = java.net.URLDecoder.decode(detail.split("=")[1], StandardCharsets.UTF_8);
            }
            if (detail.contains("description")) {
                description = java.net.URLDecoder.decode(detail.split("=")[1], StandardCharsets.UTF_8);
            }
            if (detail.contains("amount")) {
                amount = new BigDecimal(detail.split("=")[1]);
            }
            if (detail.contains("category")) {
                category = java.net.URLDecoder.decode(detail.split("=")[1], StandardCharsets.UTF_8);
            }
            if (detail.contains("expenseDate")) {
                expenseDate = java.net.URLDecoder.decode(detail.split("=")[1], StandardCharsets.UTF_8);
            }
            if (detail.contains("emailId")) {
                emailId = java.net.URLDecoder.decode(detail.split("=")[1], StandardCharsets.UTF_8);
            }
        }

        // Extract approval and rejection links
        String approveLink = approvalRequestDTO.getApproveLink();
        String rejectLink = approvalRequestDTO.getRejectLink();

        // Call service to send email notification
        notificationService.sendApprovalNotification(expenseId, title, description, amount, category, expenseDate, emailId, approveLink, rejectLink);

        return ResponseEntity.ok().build();
    }
}
