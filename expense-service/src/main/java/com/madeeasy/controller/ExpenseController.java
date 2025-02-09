package com.madeeasy.controller;

import com.madeeasy.dto.request.ExpensePartialRequestDTO;
import com.madeeasy.dto.request.ExpenseRequestDTO;
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
}
