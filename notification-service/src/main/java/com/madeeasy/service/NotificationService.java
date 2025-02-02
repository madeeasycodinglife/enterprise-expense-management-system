package com.madeeasy.service;

import java.math.BigDecimal;

public interface NotificationService {
    void sendApprovalNotification(Long expenseId,
                                  String title,
                                  String description,
                                  BigDecimal amount,
                                  String category,
                                  String expenseDate,
                                  String emailId,
                                  String approvalLink,
                                  String rejectLink);
}
