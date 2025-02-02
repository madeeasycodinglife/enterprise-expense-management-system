package com.madeeasy.repository;

import com.madeeasy.entity.Expense;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ExpenseRepository extends JpaRepository<Expense, Long> {
    List<Expense> findByCompanyId(Long companyId);

    List<Expense> findByEmployeeId(Long employeeId);
}

