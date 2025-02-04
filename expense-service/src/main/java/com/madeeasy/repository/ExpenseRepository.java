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

    // Adjusted query: using YEAR and MONTH functions to directly get the year and month
    @Query("SELECT EXTRACT(YEAR FROM e.expenseDate) AS year, EXTRACT(MONTH FROM e.expenseDate) AS month, SUM(e.amount) AS totalAmount " +
            "FROM Expense e WHERE e.companyDomain = :companyDomain " +
            "GROUP BY EXTRACT(YEAR FROM e.expenseDate), EXTRACT(MONTH FROM e.expenseDate) ORDER BY year DESC, month DESC")
    List<Object[]> findMonthlyExpenseTrendsByCompanyDomain(@Param("companyDomain") String companyDomain);

    // Adjusted query for yearly trend
    @Query("SELECT EXTRACT(YEAR FROM e.expenseDate) AS year, SUM(e.amount) AS totalAmount " +
            "FROM Expense e WHERE e.companyDomain = :companyDomain " +
            "GROUP BY EXTRACT(YEAR FROM e.expenseDate) ORDER BY year DESC")
    List<Object[]> findYearlyExpenseTrendsByCompanyDomain(@Param("companyDomain") String companyDomain);

    @Query("SELECT EXTRACT(YEAR FROM e.expenseDate) AS year, EXTRACT(MONTH FROM e.expenseDate) AS month, SUM(e.amount) AS totalAmount " +
            "FROM Expense e WHERE e.companyDomain = :companyDomain " +
            "AND (:startYear IS NULL OR EXTRACT(YEAR FROM e.expenseDate) >= :startYear) " +
            "AND (:endYear IS NULL OR EXTRACT(YEAR FROM e.expenseDate) <= :endYear) " +
            "AND (:startMonth IS NULL OR EXTRACT(MONTH FROM e.expenseDate) >= :startMonth) " +
            "AND (:endMonth IS NULL OR EXTRACT(MONTH FROM e.expenseDate) <= :endMonth) " +
            "GROUP BY EXTRACT(YEAR FROM e.expenseDate), EXTRACT(MONTH FROM e.expenseDate) " +
            "ORDER BY year DESC, month DESC")
    List<Object[]> findMonthlyExpenseTrendsByCompanyDomain(
            @Param("companyDomain") String companyDomain,
            @Param("startYear") Integer startYear,
            @Param("endYear") Integer endYear,
            @Param("startMonth") Integer startMonth,
            @Param("endMonth") Integer endMonth);

    @Query("SELECT EXTRACT(YEAR FROM e.expenseDate) AS year, SUM(e.amount) AS totalAmount " +
            "FROM Expense e WHERE e.companyDomain = :companyDomain " +
            "AND (:startYear IS NULL OR EXTRACT(YEAR FROM e.expenseDate) >= :startYear) " +
            "AND (:endYear IS NULL OR EXTRACT(YEAR FROM e.expenseDate) <= :endYear) " +
            "GROUP BY EXTRACT(YEAR FROM e.expenseDate) ORDER BY year DESC")
    List<Object[]> findYearlyExpenseTrendsByCompanyDomain(
            @Param("companyDomain") String companyDomain,
            @Param("startYear") Integer startYear,
            @Param("endYear") Integer endYear);

}

