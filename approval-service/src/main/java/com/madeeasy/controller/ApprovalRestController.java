package com.madeeasy.controller;

import com.madeeasy.dto.request.ExpenseRequestDTO;
import com.madeeasy.entity.Approval;
import com.madeeasy.service.ApprovalService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.UnsupportedEncodingException;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping(path = "/approval-service")
@SecurityRequirement(name = "Bearer Authentication") // Global security for all endpoints
@Tag(
        name = "Approval Workflow",
        description = "API for managing expense approval processes including requesting approval for expenses and handling approval workflows."
)
public class ApprovalRestController {

    private final ApprovalService approvalService;

    @Operation(
            summary = "Request Approval for an Expense",
            description = "Sends an expense approval request to the designated approver. This method triggers the approval workflow."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Approval request sent successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request data", content = @Content),
            @ApiResponse(responseCode = "500", description = "Internal server error", content = @Content)
    })
    @PostMapping(path = "/ask-for-approve")
    public ResponseEntity<?> askForApproval(
            @RequestBody ExpenseRequestDTO expenseRequestDTO) throws UnsupportedEncodingException {
        this.approvalService.askForApproval(expenseRequestDTO);
        return ResponseEntity.ok("Approval request sent successfully.");
    }

    @GetMapping("/get-approvals/{companyDomain}")
    public ResponseEntity<?> getApprovals(
            @PathVariable String companyDomain,
            @RequestParam(required = false) Integer startYear,
            @RequestParam(required = false) Integer endYear,
            @RequestParam(required = false) Integer startMonth,
            @RequestParam(required = false) Integer endMonth) {

        List<Approval> approvals = approvalService.getApprovals(companyDomain, startYear, endYear, startMonth, endMonth);
        if (!approvals.isEmpty()) {
            return ResponseEntity.status(HttpStatus.OK).body(approvals);
        }
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body("No approvals found for the specified criteria.");
    }
}
