package com.madeeasy.service.impl;

import com.itextpdf.text.*;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;
import com.madeeasy.dto.request.ExpensePartialRequestDTO;
import com.madeeasy.dto.request.ExpenseRequestDTO;
import com.madeeasy.dto.response.ExpenseCategoryBreakdown;
import com.madeeasy.dto.response.ExpenseResponseDTO;
import com.madeeasy.dto.response.ExpenseTrend;
import com.madeeasy.entity.Expense;
import com.madeeasy.entity.ExpenseCategory;
import com.madeeasy.entity.ExpenseStatus;
import com.madeeasy.repository.ExpenseRepository;
import com.madeeasy.service.ExpenseService;
import com.madeeasy.vo.ApprovalRequestDTO;
import com.madeeasy.vo.CompanyResponseDTO;
import com.madeeasy.vo.UserResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;


@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class ExpenseServiceImpl implements ExpenseService {

    private final ExpenseRepository expenseRepository;
    private final RestTemplate restTemplate;
    private final HttpServletRequest httpServletRequest;

    @Override
    public void submitExpense(ExpenseRequestDTO expenseRequestDTO) {
        // Get the current authenticated user's email
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String emailId = (String) authentication.getPrincipal();

        // Get the access token from request headers
        String authHeader = httpServletRequest.getHeader(HttpHeaders.AUTHORIZATION);
        String accessToken = authHeader.substring("Bearer ".length());

        // Rest call to auth-service to get user details by email
        String authUrlToGetUser = "http://localhost:8081/auth-service/get-user/" + emailId;
        UserResponse userResponse = restTemplate.exchange(authUrlToGetUser, HttpMethod.GET,
                new HttpEntity<>(createHeaders(accessToken)), UserResponse.class).getBody();
        if (userResponse == null) {
            throw new IllegalStateException("Unable to fetch user information.");
        }

        Long userId = userResponse.getId();

        // Rest call to company-service to get company domain
        String companyUrlToGetCompany = "http://localhost:8082/company-service/domain-name/" + userResponse.getCompanyDomain();
        CompanyResponseDTO companyResponse = restTemplate.exchange(companyUrlToGetCompany, HttpMethod.GET,
                new HttpEntity<>(createHeaders(accessToken)), CompanyResponseDTO.class).getBody();
        if (companyResponse == null) {
            throw new IllegalStateException("Unable to fetch company information.");
        }

        // Define the threshold for auto-approval
        BigDecimal approvalThreshold = new BigDecimal("5000"); // Example: Auto-approve if <= 5000

        // Create an Expense entity and set required fields
        Expense expense = new Expense();
        expense.setEmployeeId(userId);
        expense.setCompanyDomain(companyResponse.getDomain());
        expense.setTitle(expenseRequestDTO.getTitle());
        expense.setDescription(expenseRequestDTO.getDescription());
        expense.setAmount(expenseRequestDTO.getAmount());
        expense.setCategory(expenseRequestDTO.getCategory());
        expense.setExpenseDate(expenseRequestDTO.getExpenseDate());

        if (expenseRequestDTO.getAmount().compareTo(approvalThreshold) <= 0) {
            // Auto-approve expense
            expense.setStatus(ExpenseStatus.APPROVED);
            // Save the expense to the database
            expenseRepository.save(expense);
            log.info("Expense auto-approved: {} by user {}", expenseRequestDTO.getAmount(), userId);
        } else {
            // Requires multi-level approval
            expense.setStatus(ExpenseStatus.SUBMITTED);
            // Save the expense to the database
            expenseRepository.save(expense);
            // Construct the ApprovalRequestDTO object
            ApprovalRequestDTO approvalRequestDTO = ApprovalRequestDTO.builder()
                    .expenseId(expense.getId())
                    .companyDomain(expense.getCompanyDomain())
                    .title(expense.getTitle())
                    .description(expense.getDescription())
                    .amount(expense.getAmount())
                    .category(expense.getCategory())
                    .expenseDate(expense.getExpenseDate())
                    .build();

            // Set up the headers (set content type as JSON)
            HttpHeaders headers = new HttpHeaders();
            headers.set(HttpHeaders.CONTENT_TYPE, String.valueOf(MediaType.APPLICATION_JSON));
            headers.set(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken); // Assuming authorization header is needed

            // Create an HttpEntity with the body and headers
            HttpEntity<ApprovalRequestDTO> requestEntity = new HttpEntity<>(approvalRequestDTO, headers);

            // Make the POST request (assuming the endpoint expects a POST request)
            String approvalUrl = "http://localhost:8085/approval-service/ask-for-approve";
            ResponseEntity<Void> response = restTemplate.exchange(approvalUrl, HttpMethod.POST, requestEntity, Void.class);

            if (response.getStatusCode() == HttpStatus.OK) {
                log.info("Approval request sent successfully.");
            } else {
                log.error("Failed to send approval request: {}", response.getStatusCode());
            }

        }
    }

    @Override
    public ExpenseResponseDTO getExpenseById(Long id) {
        Expense expense = this.expenseRepository.findById(id).orElseThrow(() -> new RuntimeException("Expense not found"));
        return ExpenseResponseDTO.builder()
                .id(expense.getId())
                .employeeId(expense.getEmployeeId())
                .companyDomain(expense.getCompanyDomain())
                .title(expense.getTitle())
                .description(expense.getDescription())
                .amount(expense.getAmount())
                .category(expense.getCategory())
                .expenseDate(expense.getExpenseDate())
                .status(expense.getStatus())
                .build();
    }

    @Override
    public List<ExpenseResponseDTO> getAllExpenses() {
        return this.expenseRepository.findAll()
                .stream().map(expense -> ExpenseResponseDTO.builder()
                        .id(expense.getId())
                        .employeeId(expense.getEmployeeId())
                        .companyDomain(expense.getCompanyDomain())
                        .title(expense.getTitle())
                        .description(expense.getDescription())
                        .amount(expense.getAmount())
                        .category(expense.getCategory())
                        .expenseDate(expense.getExpenseDate())
                        .status(expense.getStatus())
                        .build()
                ).toList();
    }

    @Override
    public ExpenseResponseDTO updateExpense(Long id, ExpensePartialRequestDTO expensePartialRequestDTO) {
        Expense existingExpense = expenseRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Expense not found with ID: " + id));

        if (expensePartialRequestDTO.getTitle() != null && !expensePartialRequestDTO.getTitle().isEmpty()) {
            existingExpense.setTitle(expensePartialRequestDTO.getTitle());
        }
        if (expensePartialRequestDTO.getDescription() != null && !expensePartialRequestDTO.getDescription().isEmpty()) {
            existingExpense.setDescription(expensePartialRequestDTO.getDescription());
        }
        if (expensePartialRequestDTO.getAmount() != null && expensePartialRequestDTO.getAmount().compareTo(BigDecimal.ZERO) > 0) {
            existingExpense.setAmount(expensePartialRequestDTO.getAmount());
        }
        if (expensePartialRequestDTO.getCategory() != null && !expensePartialRequestDTO.getCategory().name().isEmpty()) {
            existingExpense.setCategory(expensePartialRequestDTO.getCategory());
        }
        if (expensePartialRequestDTO.getExpenseDate() != null) {
            existingExpense.setExpenseDate(expensePartialRequestDTO.getExpenseDate());
        }

        Expense savedExpense = expenseRepository.save(existingExpense);
        return ExpenseResponseDTO.builder()
                .id(savedExpense.getId())
                .employeeId(savedExpense.getEmployeeId())
                .companyDomain(savedExpense.getCompanyDomain())
                .title(savedExpense.getTitle())
                .description(savedExpense.getDescription())
                .amount(savedExpense.getAmount())
                .category(savedExpense.getCategory())
                .expenseDate(savedExpense.getExpenseDate())
                .status(savedExpense.getStatus())
                .build();
    }

    @Override
    public void deleteExpense(Long id) {
        this.expenseRepository.deleteById(id);
    }

    @Override
    public List<ExpenseResponseDTO> getExpensesByCategoryAndCompanyDomain(String domainName, String category) {
        // Step 1: Validate and map category to Enum
        ExpenseCategory expenseCategory;
        try {
            expenseCategory = ExpenseCategory.valueOf(category.toUpperCase()); // Convert String to Enum
        } catch (IllegalArgumentException e) {
            // Log and handle invalid category
            throw new RuntimeException("Invalid category: " + category);
        }

        // Step 2: Get company details from company service by domain name
        Long companyId;
        try {
            String companyUrl = "http://localhost:8082/company-service/domain-name/" + domainName;
            ResponseEntity<CompanyResponseDTO> companyResponse = restTemplate.exchange(companyUrl, HttpMethod.GET, null, CompanyResponseDTO.class);

            if (companyResponse.getStatusCode() != HttpStatus.OK || companyResponse.getBody() == null) {
                // Handle cases where the company service call fails or returns no data
                throw new RuntimeException("Failed to retrieve company details for domain: " + domainName);
            }

            companyId = companyResponse.getBody().getId();
        } catch (RestClientException e) {
            // Handle any REST client exceptions (e.g., network issues, 404, etc.)
            throw new RuntimeException("Error calling company service: " + e.getMessage(), e);
        }

        // Step 3: Fetch expenses from the database
        List<Expense> expenseList;
        try {
            expenseList = expenseRepository.findExpensesByCategoryAndCompanyDomain(expenseCategory, domainName);
        } catch (Exception e) {
            // Handle any database-related issues
            throw new RuntimeException("Error retrieving expenses from the database.", e);
        }

        // Step 4: Map expenses to ExpenseResponseDTO
        return expenseList.stream().map(expense -> ExpenseResponseDTO.builder()
                .id(expense.getId())
                .employeeId(expense.getEmployeeId())
                .companyDomain(expense.getCompanyDomain())
                .title(expense.getTitle())
                .description(expense.getDescription())
                .amount(expense.getAmount())
                .category(expense.getCategory())
                .expenseDate(expense.getExpenseDate())
                .status(expense.getStatus())
                .build()
        ).toList();
    }


    @Override
    public byte[] generateExpenseInvoice(String companyDomain, Integer startYear, Integer endYear, Integer startMonth, Integer endMonth, String category) {
        // Convert the category to ExpenseCategory enum if not null
        ExpenseCategory categoryToUse = null;
        if (category != null && !category.isEmpty()) {
            try {
                categoryToUse = ExpenseCategory.valueOf(category.toUpperCase());
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("Invalid category provided");
            }
        }

        // Fetch the expenses with the optional filters (year, month, category)
        List<Expense> expenseList = expenseRepository.findExpensesWithFilters(companyDomain, startYear, endYear, startMonth, endMonth, categoryToUse);

        try {
            Document document = new Document();
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            PdfWriter.getInstance(document, outputStream);
            document.open();

            // **Invoice Header**
            Font titleFont = new Font(FontFactory.getFont(FontFactory.HELVETICA_BOLD).getFamily(), 18, Font.NORMAL, new BaseColor(33, 150, 243)); // Tech-friendly blue color
            Paragraph title = new Paragraph("Expense Invoice", titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            title.setSpacingAfter(20);  // Adding space after the title
            document.add(title);

            // **Generated On Line**
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            Font generatedOnFont = new Font(FontFactory.getFont(FontFactory.HELVETICA).getFamily(), 12, Font.NORMAL, new BaseColor(0, 150, 136)); // Tech green color
            Paragraph generatedOn = new Paragraph("Generated on: " + LocalDateTime.now().format(formatter), generatedOnFont);
            generatedOn.setAlignment(Element.ALIGN_LEFT);
            generatedOn.setSpacingAfter(20);  // Adding space after the "Generated on"
            document.add(generatedOn);

            // **Create Table for Expenses**
            PdfPTable table = new PdfPTable(6);
            table.setWidthPercentage(100);
            table.setSpacingBefore(10f);
            table.setSpacingAfter(10f);

            // **Define Column Headers**
            String[] headers = {"Expense ID", "Title", "Description", "Amount ($)", "Category", "Date"};
            for (String header : headers) {
                PdfPCell headerCell = new PdfPCell(new Phrase(header));
                headerCell.setBackgroundColor(new BaseColor(184, 218, 255));  // Light blue header color
                headerCell.setPadding(5);
                table.addCell(headerCell);
            }

            // **Insert Data Rows with alternating row colors**
            boolean isEvenRow = true;
            for (Expense expense : expenseList) {
                BaseColor rowColor = isEvenRow ? new BaseColor(245, 245, 245) : new BaseColor(255, 255, 255);  // Alternating row colors

                // Set row color
                table.addCell(createStyledCell(String.valueOf(expense.getId()), rowColor));
                table.addCell(createStyledCell(expense.getTitle(), rowColor));
                table.addCell(createStyledCell(expense.getDescription(), rowColor));
                table.addCell(createStyledCell("$" + expense.getAmount(), rowColor));
                table.addCell(createStyledCell(expense.getCategory().toString(), rowColor));
                table.addCell(createStyledCell(expense.getExpenseDate().toString(), rowColor));

                // Toggle row color for next row
                isEvenRow = !isEvenRow;
            }

            document.add(table);
            document.close();

            return outputStream.toByteArray();
        } catch (Exception e) {
            log.error("Error generating invoice PDF", e);
            throw new RuntimeException("Failed to generate invoice", e);
        }
    }

    // Helper method to create a styled cell
    private PdfPCell createStyledCell(String content, BaseColor rowColor) {
        PdfPCell cell = new PdfPCell(new Phrase(content));
        cell.setBackgroundColor(rowColor);
        cell.setPadding(5);
        return cell;
    }


    @Override
    public List<ExpenseTrend> getMonthlyExpenseTrends(
            String companyDomain, Integer startYear, Integer endYear, Integer startMonth, Integer endMonth) {

        List<Object[]> results = expenseRepository.findMonthlyExpenseTrendsByCompanyDomain(
                companyDomain, startYear, endYear, startMonth, endMonth);

        List<ExpenseTrend> trends = new ArrayList<>();

        for (Object[] result : results) {
            int year = ((Number) result[0]).intValue();
            int month = ((Number) result[1]).intValue();
            BigDecimal totalAmount = (BigDecimal) result[2];

            trends.add(ExpenseTrend.builder().month(month).year(year).totalAmount(totalAmount).build());
        }

        return trends;
    }

    @Override
    public List<ExpenseTrend> getYearlyExpenseTrends(String companyDomain, Integer startYear, Integer endYear) {
        List<Object[]> results = expenseRepository.findYearlyExpenseTrendsByCompanyDomain(
                companyDomain, startYear, endYear);

        List<ExpenseTrend> trends = new ArrayList<>();

        for (Object[] result : results) {
            int year = ((Number) result[0]).intValue();
            BigDecimal totalAmount = (BigDecimal) result[1];
            trends.add(ExpenseTrend.builder().year(year).totalAmount(totalAmount).build());
        }

        return trends;
    }

    private HttpHeaders createHeaders(String accessToken) {
        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken);  // Ensure 'Bearer' prefix
        return headers;
    }

    // Method to get expense breakdown by category with optional filters for year and month
    @Override
    public List<ExpenseCategoryBreakdown> getExpenseBreakdownByCategory(
            String companyDomain, Integer startYear, Integer endYear, Integer startMonth, Integer endMonth, ExpenseCategory category) {

        // Fetch the data based on filters from the repository
        List<Object[]> results = expenseRepository.findExpenseCategoryBreakdown(
                companyDomain, startYear, endYear, startMonth, endMonth, category);

        List<ExpenseCategoryBreakdown> breakdown = new ArrayList<>();

        for (Object[] result : results) {
            int month = ((Number) result[0]).intValue();
            int year = ((Number) result[1]).intValue();
            ExpenseCategory categoryName = (ExpenseCategory) result[2];
            BigDecimal totalAmount = (BigDecimal) result[3];

            // Add the result to the breakdown
            breakdown.add(ExpenseCategoryBreakdown.builder().month(month).year(year).category(categoryName).totalAmount(totalAmount).build());
        }

        return breakdown;
    }
}
