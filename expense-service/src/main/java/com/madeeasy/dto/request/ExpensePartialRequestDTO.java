package com.madeeasy.dto.request;

import com.madeeasy.entity.ExpenseCategory;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.PastOrPresent;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class ExpensePartialRequestDTO {

    private String title;

    private String description;

    @DecimalMin(value = "0.01", message = "Amount must be greater than 0")
    private BigDecimal amount;

    private ExpenseCategory category;

    @PastOrPresent(message = "Expense date cannot be in the future")
    private LocalDateTime expenseDate; // Employee-provided date
}
