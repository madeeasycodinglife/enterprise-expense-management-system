package com.madeeasy.service.impl;

import com.madeeasy.dto.request.ExpenseRequestDTO;
import com.madeeasy.entity.Expense;
import com.madeeasy.entity.ExpenseStatus;
import com.madeeasy.repository.ExpenseRepository;
import com.madeeasy.service.ExpenseService;
import com.madeeasy.vo.CompanyResponseDTO;
import com.madeeasy.vo.UserResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;


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
        String accessToken = httpServletRequest.getHeader(HttpHeaders.AUTHORIZATION);

        // Rest call to auth-service to get user details by email
        String authUrlToGetUser = "http://localhost:8081/auth-service/get-user/" + emailId;
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

        Long companyId = companyResponse.getId();

        // Create an Expense entity and set all the required fields
        Expense expense = new Expense();
        expense.setEmployeeId(userId);  // Set employeeId (userId)
        expense.setCompanyId(companyId);  // Set companyId

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

        log.info("Expense submitted successfully by user {} with company id {}", userId, companyId);
    }

    private HttpHeaders createHeaders(String accessToken) {
        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.AUTHORIZATION, accessToken);
        return headers;
    }

}
