package com.madeeasy.service.impl;

import com.itextpdf.text.*;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;
import com.madeeasy.dto.request.ExpensePartialRequestDTO;
import com.madeeasy.dto.request.ExpenseRequestDTO;
import com.madeeasy.dto.response.ExpenseResponseDTO;
import com.madeeasy.entity.Expense;
import com.madeeasy.entity.ExpenseCategory;
import com.madeeasy.entity.ExpenseStatus;
import com.madeeasy.repository.ExpenseRepository;
import com.madeeasy.service.ExpenseService;
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
        System.out.println("authentication = " + authentication);
        String emailId = (String) authentication.getPrincipal();

        // Get the access token from request headers
        String authHeader = httpServletRequest.getHeader(HttpHeaders.AUTHORIZATION);
        String accessToken = authHeader.substring("Bearer ".length());
        // Rest call to auth-service to get user details by email
        String authUrlToGetUser = "http://localhost:8081/auth-service/get-user/" + emailId;
        System.out.println("authUrlToGetUser = " + authUrlToGetUser);
        System.out.println("accessToken: " + accessToken);
        UserResponse userResponse = restTemplate.exchange(authUrlToGetUser, HttpMethod.GET,
                new HttpEntity<>(createHeaders(accessToken)), UserResponse.class).getBody();

        if (userResponse == null) {
            throw new IllegalStateException("Unable to fetch user information.");
        }

        Long userId = userResponse.getId();

        // Rest call to company-service to get company id
        String companyUrlToGetCompany = "http://localhost:8082/company-service/domain-name/" + userResponse.getCompanyDomain();
        CompanyResponseDTO companyResponse = restTemplate.exchange(companyUrlToGetCompany, HttpMethod.GET,
                new HttpEntity<>(createHeaders(accessToken)), CompanyResponseDTO.class).getBody();

        if (companyResponse == null) {
            throw new IllegalStateException("Unable to fetch company information.");
        }


        // Create an Expense entity and set all the required fields
        Expense expense = new Expense();
        expense.setEmployeeId(userId);  // Set employeeId (userId)
        expense.setCompanyDomain(companyResponse.getDomain());  // Set companyId

        // Set other fields from ExpenseRequestDTO
        expense.setTitle(expenseRequestDTO.getTitle());  // Set title
        expense.setDescription(expenseRequestDTO.getDescription());  // Set description
        expense.setAmount(expenseRequestDTO.getAmount());  // Set amount
        expense.setCategory(expenseRequestDTO.getCategory());  // Set category
        expense.setExpenseDate(expenseRequestDTO.getExpenseDate());  // Set expense date

        // Set default status to 'SUBMITTED' if not already set in the DTO (status will default if not set)
        expense.setStatus(ExpenseStatus.SUBMITTED);  // Ensure the status is set if not already done

        // Save the expense to the database
        expenseRepository.save(expense);

        log.info("Expense submitted successfully by user {} with company domain {}", userId, companyResponse.getDomain());
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
    public byte[] generateExpenseInvoice(String companyDomain) {
        List<Expense> expenseList = expenseRepository.findByCompanyDomain(companyDomain);

        try {
            Document document = new Document();
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            PdfWriter.getInstance(document, outputStream);
            document.open();

            // **Invoice Header**
            Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18);
            Paragraph title = new Paragraph("Expense Invoice", titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            document.add(title);
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            document.add(new Paragraph("Generated on: " + LocalDateTime.now().format(formatter)));
            document.add(new Paragraph("\n"));

            // **Create Table for Expenses**
            PdfPTable table = new PdfPTable(6);
            table.setWidthPercentage(100);
            table.setSpacingBefore(10f);
            table.setSpacingAfter(10f);

            // **Define Column Headers**
            String[] headers = {"Expense ID", "Title", "Description", "Amount ($)", "Category", "Date"};
            for (String header : headers) {
                PdfPCell headerCell = new PdfPCell(new Phrase(header));
                headerCell.setBackgroundColor(new BaseColor(184, 218, 255));
                headerCell.setPadding(5);
                table.addCell(headerCell);
            }

            // **Insert Data Rows**
            for (Expense expense : expenseList) {
                table.addCell(String.valueOf(expense.getId()));
                table.addCell(expense.getTitle());
                table.addCell(expense.getDescription());
                table.addCell("$" + expense.getAmount());
                table.addCell(expense.getCategory().toString());
                table.addCell(expense.getExpenseDate().toString());
            }

            document.add(table);
            document.close();

            return outputStream.toByteArray();
        } catch (Exception e) {
            log.error("Error generating invoice PDF", e);
            throw new RuntimeException("Failed to generate invoice", e);
        }
    }

    private HttpHeaders createHeaders(String accessToken) {
        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken);  // Ensure 'Bearer' prefix
        return headers;
    }

}
