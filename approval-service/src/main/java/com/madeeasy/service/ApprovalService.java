package com.madeeasy.service;

import com.madeeasy.dto.request.ExpenseRequestDTO;

import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;

public interface ApprovalService {
    void askForApproval(ExpenseRequestDTO expenseRequestDTO) throws UnsupportedEncodingException;

    void approveExpenseFromEmail(Long expenseId,
                                 String title,
                                 String description,
                                 BigDecimal amount,
                                 String category,
                                 String expenseDate,
                                 String accessToken,
                                 String emailId,
                                 String role);

    void rejectExpenseFromEmail(Long expenseId,
                                String title,
                                String description,
                                BigDecimal amount,
                                String category,
                                String expenseDate,
                                String emailId,
                                String role);
}
