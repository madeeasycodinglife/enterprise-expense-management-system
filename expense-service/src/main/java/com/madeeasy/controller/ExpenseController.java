package com.madeeasy.controller;

import com.madeeasy.dto.request.ExpensePartialRequestDTO;
import com.madeeasy.dto.request.ExpenseRequestDTO;
import com.madeeasy.dto.response.ExpenseCategoryBreakdown;
import com.madeeasy.dto.response.ExpenseTrend;
import com.madeeasy.entity.ExpenseCategory;
import com.madeeasy.service.ExpenseService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
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

    @Operation(summary = "Submit a new expense", description = "Registers a new expense entry.", tags = {"Expense Management"})
    @ApiResponse(responseCode = "201", description = "Expense submitted successfully")
    @PostMapping(path = "/submit")
    @SecurityRequirement(name = "Bearer Authentication")
    public ResponseEntity<?> submitExpense(@Valid @RequestBody ExpenseRequestDTO request) {
        this.expenseService.submitExpense(request);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @Operation(summary = "Get an expense by ID", description = "Fetches details of a specific expense by its ID.", tags = {"Expense Management"})
    @ApiResponse(responseCode = "200", description = "Expense found")
    @GetMapping(path = "/get/{id}")
    @SecurityRequirement(name = "Bearer Authentication")
    public ResponseEntity<?> getExpenseById(@PathVariable("id") Long id) {
        return ResponseEntity.ok(this.expenseService.getExpenseById(id));
    }

    @Operation(summary = "Get all expenses", description = "Retrieves a list of all expenses.", tags = {"Expense Management"})
    @ApiResponse(responseCode = "200", description = "List of expenses retrieved successfully")
    @GetMapping(path = "/get-all-expenses")
    @SecurityRequirement(name = "Bearer Authentication")
    public ResponseEntity<?> getAllExpenses() {
        return ResponseEntity.ok(this.expenseService.getAllExpenses());
    }

    @Operation(summary = "Delete an expense", description = "Removes an expense by its ID.", tags = {"Expense Management"})
    @ApiResponse(responseCode = "204", description = "Expense deleted successfully")
    @DeleteMapping(path = "/delete/{id}")
    @SecurityRequirement(name = "Bearer Authentication")
    public ResponseEntity<?> deleteExpense(@PathVariable("id") Long id) {
        this.expenseService.deleteExpense(id);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Partially update an expense", description = "Allows updating selected fields of an expense.", tags = {"Expense Management"})
    @ApiResponse(responseCode = "200", description = "Expense updated successfully")
    @PatchMapping(path = "/update/{id}")
    @SecurityRequirement(name = "Bearer Authentication")
    public ResponseEntity<?> updateExpense(@PathVariable("id") Long id, @Valid @RequestBody ExpensePartialRequestDTO expensePartialRequestDTO) {
        return ResponseEntity.ok(this.expenseService.updateExpense(id, expensePartialRequestDTO));
    }

    @Operation(summary = "Generate expense invoice", description = "Generates a PDF invoice for expenses based on filters.", tags = {"Expense Reports"})
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Invoice generated successfully",
                    content = @Content(
                            mediaType = "application/pdf",  // Specify the media type for PDF
                            schema = @Schema(type = "string", format = "binary")  // Indicate binary data
                    )
            ),
            @ApiResponse(responseCode = "400", description = "Invalid request data")
    })
    @GetMapping("/generate/invoice/{domainName}")
    @SecurityRequirement(name = "Bearer Authentication")
    public ResponseEntity<byte[]> generateInvoice(@PathVariable String domainName,
                                                  @RequestParam(required = false) Integer startYear,
                                                  @RequestParam(required = false) Integer endYear,
                                                  @RequestParam(required = false) Integer startMonth,
                                                  @RequestParam(required = false) Integer endMonth,
                                                  @RequestParam(required = false) String category) {
        byte[] pdfBytes = expenseService.generateExpenseInvoice(domainName, startYear, endYear, startMonth, endMonth, category);
        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=Expense_Invoice.pdf");
        headers.set(HttpHeaders.CONTENT_TYPE, "application/pdf");
        return new ResponseEntity<>(pdfBytes, headers, HttpStatus.OK);
    }

    // 📌 1️⃣ Monthly Expense Trends Endpoint
    @Operation(
            summary = "Get monthly expense trends",
            description = "Fetches monthly expense trends based on optional filters for year and month.",
            tags = {"Expense Trends"}
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Monthly expense trends retrieved successfully"),
            @ApiResponse(responseCode = "404", description = "No expense trends found")
    })
    @GetMapping("/monthly-trends/{companyDomain}")
    @SecurityRequirement(name = "Bearer Authentication")
    public ResponseEntity<?> getMonthlyExpenseTrends(
            @PathVariable String companyDomain,
            @RequestParam(required = false) Integer startYear,
            @RequestParam(required = false) Integer endYear,
            @RequestParam(required = false) Integer startMonth,
            @RequestParam(required = false) Integer endMonth) {

        List<ExpenseTrend> monthlyExpenseTrends = this.expenseService.getMonthlyExpenseTrends(companyDomain, startYear, endYear, startMonth, endMonth);
        if (!monthlyExpenseTrends.isEmpty()) {
            return ResponseEntity.status(HttpStatus.OK).body(monthlyExpenseTrends);
        }
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body("No Expense Trends are available.");
    }

    // 📌 2️⃣ Yearly Expense Trends Endpoint
    @Operation(
            summary = "Get yearly expense trends",
            description = "Fetches yearly expense trends based on optional filters for the year.",
            tags = {"Expense Trends"}
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Yearly expense trends retrieved successfully"),
            @ApiResponse(responseCode = "404", description = "No expense trends found")
    })
    @GetMapping("/yearly-trends/{companyDomain}")
    @SecurityRequirement(name = "Bearer Authentication")
    public ResponseEntity<?> getYearlyExpenseTrends(
            @PathVariable String companyDomain,
            @RequestParam(required = false) Integer startYear,
            @RequestParam(required = false) Integer endYear) {

        List<ExpenseTrend> yearlyExpenseTrends = this.expenseService.getYearlyExpenseTrends(companyDomain, startYear, endYear);
        if (!yearlyExpenseTrends.isEmpty()) {
            return ResponseEntity.status(HttpStatus.OK).body(yearlyExpenseTrends);
        }
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body("No Expense Trends are available.");
    }

    // 📌 3️⃣ Expense Category Breakdown Endpoint
    @Operation(
            summary = "Get expense category breakdown",
            description = "Fetches the expense breakdown by category based on optional filters for year, month, and category.",
            tags = {"Expense Breakdown"}
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Expense category breakdown retrieved successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid category provided"),
            @ApiResponse(responseCode = "404", description = "No expense breakdown found for the given criteria")
    })
    @GetMapping("/category-breakdown/{companyDomain}")
    @SecurityRequirement(name = "Bearer Authentication")
    public ResponseEntity<?> getCategoryBreakdown(
            @PathVariable String companyDomain,
            @RequestParam(required = false) Integer startYear,
            @RequestParam(required = false) Integer endYear,
            @RequestParam(required = false) Integer startMonth,
            @RequestParam(required = false) Integer endMonth,
            @RequestParam(required = false) String category) {

        ExpenseCategory categoryEnum = null;
        if (category != null) {
            try {
                categoryEnum = ExpenseCategory.valueOf(category.toUpperCase());
            } catch (IllegalArgumentException e) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Invalid category provided.");
            }
        }
        List<ExpenseCategoryBreakdown> expenseBreakdownByCategory = this.expenseService.getExpenseBreakdownByCategory(companyDomain, startYear, endYear, startMonth, endMonth, categoryEnum);
        if (!expenseBreakdownByCategory.isEmpty()) {
            return ResponseEntity.status(HttpStatus.OK).body(expenseBreakdownByCategory);
        }
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body("No expense breakdown found for the given criteria.");
    }

}
