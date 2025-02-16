package com.madeeasy.service.imipl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.madeeasy.dto.request.CompanyRequestDTO;
import com.madeeasy.dto.request.DomainUpdateRequest;
import com.madeeasy.dto.request.UserRequest;
import com.madeeasy.dto.response.CompanyResponseDTO;
import com.madeeasy.dto.response.UserResponse;
import com.madeeasy.entity.Company;
import com.madeeasy.exception.ClientException;
import com.madeeasy.exception.ResourceNotFoundException;
import com.madeeasy.exception.UnauthorizedAccessException;
import com.madeeasy.repository.CompanyRepository;
import com.madeeasy.service.CompanyService;
import com.madeeasy.util.JwtUtils;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;


@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class CompanyServiceImpl implements CompanyService {

    private final CompanyRepository companyRepository;
    private final RestTemplate restTemplate;
    private final HttpServletRequest httpServletRequest;
    private final JwtUtils jwtUtils;

    @Override
    public CompanyResponseDTO registerCompany(CompanyRequestDTO companyRequestDTO) {
        if (companyRepository.findByDomain(companyRequestDTO.getDomain()).isPresent()) {
            throw new ClientException("Company already exists", HttpStatus.CONFLICT);
        }
        Company company = new Company();
        company.setEmailId(companyRequestDTO.getEmailId());
        company.setName(companyRequestDTO.getName());
        company.setDomain(companyRequestDTO.getDomain());
        company.setAutoApproveThreshold(companyRequestDTO.getAutoApproveThreshold());
        log.info("before saving : {}", company);
        Company savedCompany = this.companyRepository.save(company);
        log.info("after saving : {}", savedCompany);

        // Get the access token from request headers
        String authHeader = httpServletRequest.getHeader(HttpHeaders.AUTHORIZATION);
        String accessToken = authHeader.substring("Bearer ".length());

        String emailId = this.jwtUtils.getUserName(accessToken);

        // Rest call to auth-service to get user details by email
        String authUrlToUpdateUser = "http://auth-service/auth-service/partial-update/" + emailId;
        try {
            UserRequest userRequest = UserRequest.builder()
                    .email(emailId)
                    .companyDomain(companyRequestDTO.getDomain())
                    .build();
            // Create an HttpEntity with the request body (UserRequest) and headers
            HttpEntity<UserRequest> entity = new HttpEntity<>(userRequest, createHeaders(accessToken));

            // Make the PATCH request to update the user
            restTemplate.exchange(authUrlToUpdateUser, HttpMethod.PATCH, entity, UserResponse.class);

        } catch (HttpClientErrorException.Unauthorized e) {
            log.error("Authorization failed: {}", e.getResponseBodyAsString());

            // Get the response body (JSON) from the exception
            String responseBody = e.getResponseBodyAsString();

            // Parse the response body (which is a JSON string)
            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode jsonNode = null;
            try {
                jsonNode = objectMapper.readTree(responseBody);
            } catch (JsonProcessingException ex) {
                log.error("Failed to parse response body: {}", ex.getMessage());
            }

            if (jsonNode != null) {
                String message = jsonNode.get("message").asText();
                String status = jsonNode.get("status").asText();

                HttpStatus httpStatus;
                try {
                    httpStatus = HttpStatus.valueOf(status); // Convert status string to HttpStatus
                } catch (IllegalArgumentException e2) {
                    log.error("Invalid status '{}' found, defaulting to HttpStatus.UNAUTHORIZED", status);
                    httpStatus = HttpStatus.UNAUTHORIZED;
                }

                log.error("Parsed message: {}", message);
                log.error("Parsed status: {}", status);

                // Re-throw ClientException so that it will be caught by the GlobalExceptionHandler
                throw new ClientException(message, httpStatus);
            } else {
                log.error("Failed to parse JSON response body.");
                throw new ClientException("Authorization failed, unable to parse error response", HttpStatus.INTERNAL_SERVER_ERROR);
            }
        } catch (HttpClientErrorException e) {
            log.error("HTTP error occurred: {}", e.getResponseBodyAsString());

            // Get the response body (JSON) from the exception
            String responseBody = e.getResponseBodyAsString();

            // Parse the response body (which is a JSON string)
            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode jsonNode = null;
            try {
                jsonNode = objectMapper.readTree(responseBody);
            } catch (JsonProcessingException ex) {
                log.error("Failed to parse response body: {}", ex.getMessage());
            }

            // Extract message and status
            String message = jsonNode.get("message").asText();
            String status = jsonNode.get("status").asText();

            // Check the status to decide whether it's 401 or 404, etc.
            HttpStatus httpStatus = HttpStatus.valueOf(status); // Convert the status string to HttpStatus

            // Handle 404 NOT_FOUND specifically
            if (httpStatus == HttpStatus.NOT_FOUND) {
                log.error("User not found: {}", message);
                // Here we throw a ClientException with 404 status
                throw new ClientException(message, httpStatus);
            } else if (httpStatus == HttpStatus.UNAUTHORIZED) {
                log.error("Authorization failed: {}", message);
                // Handle unauthorized error as you did earlier
                throw new ClientException(message, httpStatus);
            } else {
                // If it's another error, we might still want to use a ResourceException
                throw new ClientException(message, httpStatus);
            }
        }

        return CompanyResponseDTO.builder()
                .id(savedCompany.getId())
                .emailId(savedCompany.getEmailId())
                .name(savedCompany.getName())
                .domain(savedCompany.getDomain())
                .autoApproveThreshold(savedCompany.getAutoApproveThreshold())
                .build();
    }

    private HttpHeaders createHeaders(String accessToken) {
        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken);  // Ensure 'Bearer' prefix
        headers.set("X-Company-Service", "expense-tracker-app");
        return headers;
    }

    @Override
    public CompanyResponseDTO updateCompany(String domain, CompanyRequestDTO companyRequestDTO) {
        // Fetch the existing company by domain
        Company company = companyRepository.findByDomain(domain)
                .orElseThrow(() -> new ResourceNotFoundException(HttpStatus.NOT_FOUND, "Company not found for the given domain"));

        // Store the old domain to check if it has changed
        String oldDomain = company.getDomain();

        // Update company fields if they are provided and valid
        updateCompanyFields(company, companyRequestDTO);

        // Check if domain has changed and perform the necessary external update if needed
        if (hasDomainChanged(oldDomain, companyRequestDTO.getDomain())) {
            updateUserDomainInAuthService(oldDomain, companyRequestDTO.getDomain());
            company.setDomain(companyRequestDTO.getDomain());
        }

        // Persist the updated company to the database
        company = companyRepository.save(company);

        // Return the updated company information as a response
        return mapToCompanyResponseDTO(company);
    }

    /**
     * Update company fields with provided data from the request DTO.
     */
    private void updateCompanyFields(Company company, CompanyRequestDTO companyRequestDTO) {
        if (companyRequestDTO.getName() != null && !companyRequestDTO.getName().isBlank()) {
            company.setName(companyRequestDTO.getName());
        }
        if (companyRequestDTO.getEmailId() != null && !companyRequestDTO.getEmailId().isBlank()) {
            company.setEmailId(companyRequestDTO.getEmailId());
        }
        if (companyRequestDTO.getAutoApproveThreshold() != null) {
            if (companyRequestDTO.getAutoApproveThreshold() <= 0) {
                throw new IllegalArgumentException("Auto-approve threshold must be a positive value");
            }
            company.setAutoApproveThreshold(companyRequestDTO.getAutoApproveThreshold());
        }
    }

    /**
     * Check if the domain has changed.
     */
    private boolean hasDomainChanged(String oldDomain, String newDomain) {
        return newDomain != null && !newDomain.isBlank() && !oldDomain.equals(newDomain);
    }

    /**
     * Make an external service call to update the user domain in the auth-service if the domain has changed.
     */
    private void updateUserDomainInAuthService(String oldDomain, String newDomain) {
        String accessToken = extractAccessToken();
        String emailId = jwtUtils.getUserName(accessToken);
        String authUrlToUpdateUser = "http://auth-service/auth-service/partial-update/" + emailId;

        try {
            UserRequest userRequest = UserRequest.builder()
                    .email(emailId)
                    .companyDomain(newDomain)
                    .build();

            HttpEntity<UserRequest> entity = new HttpEntity<>(userRequest, createHeaders(accessToken));
            restTemplate.exchange(authUrlToUpdateUser, HttpMethod.PATCH, entity, UserResponse.class);

            updateEmployeeDomainsInAuthService(oldDomain, newDomain, accessToken);
            log.info("Successfully updated user domain to {}", newDomain);

        } catch (HttpClientErrorException e) {
            handleAuthServiceError(e);
        }
    }

    private void updateEmployeeDomainsInAuthService(String oldDomain, String newDomain, String accessToken) {
        String authServiceUrl = "http://auth-service/auth-service/update-domain";  // Update this URL accordingly

        try {
            // Create a request body to update multiple users (for all employees with the old domain)
            DomainUpdateRequest domainUpdateRequest = DomainUpdateRequest.builder()
                    .oldDomain(oldDomain)
                    .newDomain(newDomain)
                    .build();

            HttpEntity<DomainUpdateRequest> entity = new HttpEntity<>(domainUpdateRequest, createHeaders(accessToken));

            // Make an API call to the auth-service to update all users
            restTemplate.exchange(authServiceUrl, HttpMethod.PATCH, entity, Void.class);

            log.info("Successfully updated all employees' domains from {} to {}", oldDomain, newDomain);

        } catch (HttpClientErrorException e) {
            handleAuthServiceError(e);
        }
    }


    /**
     * Extracts the authorization token from the request headers.
     */
    private String extractAccessToken() {
        String authHeader = httpServletRequest.getHeader(HttpHeaders.AUTHORIZATION);
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new UnauthorizedAccessException(HttpStatus.UNAUTHORIZED, "Missing or invalid authorization token");
        }
        return authHeader.substring("Bearer ".length());
    }

    /**
     * Handles errors when calling the auth-service, parsing the response to provide detailed error information.
     */
    private void handleAuthServiceError(HttpClientErrorException e) {
        String responseBody = e.getResponseBodyAsString();
        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode jsonNode;

        try {
            jsonNode = objectMapper.readTree(responseBody);
            String message = jsonNode.get("message").asText();
            String status = jsonNode.get("status").asText();
            HttpStatus httpStatus = HttpStatus.valueOf(status);

            log.error("Auth service error: {}", message);
            throw new ClientException(message, httpStatus);
        } catch (JsonProcessingException ex) {
            log.error("Failed to parse error response from auth-service", ex);
            throw new ClientException("Error processing the response from the auth service", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Maps the company entity to the response DTO.
     */
    private CompanyResponseDTO mapToCompanyResponseDTO(Company company) {
        return CompanyResponseDTO.builder()
                .id(company.getId())
                .emailId(company.getEmailId())
                .name(company.getName())
                .domain(company.getDomain())
                .autoApproveThreshold(company.getAutoApproveThreshold())
                .build();
    }

    @Override
    public CompanyResponseDTO getCompanyByDomainName(String domain) {
        Company foundCompany = this.companyRepository.findByDomain(domain).orElseThrow(() -> new ClientException("Invalid Company Domain or Company Domain Not Found !", HttpStatus.NOT_FOUND));

        return CompanyResponseDTO.builder()
                .id(foundCompany.getId())
                .emailId(foundCompany.getEmailId())
                .name(foundCompany.getName())
                .domain(foundCompany.getDomain())
                .autoApproveThreshold(foundCompany.getAutoApproveThreshold())
                .build();
    }
}
