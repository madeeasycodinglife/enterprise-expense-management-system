package com.madeeasy.service;

import com.madeeasy.dto.request.ExpenseRequestDTO;
import com.madeeasy.entity.Approval;

import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.util.List;

public interface ApprovalService {
    void askForApproval(ExpenseRequestDTO expenseRequestDTO) throws UnsupportedEncodingException;

    List<Approval> getApprovals(String companyDomain, Integer startYear, Integer endYear, Integer startMonth, Integer endMonth);

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

    boolean hasAlreadyResponded(Long expenseId, String employeeEmail);
}
