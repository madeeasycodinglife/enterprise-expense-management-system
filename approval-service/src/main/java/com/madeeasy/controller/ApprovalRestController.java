package com.madeeasy.controller;

import com.madeeasy.dto.request.ExpenseRequestDTO;
import com.madeeasy.service.ApprovalService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.UnsupportedEncodingException;

@RestController
@RequiredArgsConstructor
@RequestMapping(path = "/approval-service")
public class ApprovalRestController {

    private final ApprovalService approvalService;

    @PostMapping(path = "/ask-for-approve")
    public ResponseEntity<?> askForApproval(@RequestBody ExpenseRequestDTO expenseRequestDTO) throws UnsupportedEncodingException {
        this.approvalService.askForApproval(expenseRequestDTO);
        return ResponseEntity.ok("Approval request sent successfully.");
    }
}
