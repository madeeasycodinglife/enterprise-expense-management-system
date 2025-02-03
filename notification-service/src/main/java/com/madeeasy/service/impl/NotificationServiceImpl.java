package com.madeeasy.service.impl;

import com.madeeasy.service.NotificationService;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;


@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {
    private final JavaMailSender mailSender;
    private final HttpServletRequest httpServletRequest;

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

        String emailContent = generateApprovalEmail("Pabitra Bera", title,
                amount, approvalLink, rejectLink);

        try {
            sendEmail(emailId, "🚀 Expense Approval Required", emailContent);
        } catch (MessagingException e) {
            log.error("Failed to send approval email", e);
        }

    }

    private String generateApprovalEmail(String approverName, String expenseTitle, BigDecimal amount,
                                         String approvalLink, String rejectionLink) {
        return "<html>" +
                "<head>" +
                "<style>" +
                "body { font-family: Arial, sans-serif; text-align: center; }" +
                ".container { max-width: 500px; margin: auto; padding: 20px; border-radius: 10px;" +
                "background-color: #f9f9f9; box-shadow: 0 0 10px rgba(0, 0, 0, 0.1); }" +
                "h2 { color: #333; }" +
                "p { font-size: 16px; color: #666; }" +
                ".button { display: inline-block; padding: 12px 20px; margin: 10px; font-size: 16px;" +
                "border-radius: 5px; text-decoration: none; font-weight: bold; }" +
                ".approve { background-color: #28a745; color: white; }" +
                ".reject { background-color: #dc3545; color: white; }" +
                "</style>" +
                "</head>" +
                "<body>" +
                "<div class=\"container\">" +
                "<h2>🚀 Expense Approval Required</h2>" +
                "<p>Hello, " + approverName + ",</p>" +
                "<p>A new expense requires your approval:</p>" +
                "<p><strong>Expense:</strong> " + expenseTitle + "</p>" +
                "<p><strong>Amount:</strong> $" + amount + "</p>" +
                "<a href='" + approvalLink + "' class=\"button approve\">✅ Approve</a>" +
                "<a href='" + rejectionLink + "' class=\"button reject\">❌ Reject</a>" +
                "<p>Thank you,</p>" +
                "<p><strong>Enterprise Expense Management System</strong></p>" +
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