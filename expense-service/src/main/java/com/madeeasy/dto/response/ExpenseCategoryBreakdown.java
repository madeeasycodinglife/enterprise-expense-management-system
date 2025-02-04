package com.madeeasy.dto.response;

import com.madeeasy.entity.ExpenseCategory;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class ExpenseCategoryBreakdown {
    private int month;
    private int year;
    private ExpenseCategory category;
    private BigDecimal totalAmount;
}
