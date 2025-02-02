package com.madeeasy.service.impl;

import com.madeeasy.dto.request.ExpenseRequestDTO;
import com.madeeasy.entity.Approval;
import com.madeeasy.entity.ApprovalStatus;
import com.madeeasy.repository.ApprovalRepository;
import com.madeeasy.service.ApprovalService;
import com.madeeasy.util.JwtUtils;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class ApprovalServiceImpl implements ApprovalService {

    private final ApprovalRepository approvalRepository;
    private final RestTemplate restTemplate;
    private final JwtUtils jwtUtils;
    private final HttpServletRequest httpServletRequest;

    @Override
    public void askForApproval(ExpenseRequestDTO expenseRequestDTO) {
        String accessToken = httpServletRequest.getHeader(HttpHeaders.AUTHORIZATION);
        String approverRole = jwtUtils.getRoleFromToken(accessToken);
        String approverEmail = jwtUtils.getUserName(accessToken);

        // Prepare the expense details as query parameters
        String expenseDetails = "expenseId=" + expenseRequestDTO.getExpenseId() +
                "&title=" + expenseRequestDTO.getTitle() +
                "&description=" + expenseRequestDTO.getDescription() +
                "&amount=" + expenseRequestDTO.getAmount() +
                "&category=" + expenseRequestDTO.getCategory() +
                "&expenseDate=" + expenseRequestDTO.getExpenseDate();

        // Send email to Manager for approval
        String approveLink = "http://localhost:8084/approval-service/approve?" + expenseDetails + "&emailId=" + "pabitrabera2001@gmail.com" + "&role=MANAGER";
        String rejectLink = "http://localhost:8084/approval-service/reject?" + expenseDetails + "&emailId=" + "pabitrabera2001@gmail.com" + "&role=MANAGER";

        // Create the request body, including expense details and links
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("expenseDetails", expenseDetails);  // Sending full expense details
        requestBody.put("approveLink", approveLink);        // Approval link
        requestBody.put("rejectLink", rejectLink);          // Rejection link

        // Rest call to notification-services to send email
        String notificationUrl = "http://localhost:8084/notification-service/";
        restTemplate.postForObject(notificationUrl, requestBody, Void.class);

        // Save the approval request in the database (Approval table)
        Approval approval = Approval.builder()
                .expenseId(expenseRequestDTO.getExpenseId())
                .companyId(expenseRequestDTO.getCompanyId())
                .approverRole(approverRole)
                .approvedBy(approverEmail)
                .status(ApprovalStatus.PENDING)
                .build();
        this.approvalRepository.save(approval);
    }

    @Override
    public void approveExpenseFromEmail(Long expenseId,
                                        String title,
                                        String description,
                                        BigDecimal amount,
                                        String category,
                                        String expenseDate,
                                        String emailId,
                                        String role) {
        Approval approval = approvalRepository.findByExpenseId(expenseId)
                .orElseThrow(() -> new RuntimeException("Expense not found"));

        if (role.equals("MANAGER")) {
            // Approve and send to Finance
            sendNextApproval(expenseId, title, description, amount, category, expenseDate, "pabitrabera2001@gmail.com", "FINANCE");
        } else if (role.equals("FINANCE")) {
            // Approve and send to Admin
            sendNextApproval(expenseId, title, description, amount, category, expenseDate, "pabitrabera2001@gmail.com", "ADMIN");
        } else if (role.equals("ADMIN")) {
            // Final approval
            approval.setStatus(ApprovalStatus.APPROVED);
            approvalRepository.save(approval);
        }
    }

    public void sendNextApproval(Long expenseId,
                                 String title,
                                 String description,
                                 BigDecimal amount,
                                 String category,
                                 String expenseDate,
                                 String emailId,
                                 String role) {
        // Fetch expense details from the database
        Approval approval = approvalRepository.findById(expenseId)
                .orElseThrow(() -> new RuntimeException("Expense not found"));

        // Prepare the expense details as query parameters
        String expenseDetails = "expenseId=" + expenseId +
                "&amount=" + amount +
                "&category=" + category +
                "&description=" + description +
                "&title=" + title +
                "&expenseDate=" + expenseDate;

        // Prepare the approval/rejection links with the expense details and the role for the next approver
        String approveLink = "http://localhost:8084/approval-service/approve?" + expenseDetails + "&emailId=" + emailId + "&role=" + role;
        String rejectLink = "http://localhost:8084/approval-service/reject?" + expenseDetails + "&emailId=" + emailId + "&role=" + role;

        // Create the request body, including expense details and links
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("expenseDetails", expenseDetails);  // Sending full expense details
        requestBody.put("approveLink", approveLink);        // Approval link
        requestBody.put("rejectLink", rejectLink);          // Rejection link

        // Send the notification to the next approver
        String notificationUrl = "http://localhost:8084/notification-service/";
        restTemplate.postForObject(notificationUrl, requestBody, Void.class);
    }
}
