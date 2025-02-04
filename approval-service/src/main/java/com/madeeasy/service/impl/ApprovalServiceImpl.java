package com.madeeasy.service.impl;

import com.madeeasy.dto.request.ExpenseRequestDTO;
import com.madeeasy.dto.response.UserResponse;
import com.madeeasy.entity.Approval;
import com.madeeasy.entity.ApprovalStatus;
import com.madeeasy.repository.ApprovalRepository;
import com.madeeasy.service.ApprovalService;
import com.madeeasy.util.JwtUtils;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
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
    private final JwtUtils jwtUtils;
    private final HttpServletRequest httpServletRequest;

    @Override
    public void askForApproval(ExpenseRequestDTO expenseRequestDTO) throws UnsupportedEncodingException {
        String authHeader = httpServletRequest.getHeader(HttpHeaders.AUTHORIZATION);
        String accessToken = authHeader.substring("Bearer ".length());

        // rest call with authorization herader to get user details by company domain and role
        String authUrlToGetUser = "http://localhost:8081/auth-service/get-user/" + expenseRequestDTO.getCompanyDomain() + "/" + "MANAGER";
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


        // Prepare the expense details as query parameters
        String expenseDetails = "expenseId=" + URLEncoder.encode(String.valueOf(expenseRequestDTO.getExpenseId()), StandardCharsets.UTF_8) +
                "&title=" + URLEncoder.encode(expenseRequestDTO.getTitle(), StandardCharsets.UTF_8) +
                "&description=" + URLEncoder.encode(expenseRequestDTO.getDescription(), StandardCharsets.UTF_8) +
                "&amount=" + URLEncoder.encode(String.valueOf(expenseRequestDTO.getAmount()), StandardCharsets.UTF_8) +
                "&category=" + URLEncoder.encode(String.valueOf(expenseRequestDTO.getCategory()), StandardCharsets.UTF_8) +
                "&expenseDate=" + URLEncoder.encode(expenseRequestDTO.getExpenseDate().toString(), StandardCharsets.UTF_8) +
                "&accessToken=" + URLEncoder.encode(accessToken, StandardCharsets.UTF_8) +
                "&emailId=" + URLEncoder.encode(managerEmail, StandardCharsets.UTF_8);// in future call to auth-service and by company domain get manager emailId and set here

        // Send email to Manager for approval
        String approveLink = "http://localhost:8085/approval-service/approve?" + expenseDetails + "&emailId=" + managerEmail + "&role=MANAGER";
        String rejectLink = "http://localhost:8085/approval-service/reject?" + expenseDetails + "&emailId=" + managerEmail + "&role=MANAGER";

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
                .companyDomain(expenseRequestDTO.getCompanyDomain())
                .approverRole("MANAGER")
                .approvedBy(managerEmail)
                .status(ApprovalStatus.PENDING)
                .build();
        this.approvalRepository.save(approval);
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
            this.approvalRepository.save(currentApproval);
            // rest call with authorization herader to get user details by company domain and role
            String authUrlToGetUser = "http://localhost:8081/auth-service/get-user/" + currentApproval.getCompanyDomain() + "/" + "FINANCE";
            List<UserResponse> userResponseList = restTemplate.exchange(authUrlToGetUser, HttpMethod.GET,
                    new HttpEntity<>(createHeaders(accessToken)), new ParameterizedTypeReference<List<UserResponse>>() {
                    }).getBody();
            // Check if the response is null or empty
            if (userResponseList == null || userResponseList.isEmpty()) {
                throw new IllegalStateException("Unable to fetch user information.");
            }

            // Assuming the list contains only one user for the given role, get the first user
            UserResponse userResponse = userResponseList.getFirst();  // Get the first element from the list
            String financeEmail = userResponse.getEmail();  // Access the email property of the first user

            // Approve and send to Finance
            System.out.println("Manager role trying to send another notifcation to finance");
            // Save the approval request in the database (Approval table)
            Approval finaceApproval = Approval.builder()
                    .expenseId(currentApproval.getExpenseId())
                    .companyDomain(currentApproval.getCompanyDomain())
                    .approverRole("FINANCE")
                    .approvedBy(financeEmail)
                    .status(ApprovalStatus.PENDING)
                    .build();
            this.approvalRepository.save(finaceApproval);
            sendNextApproval(expenseId, title, description, amount, category, expenseDate, accessToken, financeEmail, "FINANCE");
        } else if (role.equals("FINANCE")) {
            currentApproval.setStatus(ApprovalStatus.APPROVED);
            this.approvalRepository.save(currentApproval);
            // rest call with authorization herader to get user details by company domain and role
            String authUrlToGetUser = "http://localhost:8081/auth-service/get-user/" + currentApproval.getCompanyDomain() + "/" + "ADMIN";
            List<UserResponse> userResponseList = restTemplate.exchange(authUrlToGetUser, HttpMethod.GET,
                    new HttpEntity<>(createHeaders(accessToken)), new ParameterizedTypeReference<List<UserResponse>>() {
                    }).getBody();
            // Check if the response is null or empty
            if (userResponseList == null || userResponseList.isEmpty()) {
                throw new IllegalStateException("Unable to fetch user information.");
            }

            // Assuming the list contains only one user for the given role, get the first user
            UserResponse userResponse = userResponseList.getFirst();  // Get the first element from the list
            String adminEmail = userResponse.getEmail();  // Access the email property of the first user
            // Save the approval request in the database (Approval table)
            Approval finaceApproval = Approval.builder()
                    .expenseId(currentApproval.getExpenseId())
                    .companyDomain(currentApproval.getCompanyDomain())
                    .approverRole("ADMIN")
                    .approvedBy(adminEmail)
                    .status(ApprovalStatus.PENDING)
                    .build();
            this.approvalRepository.save(finaceApproval);
            // Approve and send to Admin
            sendNextApproval(expenseId, title, description, amount, category, expenseDate, accessToken, adminEmail, "ADMIN");
        } else if (role.equals("ADMIN")) {
            // Final approval
            currentApproval.setStatus(ApprovalStatus.APPROVED);
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


        // Prepare the approval/rejection links with the expense details and the role for the next approver
        String approveLink = "http://localhost:8085/approval-service/approve?" + expenseDetails + "&emailId=" + emailId + "&role=" + role;
        String rejectLink = "http://localhost:8085/approval-service/reject?" + expenseDetails + "&emailId=" + emailId + "&role=" + role;

        System.out.println("Inside sendNextApproval method !!");
        System.out.println("approveLink " + approveLink + " rejectLink " + rejectLink);
        // Create the request body, including expense details and links
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("expenseDetails", expenseDetails);  // Sending full expense details
        requestBody.put("approveLink", approveLink);        // Approval link
        requestBody.put("rejectLink", rejectLink);          // Rejection link

        System.out.println("Ready to sent another notification");
        // Send the notification to the next approver
        String notificationUrl = "http://localhost:8084/notification-service/";
        restTemplate.postForObject(notificationUrl, requestBody, Void.class);
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
