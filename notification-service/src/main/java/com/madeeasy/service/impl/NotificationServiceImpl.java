package com.madeeasy.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.madeeasy.entity.Notification;
import com.madeeasy.exception.ResourceAccessException;
import com.madeeasy.repository.NotificationRepository;
import com.madeeasy.service.NotificationService;
import com.madeeasy.vo.UserResponse;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {
    private final JavaMailSender mailSender;
    private final HttpServletRequest httpServletRequest;
    private final RestTemplate restTemplate;
    private final HttpServletResponse httpServletResponse;
    private final ObjectMapper objectMapper;
    private final NotificationRepository notificationRepository;

    @CircuitBreaker(name = "notificationServiceCircuitBreaker", fallbackMethod = "circuitBreakerFallback")
    @Override
    public void sendApprovalNotification(Long expenseId,
                                         String title,
                                         String description,
                                         BigDecimal amount,
                                         String category,
                                         String expenseDate,
                                         String emailId,
                                         String approvalLink,
                                         String rejectLink) {
//        // Change the URLs to point to the NotificationController
//        String approvalLink = "http://localhost:8084/notification-service/approve?expenseId=" + expenseId;  // Change port to match your NotificationController's port
//        String rejectionLink = "http://localhost:8084/notification-service/reject?expenseId=" + expenseId;  // Change port to match your NotificationController's port
        String accessToken = null;
        String role = null;
        try {
            // Create a URL object
            URL urlObj = new URL(approvalLink);

            // Get the query string from the URL
            String query = urlObj.getQuery();

            // Split the query into individual key-value pairs
            String[] pairs = query.split("&");

            // Store key-value pairs in a map
            Map<String, String> queryParams = new HashMap<>();
            for (String pair : pairs) {
                String[] keyValue = pair.split("=");
                queryParams.put(keyValue[0], URLDecoder.decode(keyValue[1], StandardCharsets.UTF_8));
            }

            // Extract the accessToken
//            accessToken = queryParams.get("accessToken");
            accessToken = getAccessToken(this.httpServletRequest);
            role = queryParams.get("role");
            // Output the accessToken
            System.out.println("Access Token: " + accessToken);
        } catch (Exception e) {
            e.printStackTrace();
        }
        // Rest Call to Auth-Service to get details ?
        // Rest call to auth-service to get user details by email
        String authUrlToGetUser = "http://localhost:8081/auth-service/get-user/" + emailId;
//        UserResponse userResponse = null;
//        String approverName = null;

        String approverName = null;
        try {
            UserResponse userResponse = restTemplate.exchange(authUrlToGetUser, HttpMethod.GET,
                    new HttpEntity<>(createHeaders(accessToken)), UserResponse.class).getBody();
            assert userResponse != null;
            approverName = userResponse.getFullName();
        } catch (HttpClientErrorException e) {
            // Log the error and extract the JSON part from the message
            String errorMessage = e.getMessage();

            // Extract the JSON part from the error message (status and message)
            Map<String, Object> errorMap = extractJsonFromErrorMessage(errorMessage);

            // Extract status and message separately from the Map
            String status = (String) errorMap.get("status");  // status as String
            String message = (String) errorMap.get("message");  // message as String

            // Optionally log or process the status and message
            log.error("Error occurred while calling Auth-Service. Status: {}, Message: {}", status, message);
            throw new ResourceAccessException(HttpStatus.valueOf(status), message);
        }

        assert role != null;
        String emailContent = generateApprovalEmail(
                approverName,
                title,
                amount,
                approvalLink,
                rejectLink,
                role,
                expenseDate,   // Add expenseDate here
                description,   // Add description here
                category       // Add category here
        );


        try {
            sendEmail(emailId, "🚀 Expense Approval Required", emailContent);
        } catch (MessagingException e) {
            log.error("Failed to send approval email", e);
        }
        // Save to Audit Database after sending the email
        saveNotificationAudit(expenseId, title, description, category, emailId, role);
        log.info("Approval Email sent to : {}", emailId);
    }

    public String getAccessToken(HttpServletRequest request) {
        // Get the Authorization header from the request
        String authHeader = request.getHeader(HttpHeaders.AUTHORIZATION);

        // Check if the Authorization header is not null and starts with "Bearer "
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            // Extract the token by removing the "Bearer " part
            return authHeader.substring("Bearer ".length());
        }

        // If the header is not in the expected format, return null or throw an exception as needed
        return null; // Or you could throw an exception to indicate invalid token
    }

    private void saveNotificationAudit(Long expenseId, String title, String description, String category,
                                       String emailId, String role) {
        Notification auditNotification = Notification.builder()
                .expenseId(expenseId)
                .title(title)
                .description(description)
                .category(category)
                .emailId(emailId)
                .role(role)
                .notificationSentAt(new Date())  // Time when the notification was sent
                .build();

        this.notificationRepository.save(auditNotification);  // Save to the database
        log.info("Notification audit saved for expenseId: {}", expenseId);
    }

    // Helper method to extract JSON part from error message and convert it into a Map
    private Map<String, Object> extractJsonFromErrorMessage(String errorMessage) {
        String regex = "\\{.*?\\}";  // Regular expression to match JSON in curly brackets

        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(errorMessage);

        // Find and extract the JSON part
        if (matcher.find()) {
            String jsonPart = matcher.group();  // Extract the JSON string

            // Convert the JSON string to a Map
            try {
                ObjectMapper objectMapper = new ObjectMapper();
                return objectMapper.readValue(jsonPart, Map.class);  // Convert to Map<String, Object>
            } catch (Exception e) {
                log.error("Failed to parse JSON: {}", e.getMessage());
            }
        }

        return Map.of();  // Return an empty map if no JSON part found
    }

    // Fallback method for retry
    public void retryFallback(Long expenseId, String title, String description, BigDecimal amount,
                              String category, String expenseDate, String emailId, String approvalLink,
                              String rejectLink, Exception e) {
        log.error("Retry failed after multiple attempts: {}", e.getMessage());
        // Handle the retry failure (e.g., return a specific response or rethrow an exception)
    }

    // Fallback method for circuit breaker when Auth Service is down
    public void circuitBreakerFallback(Long expenseId, String title, String description, BigDecimal amount,
                                       String category, String expenseDate, String emailId, String approvalLink,
                                       String rejectLink, Throwable t) throws Throwable {

        // Check if the throwable is an instance of ResourceAccessException
        if (t instanceof ResourceAccessException) {
            // Cast the throwable to your custom exception
            ResourceAccessException resourceAccessException = (ResourceAccessException) t;

            // Optionally log the exception (optional but good for debugging)
            log.error("ResourceAccessException in circuit breaker: {}", resourceAccessException.getMessage());

            // Extract status and message from the exception (assuming getStatus() provides a numeric status code like "401")
            String statusCodeStr = resourceAccessException.getStatus().toString().substring(0, 3);  // Extracts the first 3 digits, e.g., "401"
            String message = resourceAccessException.getMessage();  // Assuming the message is set in the exception

            log.error("status: {}, message: {}", statusCodeStr, message);

            // Convert the status code to integer and map it to HttpStatus
            int statusCode = Integer.parseInt(statusCodeStr);

            // Use HttpStatus.valueOf() with the numeric status code value
            HttpStatus httpStatus = HttpStatus.resolve(statusCode);

            log.error("status : {}, message : {}", httpStatus, message);
            // Throw the custom exception (use the status and message from your logic)
            throw new ResourceAccessException(httpStatus, message);
        }

        log.error("Circuit breaker triggered: Auth Service is unavailable: {}", t.getMessage());

        // Return a custom error response indicating the Auth Service is down
        Map<String, Object> errorResponse = Map.of(
                "status", HttpStatus.SERVICE_UNAVAILABLE,
                "message", "Auth Service is currently unavailable. Please try again later."
        );

        // Set the HTTP response type and status
        this.httpServletResponse.setContentType("application/json");
        this.httpServletResponse.setStatus(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
        this.httpServletResponse.getWriter().write(objectMapper.writeValueAsString(errorResponse));
    }


    private HttpHeaders createHeaders(String accessToken) {
        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken);  // Ensure 'Bearer' prefix
        return headers;
    }

    private String generateApprovalEmail(String approverName, String expenseTitle, BigDecimal amount,
                                         String approvalLink, String rejectionLink, String approverRole,
                                         String expenseDate, String description, String category) {
        // Determine the correct contact person based on the role
        String contactPerson;
        if (approverRole.equalsIgnoreCase("Manager")) {
            contactPerson = "Expense Initiator";
        } else if (approverRole.equalsIgnoreCase("Finance")) {
            contactPerson = "Department Manager";
        } else {
            contactPerson = "Finance Department"; // this is for ADMIN
        }

        return "<html>" +
                "<head>" +
                "<style>" +
                "body { font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; background-color: #f5f5f5; padding: 20px; text-align: center; }" +
                ".container { max-width: 600px; margin: auto; padding: 30px; background-color: #ffffff;" +
                "border-radius: 12px; box-shadow: 0 4px 8px rgba(0, 0, 0, 0.1); text-align: left; }" +
                ".header { background-color: #007bff; color: #ffffff; padding: 20px; text-align: center; " +
                "font-size: 22px; font-weight: bold; border-radius: 12px 12px 0 0; }" +
                ".content { padding: 20px; font-size: 16px; color: #333333; line-height: 1.6; }" +
                ".highlight { font-size: 18px; font-weight: bold; }" +
                ".expense-title { color: #0056b3; }" +
                ".amount { color: #28a745; }" +
                ".expense-date { color: #e26a6a; }" +
                ".category { color: #6f42c1; }" +
                ".description { font-size: 16px; color: #555555; background-color: #e9ecef; padding: 10px; border-radius: 5px; margin-top: 15px; }" +
                ".sub-heading { font-size: 16px; color: #555555; margin-top: 10px; font-weight: bold; }" +
                ".footer { font-size: 14px; color: #777777; margin-top: 20px; text-align: center; }" +
                ".button-container { text-align: center; margin-top: 20px; }" +
                ".button { display: inline-block; padding: 12px 20px; margin: 10px; font-size: 16px; " +
                "border-radius: 5px; text-decoration: none; font-weight: bold; color: white; border: none; cursor: pointer; }" +
                ".approve { background-color: #28a745; }" +
                ".reject { background-color: #dc3545; }" +
                "</style>" +
                "</head>" +
                "<body>" +
                "<div class=\"container\">" +
                "<div class=\"header\">🚀 Enterprise Expense Management System</div>" +
                "<div class=\"content\">" +
                "<p>Dear <strong>" + approverName + "</strong>,</p>" +
                "<p>An expense request has been submitted and requires your immediate attention:</p>" +

                "<p><span class='sub-heading'>Expense Title:</span> <span class='highlight expense-title'>" + expenseTitle + "</span></p>" +
                "<p><span class='sub-heading'>Amount:</span> <span class='highlight amount'>$" + amount + "</span></p>" +
                "<p><span class='sub-heading'>Expense Date:</span> <span class='highlight expense-date'>" + expenseDate + "</span></p>" +
                "<p><span class='sub-heading'>Category:</span> <span class='highlight category'>" + category + "</span></p>" +

                "<div class='description'>" +
                "<strong>Description:</strong><br>" +
                description +
                "</div>" +

                "<div class='button-container'>" +
                "<a href='" + approvalLink + "' class=\"button approve\">✅ Approve Expense</a>" +
                "<a href='" + rejectionLink + "' class=\"button reject\">❌ Reject Expense</a>" +
                "</div>" +

                "<p>If you have any questions, please contact the <strong>" + contactPerson + "</strong>.</p>" +
                "</div>" +
                "<div class=\"footer\">&copy; 2025 Enterprise Expense Management System. All Rights Reserved.</div>" +
                "</div>" +
                "</body>" +
                "</html>";
    }


    private void sendEmail(String to, String subject, String content) throws MessagingException {
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

        helper.setTo(to);
        helper.setSubject(subject);
        helper.setText(content, true); // true -> send as HTML

        try {
            mailSender.send(message);
        } catch (MailException e) {
            log.error("Error : {}", e.getCause());
        }
        log.info("Approval email sent to {}", to);
    }
}