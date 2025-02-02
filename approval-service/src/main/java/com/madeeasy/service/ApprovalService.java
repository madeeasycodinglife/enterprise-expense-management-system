package com.madeeasy.service;

import com.madeeasy.dto.request.ExpenseRequestDTO;

import java.math.BigDecimal;

public interface ApprovalService {
    void askForApproval(ExpenseRequestDTO expenseRequestDTO);

    void approveExpenseFromEmail(Long expenseId,
                                 String title,
                                 String description,
                                 BigDecimal amount,
                                 String category,
                                 String expenseDate,
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
