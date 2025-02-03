package com.madeeasy.repository;

import com.madeeasy.entity.Approval;
import com.madeeasy.entity.ApprovalStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ApprovalRepository extends JpaRepository<Approval, Long> {

    List<Approval> findByExpenseId(Long expenseId);

    List<Approval> findByExpenseIdAndStatus(Long expenseId, ApprovalStatus status);

    // Custom query to find approval by expenseId, approvedBy email, and status 'PENDING'
    Optional<Approval> findByExpenseIdAndApprovedByAndStatus(Long expenseId, String approvedBy, ApprovalStatus status);

    Optional<Approval> findByExpenseIdAndApproverRoleAndStatus(Long expenseId, String role, ApprovalStatus approvalStatus);
}
