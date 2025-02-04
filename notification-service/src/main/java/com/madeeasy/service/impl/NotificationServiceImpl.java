package com.madeeasy.service.impl;

import com.madeeasy.service.NotificationService;
import com.madeeasy.vo.UserResponse;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;


@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {
    private final JavaMailSender mailSender;
    private final HttpServletRequest httpServletRequest;
    private final RestTemplate restTemplate;

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
            accessToken = queryParams.get("accessToken");
            role = queryParams.get("role");
            // Output the accessToken
            System.out.println("Access Token: " + accessToken);
        } catch (Exception e) {
            e.printStackTrace();
        }
        // Rest Call to Auth-Service to get details ?
        // Rest call to auth-service to get user details by email
        String authUrlToGetUser = "http://localhost:8081/auth-service/get-user/" + emailId;
        UserResponse userResponse = restTemplate.exchange(authUrlToGetUser, HttpMethod.GET,
                new HttpEntity<>(createHeaders(accessToken)), UserResponse.class).getBody();
        assert userResponse != null;
        String approverName = userResponse.getFullName();
        String emailContent = generateApprovalEmail(approverName, title,
                amount, approvalLink, rejectLink, role);

        try {
            sendEmail(emailId, "🚀 Expense Approval Required", emailContent);
        } catch (MessagingException e) {
            log.error("Failed to send approval email", e);
        }

    }

    private HttpHeaders createHeaders(String accessToken) {
        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken);  // Ensure 'Bearer' prefix
        return headers;
    }

    private String generateApprovalEmail(String approverName, String expenseTitle, BigDecimal amount,
                                         String approvalLink, String rejectionLink, String approverRole) {
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
                "body { font-family: Arial, sans-serif; background-color: #f4f4f4; padding: 20px; text-align: center; }" +
                ".container { max-width: 600px; margin: auto; padding: 30px; background-color: #ffffff;" +
                "border-radius: 10px; box-shadow: 0 4px 8px rgba(0, 0, 0, 0.2); text-align: left; }" +
                ".header { background-color: #007bff; color: #ffffff; padding: 15px; text-align: center; " +
                "font-size: 20px; font-weight: bold; border-radius: 10px 10px 0 0; }" +
                ".content { padding: 20px; font-size: 16px; color: #333333; }" +
                ".highlight { font-size: 18px; font-weight: bold; color: #007bff; }" +
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
                "<p><strong>Expense Title:</strong> <span class='highlight'>" + expenseTitle + "</span></p>" +
                "<p><strong>Amount:</strong> <span class='highlight'>$" + amount + "</span></p>" +
                "<p>Please review the request and take appropriate action:</p>" +
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

        mailSender.send(message);
        log.info("Approval email sent to {}", to);
    }
}