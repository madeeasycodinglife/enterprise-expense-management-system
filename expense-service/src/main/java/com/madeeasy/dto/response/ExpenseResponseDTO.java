package com.madeeasy.dto.response;

import com.madeeasy.entity.ExpenseCategory;
import com.madeeasy.entity.ExpenseStatus;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class ExpenseResponseDTO {

    private Long id;
    private Long employeeId;
    private Long companyId;
    private String title;
    private String description;
    private BigDecimal amount;
    private ExpenseCategory category;
    private ExpenseStatus status;
    private LocalDateTime expenseDate;
}

