package com.madeeasy.service;

import com.madeeasy.dto.request.ExpensePartialRequestDTO;
import com.madeeasy.dto.request.ExpenseRequestDTO;
import com.madeeasy.dto.response.ExpenseResponseDTO;

import java.util.List;

public interface ExpenseService {
    void submitExpense(ExpenseRequestDTO expenseRequestDTO); // Create

    ExpenseResponseDTO getExpenseById(Long id); // Read

    List<ExpenseResponseDTO> getAllExpenses(); // Read All

    ExpenseResponseDTO updateExpense(Long id, ExpensePartialRequestDTO expensePartialRequestDTO); // Update

    void deleteExpense(Long id); // Delete
}
