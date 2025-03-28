package com.madeeasy.entity;

import com.madeeasy.dto.request.ExpenseCategory;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Entity
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Approval {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "approval_sequence_generator")
    @SequenceGenerator(
            name = "approval_sequence_generator",
            sequenceName = "approval_sequence",
            allocationSize = 1
    )
    private Long id;

    @Column(nullable = false)
    private Long expenseId;

    @Column(nullable = false)
    private String companyDomain;

    @Column(nullable = false)
    private String approverRole; // MANAGER, FINANCE, ADMIN

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ApprovalStatus status;

    @Column(nullable = false)
    private String approvedBy; // Email of approver

    @Column(nullable = false)
    private LocalDateTime expenseDate;

    @Column(nullable = false, updatable = false)
    private LocalDateTime approvalInitiationDate;

    @Column
    private LocalDateTime approvalCompletionDate;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false)
    private String description;

    @Column(nullable = false)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ExpenseCategory category;
}
