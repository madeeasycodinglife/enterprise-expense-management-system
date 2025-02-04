package com.madeeasy.service;

import com.madeeasy.dto.request.ExpensePartialRequestDTO;
import com.madeeasy.dto.request.ExpenseRequestDTO;
import com.madeeasy.dto.response.ExpenseResponseDTO;
import com.madeeasy.dto.response.ExpenseTrend;

import java.util.List;

public interface ExpenseService {
    void submitExpense(ExpenseRequestDTO expenseRequestDTO); // Create

    ExpenseResponseDTO getExpenseById(Long id); // Read

    List<ExpenseResponseDTO> getAllExpenses(); // Read All

    ExpenseResponseDTO updateExpense(Long id, ExpensePartialRequestDTO expensePartialRequestDTO); // Update

    void deleteExpense(Long id); // Delete

    List<ExpenseResponseDTO> getExpensesByCategoryAndCompanyDomain(String domainName, String category);

    byte[] generateExpenseInvoice(String domainName);


    List<ExpenseTrend> getMonthlyExpenseTrends(String companyDomain, Integer startYear, Integer endYear, Integer startMonth, Integer endMonth);

    List<ExpenseTrend> getYearlyExpenseTrends(String companyDomain, Integer startYear, Integer endYear);
}
