package com.madeeasy.repository;

import com.madeeasy.entity.Approval;
import com.madeeasy.entity.ApprovalStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ApprovalRepository extends JpaRepository<Approval, Long> {

    List<Approval> findByExpenseId(Long expenseId);

    List<Approval> findByExpenseIdAndStatus(Long expenseId, ApprovalStatus status);

    // Custom query to find approval by expenseId, approvedBy email, and status 'PENDING'
    Optional<Approval> findByExpenseIdAndApprovedByAndStatus(Long expenseId, String approvedBy, ApprovalStatus status);

    Optional<Approval> findByExpenseIdAndApproverRoleAndStatus(Long expenseId, String role, ApprovalStatus approvalStatus);

    boolean existsByExpenseIdAndApprovedByAndStatusIn(Long expenseId, String approvedBy, Collection<ApprovalStatus> approved);

    @Query("SELECT a FROM Approval a WHERE a.companyDomain = :companyDomain " +
            "AND (:startYear IS NULL OR EXTRACT(YEAR FROM a.expenseDate) >= :startYear) " +
            "AND (:endYear IS NULL OR EXTRACT(YEAR FROM a.expenseDate) <= :endYear) " +
            "AND (:startMonth IS NULL OR EXTRACT(MONTH FROM a.expenseDate) >= :startMonth) " +
            "AND (:endMonth IS NULL OR EXTRACT(MONTH FROM a.expenseDate) <= :endMonth)")
    List<Approval> findApprovalsWithFilters(
            @Param("companyDomain") String companyDomain,
            @Param("startYear") Integer startYear,
            @Param("endYear") Integer endYear,
            @Param("startMonth") Integer startMonth,
            @Param("endMonth") Integer endMonth);
}
