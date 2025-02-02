package com.madeeasy.service;

import com.madeeasy.dto.request.ExpenseRequestDTO;

public interface ExpenseService {
    void submitExpense(ExpenseRequestDTO expenseRequestDTO);
}
