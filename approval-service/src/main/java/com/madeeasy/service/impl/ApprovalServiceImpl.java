package com.madeeasy.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.madeeasy.dto.request.ExpenseRequestDTO;
import com.madeeasy.dto.response.UserResponse;
import com.madeeasy.entity.Approval;
import com.madeeasy.entity.ApprovalStatus;
import com.madeeasy.exception.ClientException;
import com.madeeasy.exception.ResourceException;
import com.madeeasy.repository.ApprovalRepository;
import com.madeeasy.service.ApprovalService;
import com.madeeasy.util.JwtUtils;
import com.madeeasy.vo.CompanyResponseDTO;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;

import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class ApprovalServiceImpl implements ApprovalService {

    private final ApprovalRepository approvalRepository;
    private final RestTemplate restTemplate;
    private final RestTemplate localRestTemplate;
    private final JwtUtils jwtUtils;
    private final HttpServletRequest httpServletRequest;
    private final DiscoveryClient discoveryClient;

    //    @Retry(name = "approvalServiceRetry", fallbackMethod = "retryFallback")
//    @CircuitBreaker(name = "approvalServiceCircuitBreaker", fallbackMethod = "circuitBreakerFallback")
//    @Override
//    public void askForApproval(ExpenseRequestDTO expenseRequestDTO) throws UnsupportedEncodingException {
//        String authHeader = httpServletRequest.getHeader(HttpHeaders.AUTHORIZATION);
//        String accessToken = authHeader.substring("Bearer ".length());
//
//        try {
//            // rest call with authorization herader to get user details by company domain and role
//            String authUrlToGetUser = "http://localhost:8081/auth-service/get-user/" + expenseRequestDTO.getCompanyDomain() + "/" + "MANAGER";
//            List<UserResponse> userResponseList = restTemplate.exchange(authUrlToGetUser, HttpMethod.GET,
//                    new HttpEntity<>(createHeaders(accessToken)), new ParameterizedTypeReference<List<UserResponse>>() {
//                    }).getBody();
//            // Check if the response is null or empty
//            if (userResponseList == null || userResponseList.isEmpty()) {
//                throw new IllegalStateException("Unable to fetch user information.");
//            }
//
//            // Assuming the list contains only one user for the given role, get the first user
//            UserResponse userResponse = userResponseList.getFirst();  // Get the first element from the list
//            String managerEmail = userResponse.getEmail();  // Access the email property of the first user
//
//
//            // Prepare the expense details as query parameters
//            String expenseDetails = "expenseId=" + URLEncoder.encode(String.valueOf(expenseRequestDTO.getExpenseId()), StandardCharsets.UTF_8) +
//                    "&title=" + URLEncoder.encode(expenseRequestDTO.getTitle(), StandardCharsets.UTF_8) +
//                    "&description=" + URLEncoder.encode(expenseRequestDTO.getDescription(), StandardCharsets.UTF_8) +
//                    "&amount=" + URLEncoder.encode(String.valueOf(expenseRequestDTO.getAmount()), StandardCharsets.UTF_8) +
//                    "&category=" + URLEncoder.encode(String.valueOf(expenseRequestDTO.getCategory()), StandardCharsets.UTF_8) +
//                    "&expenseDate=" + URLEncoder.encode(expenseRequestDTO.getExpenseDate().toString(), StandardCharsets.UTF_8) +
//                    "&accessToken=" + URLEncoder.encode(accessToken, StandardCharsets.UTF_8) +
//                    "&emailId=" + URLEncoder.encode(managerEmail, StandardCharsets.UTF_8);// in future call to auth-service and by company domain get manager emailId and set here
//
//            // Send email to Manager for approval
//            String approveLink = "http://localhost:8085/approval-service/approve?" + expenseDetails + "&emailId=" + managerEmail + "&role=MANAGER";
//            String rejectLink = "http://localhost:8085/approval-service/reject?" + expenseDetails + "&emailId=" + managerEmail + "&role=MANAGER";
//
//            // Create the request body, including expense details and links
//            Map<String, Object> requestBody = new HashMap<>();
//            requestBody.put("expenseDetails", expenseDetails);  // Sending full expense details
//            requestBody.put("approveLink", approveLink);        // Approval link
//            requestBody.put("rejectLink", rejectLink);          // Rejection link
//
//            // Rest call to notification-services to send email
//            String notificationUrl = "http://localhost:8084/notification-service/";
//            restTemplate.postForObject(notificationUrl, requestBody, Void.class);
//
//            // Save the approval request in the database (Approval table)
//            Approval approval = Approval.builder()
//                    .expenseId(expenseRequestDTO.getExpenseId())
//                    .companyDomain(expenseRequestDTO.getCompanyDomain())
//                    .approverRole("MANAGER")
//                    .approvedBy(managerEmail)
//                    .status(ApprovalStatus.PENDING)
//                    .build();
//            this.approvalRepository.save(approval);
//        } catch (HttpClientErrorException.Unauthorized e) {
//            log.error("Authorization failed: {}", e.getResponseBodyAsString());
//// Get the response body (JSON) from the exception
//            String responseBody = e.getResponseBodyAsString();
//
//// Parse the response body (which is a JSON string)
//            ObjectMapper objectMapper = new ObjectMapper();
//            JsonNode jsonNode = null;
//            try {
//                jsonNode = objectMapper.readTree(responseBody);
//            } catch (JsonProcessingException ex) {
//                // Log the parsing error
//                log.error("Failed to parse response body: {}", ex.getMessage());
//                // Optionally, throw an error here if parsing is crucial
//            }
//
//// Ensure that jsonNode is not null and extract message and status
//            if (jsonNode != null) {
//                String message = jsonNode.get("message").asText();
//                String status = jsonNode.get("status").asText();
//
//                // Map status to HttpStatus enum (handle the case where the status is not directly a valid enum)
//                HttpStatus httpStatus;
//                try {
//                    httpStatus = HttpStatus.valueOf(status); // Convert the status string to HttpStatus
//                } catch (IllegalArgumentException e2) {
//                    // If status doesn't match a valid HttpStatus, default to UNAUTHORIZED
//                    log.error("Invalid status '{}' found, defaulting to HttpStatus.UNAUTHORIZED", status);
//                    httpStatus = HttpStatus.UNAUTHORIZED;
//                }
//
//                // Log the message and status for better visibility
//                log.error("Parsed message: {}", message);
//                log.error("Parsed status: {}", status);
//
//                // Throw a custom exception with the parsed message and status
//                throw new ClientException(message, httpStatus);
//            } else {
//                // If jsonNode is null, handle the error
//                log.error("Failed to parse JSON response body.");
//                throw new ClientException("Authorization failed, unable to parse error response", HttpStatus.INTERNAL_SERVER_ERROR);
//            }
//
//        } catch (HttpClientErrorException e) {
//            log.error("HTTP error occurred: {}", e.getResponseBodyAsString());
//            throw new ResourceException("An error occurred while accessing auth-service.");
//        }
//    }
//
    @Retry(name = "approvalServiceRetry", fallbackMethod = "retryFallback")
    @CircuitBreaker(name = "approvalServiceCircuitBreaker", fallbackMethod = "circuitBreakerFallback")
    @Override
    public void askForApproval(ExpenseRequestDTO expenseRequestDTO) throws UnsupportedEncodingException {


        String authHeader = httpServletRequest.getHeader(HttpHeaders.AUTHORIZATION);
        String accessToken = authHeader.substring("Bearer ".length());

        try {
            // Rest call with authorization header to get user details by company domain and role
            String authUrlToGetUser = "http://auth-service/auth-service/get-user/" + expenseRequestDTO.getCompanyDomain() + "/" + "MANAGER";
            List<UserResponse> userResponseList = restTemplate.exchange(authUrlToGetUser, HttpMethod.GET,
                    new HttpEntity<>(createHeaders(accessToken)), new ParameterizedTypeReference<List<UserResponse>>() {
                    }).getBody();

            // Check if the response is null or empty
            if (userResponseList == null || userResponseList.isEmpty()) {
                throw new IllegalStateException("Unable to fetch user information.");
            }

            // Assuming the list contains only one user for the given role, get the first user
            UserResponse userResponse = userResponseList.getFirst();  // Get the first element from the list
            String managerEmail = userResponse.getEmail();  // Access the email property of the first user


            // call first company service and get approval threshold start


            // Rest call to company-service to get company domain
            String companyUrlToGetCompany = "http://company-service/company-service/domain-name/" + userResponse.getCompanyDomain();
            CompanyResponseDTO companyResponse = null;
            try {
                companyResponse = restTemplate.exchange(companyUrlToGetCompany, HttpMethod.GET,
                        new HttpEntity<>(createHeaders(accessToken)), CompanyResponseDTO.class).getBody();
            } catch (HttpClientErrorException | HttpServerErrorException e) {
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
                    String statusStr = jsonNode.get("status").asText().substring(0, 3);

                    // Convert the status code to integer and map it to HttpStatus
                    int statusCode = Integer.parseInt(statusStr);

                    // Use HttpStatus.valueOf() with the numeric status code value
                    HttpStatus status = HttpStatus.resolve(statusCode);

                    log.error("Parsed message: {}", message);
                    log.error("Parsed status: {}", status);

                    // Re-throw ClientException so that it will be caught by the GlobalExceptionHandler
                    throw new ClientException(message, status);
                } else {
                    log.error("Failed to parse JSON response body.");
                    throw new ClientException("Authorization failed, unable to parse error response", HttpStatus.INTERNAL_SERVER_ERROR);
                }
            }

            if (companyResponse == null) {
                throw new IllegalStateException("Unable to fetch company information.");
            }
            if (expenseRequestDTO.getAmount().compareTo(BigDecimal.valueOf(companyResponse.getAutoApproveThreshold())) <= 0) {
                String emailId = jwtUtils.getUserName(accessToken);
                // Auto-approve expense
                Approval approval = Approval.builder()
                        .expenseId(expenseRequestDTO.getExpenseId())
                        .companyDomain(expenseRequestDTO.getCompanyDomain())
                        .approverRole("AUTO_APPROVER")
                        .approvedBy(emailId)
                        .status(ApprovalStatus.APPROVED)
                        .approvalInitiationDate(LocalDateTime.now())
                        .approvalCompletionDate(LocalDateTime.now())
                        .build();
                // Save the expense to the database
                this.approvalRepository.save(approval);
                log.info("Expense auto-approved: {} by user {}", expenseRequestDTO.getAmount(), userResponse.getEmail());
            } else {
                // Prepare the expense details as query parameters
                String expenseDetails = "expenseId=" + URLEncoder.encode(String.valueOf(expenseRequestDTO.getExpenseId()), StandardCharsets.UTF_8) +
                        "&title=" + URLEncoder.encode(expenseRequestDTO.getTitle(), StandardCharsets.UTF_8) +
                        "&description=" + URLEncoder.encode(expenseRequestDTO.getDescription(), StandardCharsets.UTF_8) +
                        "&amount=" + URLEncoder.encode(String.valueOf(expenseRequestDTO.getAmount()), StandardCharsets.UTF_8) +
                        "&category=" + URLEncoder.encode(String.valueOf(expenseRequestDTO.getCategory()), StandardCharsets.UTF_8) +
                        "&expenseDate=" + URLEncoder.encode(expenseRequestDTO.getExpenseDate().toString(), StandardCharsets.UTF_8) +
                        "&accessToken=" + URLEncoder.encode(accessToken, StandardCharsets.UTF_8) +
                        "&emailId=" + URLEncoder.encode(managerEmail, StandardCharsets.UTF_8);// in future call to auth-service and by company domain get manager emailId and set here
                // Get healthy approval service URLs
                List<String> healthyServiceUrls = getApprovalServiceUrls();

                if (healthyServiceUrls.isEmpty()) {
                    // Handle error (e.g., no healthy instances available)
                    return;
                }

                // Pick a random URL from the healthy services list
                String approvalServiceUrl = healthyServiceUrls.getFirst();  // You can add more logic for load balancing if needed


                // Send email to Manager for approval
//            String approveLink = "http://approval-service/approval-service/approve?" + expenseDetails + "&emailId=" + managerEmail + "&role=MANAGER";
//            String rejectLink = "http://approval-service/approval-service/reject?" + expenseDetails + "&emailId=" + managerEmail + "&role=MANAGER";
                // Construct the approval and rejection links
                String approveLink = "http://" + approvalServiceUrl + "/approval-service/approve?" + expenseDetails + "&emailId=" + managerEmail + "&role=MANAGER";
                String rejectLink = "http://" + approvalServiceUrl + "/approval-service/reject?" + expenseDetails + "&emailId=" + managerEmail + "&role=MANAGER";


                // Create the request body, including expense details and links
                Map<String, Object> requestBody = new HashMap<>();
                requestBody.put("expenseDetails", expenseDetails);  // Sending full expense details
                requestBody.put("approveLink", approveLink);        // Approval link
                requestBody.put("rejectLink", rejectLink);          // Rejection link

                // Rest call to notification-services to send email
                String notificationUrl = "http://notification-service/notification-service/";
                try {
                    // Prepare headers
                    HttpHeaders headers = new HttpHeaders();
                    headers.set("Authorization", "Bearer " + accessToken);  // Add the accessToken as Bearer token

                    // Your logic to call the notification service
                    // Wrap the body and headers into an HttpEntity
                    HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

                    restTemplate.exchange(notificationUrl, HttpMethod.POST, entity, Void.class);
                } catch (HttpClientErrorException | HttpServerErrorException e) {
                    // Log the error details
                    log.error("Error while calling notification service: {}", e.getMessage());

                    // Check if the status is 503 (Service Unavailable) and handle it
                    if (e.getStatusCode() == HttpStatus.SERVICE_UNAVAILABLE) {
                        // Return a more specific error response
                        throw new ResourceException("Notification service is currently unavailable. Please try again later.");
                    }

                    // If it’s some other type of error, you can throw a different exception or handle accordingly
                    throw new ResourceException("An error occurred while accessing the notification service.");
                }


                // Save the approval request in the database (Approval table)
                Approval approval = Approval.builder()
                        .expenseId(expenseRequestDTO.getExpenseId())
                        .companyDomain(expenseRequestDTO.getCompanyDomain())
                        .approverRole("MANAGER")
                        .approvedBy(managerEmail)
                        .status(ApprovalStatus.PENDING)
                        .approvalInitiationDate(LocalDateTime.now())
                        .build();
                this.approvalRepository.save(approval);
            }
            // call first company service and get approval threshold end

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
                throw new ClientException("Manager has not been created.  Please check before Proceeding", httpStatus);
            } else if (httpStatus == HttpStatus.UNAUTHORIZED) {
                log.error("Authorization failed: {}", message);
                // Handle unauthorized error as you did earlier
                throw new ClientException(message, httpStatus);
            } else {
                // If it's another error, we might still want to use a ResourceException
                throw new ResourceException("An error occurred while accessing auth-service.");
            }
        }

    }


    // Method to check if a service is healthy
    private boolean isServiceHealthy(String host, int port) {
        try {
            // Send a GET request to the health check endpoint of the service
            String healthCheckUrl = "http://" + host + ":" + port + "/actuator/health"; // Assuming Actuator is used for health check
            ResponseEntity<String> response = this.localRestTemplate.exchange(healthCheckUrl, HttpMethod.GET, null, String.class);

            // If status code is 200 OK, the service is healthy
            return response.getStatusCode() == HttpStatus.OK;
        } catch (Exception e) {
            // In case of any error (e.g., service is down), consider the service unhealthy
            return false;
        }
    }

    // Method to get the approval service URLs with health checks
    public List<String> getApprovalServiceUrls() {
        // Get all instances of 'approval-service' registered in Eureka
        List<ServiceInstance> serviceInstances = discoveryClient.getInstances("approval-service");

        // Create a list to store the URLs in the format 'localhost:port'
        List<String> serviceUrls = new ArrayList<>();

        // Iterate over the instances and extract the host and port
        for (ServiceInstance instance : serviceInstances) {
            String host = instance.getHost();  // Get the IP address or hostname of the instance
            int port = instance.getPort();       // Get the port number of the instance

            if (isServiceHealthy(host, port)) {
                // Build the URL in the format 'localhost:port'
                String serviceUrl = host + ":" + port;
                serviceUrls.add(serviceUrl);  // Add the URL to the list
                log.info("host: {}, port: {}", host, port);
            }
        }

        return serviceUrls;  // Return only healthy services
    }


    // Fallback method for retry
    public void retryFallback(ExpenseRequestDTO expenseRequestDTO, UnsupportedEncodingException e) {
        log.error("Retry failed after multiple attempts for askForApproval: {}", e.getMessage());

        // If the cause of the failure is a ClientException, rethrow it
        if (e.getCause() instanceof ClientException) {
            throw (ClientException) e.getCause();  // Propagate the original ClientException
        }

        throw new ResourceException("Failed to process after retries. Please try again later.");
    }


    // Fallback method for circuit breaker with specific service failure messages
    public void circuitBreakerFallback(ExpenseRequestDTO expenseRequestDTO, Throwable t) {
        log.error("Circuit breaker triggered: {}", t.getMessage());

        // If the cause of the failure is a ClientException, rethrow it
        if (t instanceof ClientException) {
            throw (ClientException) t;  // Propagate the original ClientException
        }

        // If the failure is related to a service, handle accordingly
        String serviceName = getFailedServiceName(t);
        String errorMessage = String.format("Service '%s' is currently unavailable. Please try again later.", serviceName);

        log.error("Circuit breaker triggered: {}", errorMessage);
        throw new ResourceException(errorMessage);
    }

    // Helper method to determine the service that failed
    private String getFailedServiceName(Throwable t) {
        if (t.getMessage().contains("auth-service")) {
            return "auth-service";
        } else if (t.getMessage().contains("notification-service")) {
            return "notification-service";
        } else {
            return "Unknown service";
        }
    }

    private HttpHeaders createHeaders(String accessToken) {
        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken);
        return headers;
    }

    @Override
    public void approveExpenseFromEmail(Long expenseId,
                                        String title,
                                        String description,
                                        BigDecimal amount,
                                        String category,
                                        String expenseDate,
                                        String accessToken,
                                        String emailId,
                                        String role) {
        List<Approval> approval = approvalRepository.findByExpenseId(expenseId);
        if (approval.isEmpty()) {
            throw new RuntimeException("No pending approval found for this expense.");
        }
        Approval currentApproval = approval.stream()
                .filter(a -> a.getApproverRole().equals(role) && a.getStatus() == ApprovalStatus.PENDING)
                .findFirst()
                .orElseThrow(() -> new RuntimeException("No pending approval found for this expense for this role."));
        /**
         * in future call auth-service and there by company domain get finance/admin emailId and make the below dynamic email
         */
        if (role.equals("MANAGER")) {
            currentApproval.setStatus(ApprovalStatus.APPROVED);
            currentApproval.setApprovalCompletionDate(LocalDateTime.now());
            this.approvalRepository.save(currentApproval);
            // rest call with authorization herader to get user details by company domain and role
            String authUrlToGetUser = "http://auth-service/auth-service/get-user/" + currentApproval.getCompanyDomain() + "/" + "FINANCE";
            String financeEmail = null;  // Access the email property of the first user
            try {
                List<UserResponse> userResponseList = restTemplate.exchange(authUrlToGetUser, HttpMethod.GET,
                        new HttpEntity<>(createHeaders(accessToken)), new ParameterizedTypeReference<List<UserResponse>>() {
                        }).getBody();
                // Check if the response is null or empty
                if (userResponseList == null || userResponseList.isEmpty()) {
                    throw new IllegalStateException("Unable to fetch user information.");
                }

                // Assuming the list contains only one user for the given role, get the first user
                UserResponse userResponse = userResponseList.getFirst();  // Get the first element from the list
                financeEmail = userResponse.getEmail();
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
                    throw new ClientException("Finance has not been created , Please check before Proceeding", httpStatus);
                } else if (httpStatus == HttpStatus.UNAUTHORIZED) {
                    log.error("Authorization failed: {}", message);
                    // Handle unauthorized error as you did earlier
                    throw new ClientException(message, httpStatus);
                } else {
                    // If it's another error, we might still want to use a ResourceException
                    throw new ResourceException("An error occurred while accessing auth-service.");
                }
            }

            // Save the approval request in the database (Approval table)
            Approval finaceApproval = Approval.builder()
                    .expenseId(currentApproval.getExpenseId())
                    .companyDomain(currentApproval.getCompanyDomain())
                    .approverRole("FINANCE")
                    .approvedBy(financeEmail)
                    .status(ApprovalStatus.PENDING)
                    .approvalInitiationDate(LocalDateTime.now())
                    .build();
            this.approvalRepository.save(finaceApproval);
            sendNextApproval(expenseId, title, description, amount, category, expenseDate, accessToken, financeEmail, "FINANCE");
        } else if (role.equals("FINANCE")) {
            currentApproval.setStatus(ApprovalStatus.APPROVED);
            currentApproval.setApprovalCompletionDate(LocalDateTime.now());
            this.approvalRepository.save(currentApproval);
            // rest call with authorization herader to get user details by company domain and role
            String authUrlToGetUser = "http://auth-service/auth-service/get-user/" + currentApproval.getCompanyDomain() + "/" + "ADMIN";
            String adminEmail = null;  // Access the email property of the first user
            try {
                List<UserResponse> userResponseList = restTemplate.exchange(authUrlToGetUser, HttpMethod.GET,
                        new HttpEntity<>(createHeaders(accessToken)), new ParameterizedTypeReference<List<UserResponse>>() {
                        }).getBody();
                // Check if the response is null or empty
                if (userResponseList == null || userResponseList.isEmpty()) {
                    throw new IllegalStateException("Unable to fetch user information.");
                }

                // Assuming the list contains only one user for the given role, get the first user
                UserResponse userResponse = userResponseList.getFirst();  // Get the first element from the list
                adminEmail = userResponse.getEmail();
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
                    throw new ClientException("Admin has not been created.  Please check before Proceeding", httpStatus);
                } else if (httpStatus == HttpStatus.UNAUTHORIZED) {
                    log.error("Authorization failed: {}", message);
                    // Handle unauthorized error as you did earlier
                    throw new ClientException(message, httpStatus);
                } else {
                    // If it's another error, we might still want to use a ResourceException
                    throw new ResourceException("An error occurred while accessing auth-service.");
                }
            }
            // Save the approval request in the database (Approval table)
            Approval finaceApproval = Approval.builder()
                    .expenseId(currentApproval.getExpenseId())
                    .companyDomain(currentApproval.getCompanyDomain())
                    .approverRole("ADMIN")
                    .approvedBy(adminEmail)
                    .status(ApprovalStatus.PENDING)
                    .approvalInitiationDate(LocalDateTime.now())
                    .build();
            this.approvalRepository.save(finaceApproval);
            // Approve and send to Admin
            sendNextApproval(expenseId, title, description, amount, category, expenseDate, accessToken, adminEmail, "ADMIN");
        } else if (role.equals("ADMIN")) {
            // Final approval
            currentApproval.setStatus(ApprovalStatus.APPROVED);
            currentApproval.setApprovalCompletionDate(LocalDateTime.now());
            approvalRepository.save(currentApproval);
        }
    }


    public void sendNextApproval(Long expenseId,
                                 String title,
                                 String description,
                                 BigDecimal amount,
                                 String category,
                                 String expenseDate,
                                 String accessToken,
                                 String emailId,
                                 String role) {

        // Prepare the expense details as query parameters

        String expenseDetails = "";
        try {
            expenseDetails = "expenseId=" + URLEncoder.encode(String.valueOf(expenseId), StandardCharsets.UTF_8) +
                    "&amount=" + URLEncoder.encode(amount.toString(), StandardCharsets.UTF_8) +
                    "&category=" + URLEncoder.encode(category, StandardCharsets.UTF_8) +
                    "&description=" + URLEncoder.encode(description, StandardCharsets.UTF_8) +
                    "&title=" + URLEncoder.encode(title, StandardCharsets.UTF_8) +
                    "&expenseDate=" + URLEncoder.encode(expenseDate, StandardCharsets.UTF_8) +
                    "&accessToken=" + URLEncoder.encode(accessToken, StandardCharsets.UTF_8) +
                    "&emailId=" + URLEncoder.encode(emailId, StandardCharsets.UTF_8);
        } catch (Exception e) {
            System.out.println(e.getMessage());

        }

        System.out.println("Encoded expense details: " + expenseDetails);

        // Get healthy approval service URLs
        List<String> healthyServiceUrls = getApprovalServiceUrls();

        if (healthyServiceUrls.isEmpty()) {
            // Handle error (e.g., no healthy instances available)
            return;
        }

        // Pick a random URL from the healthy services list
        String approvalServiceUrl = healthyServiceUrls.getFirst();  // You can add more logic for load balancing if needed


        // Prepare the approval/rejection links with the expense details and the role for the next approver
        String approveLink = "http://" + approvalServiceUrl + "/approval-service/approve?" + expenseDetails + "&emailId=" + emailId + "&role=" + role;
        String rejectLink = "http://" + approvalServiceUrl + "/approval-service/reject?" + expenseDetails + "&emailId=" + emailId + "&role=" + role;

        System.out.println("Inside sendNextApproval method !!");
        System.out.println("approveLink " + approveLink + " rejectLink " + rejectLink);
        // Create the request body, including expense details and links
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("expenseDetails", expenseDetails);  // Sending full expense details
        requestBody.put("approveLink", approveLink);        // Approval link
        requestBody.put("rejectLink", rejectLink);          // Rejection link

        System.out.println("Ready to sent another notification");
        // Send the notification to the next approver
        String notificationUrl = "http://notification-service/notification-service/";
        try {
            // Prepare headers
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + accessToken);  // Add the accessToken as Bearer token

            // Your logic to call the notification service
            // Wrap the body and headers into an HttpEntity
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

            restTemplate.exchange(notificationUrl, HttpMethod.POST, entity, Void.class);

        } catch (HttpClientErrorException | HttpServerErrorException e) {
            // Log the error details
            log.error("Error while calling notification service: {}", e.getMessage());

            // Check if the status is 503 (Service Unavailable) and handle it
            if (e.getStatusCode() == HttpStatus.SERVICE_UNAVAILABLE) {
                // Return a more specific error response
                throw new ResourceException("Notification service is currently unavailable. Please try again later.");
            }

            // If it’s some other type of error, you can throw a different exception or handle accordingly
            throw new ResourceException("An error occurred while accessing the notification service.");
        }
        System.out.println("Notification sent");
    }

    @Override
    public void rejectExpenseFromEmail(Long expenseId,
                                       String title,
                                       String description,
                                       BigDecimal amount,
                                       String category,
                                       String expenseDate,
                                       String emailId,
                                       String role) {

        // Fetch the expense approval request from the database
        List<Approval> approvals = approvalRepository.findByExpenseIdAndStatus(expenseId, ApprovalStatus.PENDING);
        if (approvals.isEmpty()) {
            throw new RuntimeException("No pending approval found for this expense.");
        }

        // Handle rejection based on role
        Approval currentApproval = approvals.stream()
                .filter(a -> a.getApproverRole().equals(role))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("No approval found for role: " + role));

        if (role.equals("MANAGER")) {
            // Update the status to REJECTED
            currentApproval.setStatus(ApprovalStatus.REJECTED);
            approvalRepository.save(currentApproval);
        } else if (role.equals("FINANCE")) {
            currentApproval.setStatus(ApprovalStatus.REJECTED);
            this.approvalRepository.save(currentApproval);
        } else if (role.equals("ADMIN")) {
            currentApproval.setStatus(ApprovalStatus.REJECTED);
            this.approvalRepository.save(currentApproval);
        }
    }

    @Override
    public boolean hasAlreadyResponded(Long expenseId, String employeeEmail) {
        List<ApprovalStatus> statusList = List.of(ApprovalStatus.APPROVED, ApprovalStatus.REJECTED);
        return this.approvalRepository.existsByExpenseIdAndApprovedByAndStatusIn(expenseId, employeeEmail, statusList);
    }

}
