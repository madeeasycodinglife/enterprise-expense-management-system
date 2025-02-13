package com.madeeasy.service;

import com.madeeasy.dto.request.ExpensePartialRequestDTO;
import com.madeeasy.dto.request.ExpenseRequestDTO;
import com.madeeasy.dto.response.ExpenseCategoryBreakdown;
import com.madeeasy.dto.response.ExpenseResponseDTO;
import com.madeeasy.dto.response.ExpenseTrend;
import com.madeeasy.entity.ExpenseCategory;

import java.util.List;

public interface ExpenseService {
    void submitExpense(ExpenseRequestDTO expenseRequestDTO); // Create

    ExpenseResponseDTO getExpenseById(Long id); // Read

    List<ExpenseResponseDTO> getAllExpenses(); // Read All

    ExpenseResponseDTO updateExpense(Long id, ExpensePartialRequestDTO expensePartialRequestDTO); // Update

    void deleteExpense(Long id); // Delete

    byte[] generateExpenseInvoice(String companyDomain, Integer startYear, Integer endYear, Integer startMonth, Integer endMonth, String category);


    List<ExpenseTrend> getMonthlyExpenseTrends(String companyDomain, Integer startYear, Integer endYear, Integer startMonth, Integer endMonth);

    List<ExpenseTrend> getYearlyExpenseTrends(String companyDomain, Integer startYear, Integer endYear);

    List<ExpenseCategoryBreakdown> getExpenseBreakdownByCategory(String companyDomain, Integer startYear, Integer endYear, Integer startMonth, Integer endMonth, ExpenseCategory category);

}
