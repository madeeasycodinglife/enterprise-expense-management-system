package com.madeeasy.repository;

import com.madeeasy.entity.Expense;
import com.madeeasy.entity.ExpenseCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ExpenseRepository extends JpaRepository<Expense, Long> {
    List<Expense> findByCompanyDomain(String domainName);

    List<Expense> findByEmployeeId(Long employeeId);
    @Query("SELECT e FROM Expense e WHERE e.category = :category AND e.companyDomain = :companyDomain")
    List<Expense> findExpensesByCategoryAndCompanyDomain(
            @Param("category") ExpenseCategory category,  // Change String to ExpenseCategory
            @Param("companyDomain") String companyDomain
    );
}

