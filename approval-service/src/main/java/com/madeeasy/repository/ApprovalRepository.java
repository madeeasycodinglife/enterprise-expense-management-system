package com.madeeasy.repository;

import com.madeeasy.entity.Approval;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ApprovalRepository extends JpaRepository<Approval, Long> {
    Optional<Approval> findByExpenseId(Long id);
}
