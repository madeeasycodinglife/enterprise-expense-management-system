package com.madeeasy.controller;

import com.madeeasy.dto.request.ExpensePartialRequestDTO;
import com.madeeasy.dto.request.ExpenseRequestDTO;
import com.madeeasy.dto.response.ExpenseCategoryBreakdown;
import com.madeeasy.dto.response.ExpenseResponseDTO;
import com.madeeasy.dto.response.ExpenseTrend;
import com.madeeasy.entity.ExpenseCategory;
import com.madeeasy.service.ExpenseService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping(path = "/expense-service")
@RequiredArgsConstructor
public class ExpenseController {
    private final ExpenseService expenseService;

    @PostMapping(path = "/submit")
    public ResponseEntity<?> submitExpense(@Valid @RequestBody ExpenseRequestDTO request) {
        this.expenseService.submitExpense(request);
        return null;
    }

    // get expense by id
    @GetMapping(path = "/get/{id}")
    public ResponseEntity<?> getExpenseById(@PathVariable("id") Long id) {
        return ResponseEntity.ok().body(this.expenseService.getExpenseById(id));
    }

    // get-all expenses
    @GetMapping(path = "/get-all-expenses")
    public ResponseEntity<?> getAllExpenses() {
        return ResponseEntity.ok().body(this.expenseService.getAllExpenses());
    }

    // delete expense by expense id
    @DeleteMapping(path = "/delete/{id}")
    public ResponseEntity<?> deleteExpense(@PathVariable("id") Long id) {
        this.expenseService.deleteExpense(id);
        return null;
    }

    // partial-update by expense id
    @PatchMapping(path = "/update/{id}")
    public ResponseEntity<?> updateExpense(@PathVariable("id") Long id, @Valid @RequestBody ExpensePartialRequestDTO expensePartialRequestDTO) {
        return ResponseEntity.ok().body(this.expenseService.updateExpense(id, expensePartialRequestDTO));
    }

    @GetMapping("/category/{category}/{domainName}")
    public ResponseEntity<?> getExpensesByCategoryAndCompany(
            @PathVariable String domainName,
            @PathVariable String category) {
        List<ExpenseResponseDTO> expenses = expenseService.getExpensesByCategoryAndCompanyDomain(domainName, category);
        return ResponseEntity.ok(expenses);
    }

    @GetMapping("/generate/invoice/{domainName}")
    public ResponseEntity<byte[]> generateInvoice(
            @PathVariable String domainName,
            @RequestParam(required = false) Integer startYear, // Optional start year filter
            @RequestParam(required = false) Integer endYear,   // Optional end year filter
            @RequestParam(required = false) Integer startMonth, // Optional start month filter
            @RequestParam(required = false) Integer endMonth,   // Optional end month filter
            @RequestParam(required = false) String category) {  // Optional category filter

        byte[] pdfBytes = expenseService.generateExpenseInvoice(
                domainName, startYear, endYear, startMonth, endMonth, category);

        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=Expense_Invoice.pdf");
        headers.set(HttpHeaders.CONTENT_TYPE, "application/pdf");

        return new ResponseEntity<>(pdfBytes, headers, HttpStatus.OK);
    }



    // Endpoint for monthly expense trends with optional filters for start/end year and month
    @GetMapping("/expenses/{companyDomain}/monthly-trends")
    public ResponseEntity<?> getMonthlyExpenseTrends(
            @PathVariable String companyDomain,
            @RequestParam(required = false) Integer startYear,  // Optional start year filter
            @RequestParam(required = false) Integer endYear,    // Optional end year filter
            @RequestParam(required = false) Integer startMonth, // Optional start month filter
            @RequestParam(required = false) Integer endMonth) { // Optional end month filter

        List<ExpenseTrend> trends = this.expenseService.getMonthlyExpenseTrends(
                companyDomain, startYear, endYear, startMonth, endMonth);

        return ResponseEntity.ok(trends);
    }

    // Endpoint for yearly expense trends with optional filters for start/end year
    @GetMapping("/expenses/{companyDomain}/yearly-trends")
    public ResponseEntity<?> getYearlyExpenseTrends(
            @PathVariable String companyDomain,
            @RequestParam(required = false) Integer startYear, // Optional start year filter
            @RequestParam(required = false) Integer endYear) {  // Optional end year filter

        List<ExpenseTrend> trends = this.expenseService.getYearlyExpenseTrends(
                companyDomain, startYear, endYear);

        return ResponseEntity.ok(trends);
    }

    @GetMapping("/expenses/{companyDomain}/category-breakdown")
    public ResponseEntity<?> getExpenseCategoryBreakdown(
            @PathVariable String companyDomain,
            @RequestParam(required = false) Integer startYear,
            @RequestParam(required = false) Integer endYear,
            @RequestParam(required = false) Integer startMonth,
            @RequestParam(required = false) Integer endMonth,
            @RequestParam(required = false) String category) {

        // Convert the category to ExpenseCategory enum if not null
        ExpenseCategory categoryToUse = null;
        if (category != null && !category.isEmpty()) {
            try {
                categoryToUse = ExpenseCategory.valueOf(category.toUpperCase()); // Convert to enum
            } catch (IllegalArgumentException e) {
                // If the category doesn't match any enum value, return an error
                return ResponseEntity.badRequest().body("Invalid category provided");
            }
        }

        // Call the service method
        List<ExpenseCategoryBreakdown> breakdown = this.expenseService.getExpenseBreakdownByCategory(
                companyDomain, startYear, endYear, startMonth, endMonth, categoryToUse);

        return ResponseEntity.ok(breakdown);
    }


}
