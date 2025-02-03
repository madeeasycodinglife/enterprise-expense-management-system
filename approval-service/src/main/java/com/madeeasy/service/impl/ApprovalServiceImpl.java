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

    // Find the approval for the current role from the list
    private Approval findPendingApprovalForRole(List<Approval> approvals, String role) {
        return approvals.stream()
                .filter(a -> a.getApproverRole().equals(role) && a.getStatus() == ApprovalStatus.PENDING)
                .findFirst()
                .orElse(null);  // Return null if no pending approval found for the role
    }

    private void handleManagerApproval(Long expenseId,
                                       String title,
                                       String description,
                                       BigDecimal amount,
                                       String category,
                                       String expenseDate,
                                       String accessToken,
                                       String emailId) {
        // Fetch the pending approval record for the MANAGER
        Approval managerApproval = findPendingApprovalForRole(expenseId, "MANAGER");

        if (managerApproval != null) {
            // Update the status to APPROVED for the MANAGER
            managerApproval.setStatus(ApprovalStatus.APPROVED);
            approvalRepository.save(managerApproval);

            // Get the finance approver's email
            String financeEmail = getApproverEmail(expenseId, "FINANCE", accessToken);

            // Send the approval request to FINANCE
            sendNextApproval(expenseId, title, description, amount, category, expenseDate, accessToken, financeEmail, "FINANCE");
        }
    }

    private void handleFinanceApproval(Long expenseId,
                                       String title,
                                       String description,
                                       BigDecimal amount,
                                       String category,
                                       String expenseDate,
                                       String accessToken,
                                       String emailId) {
        // Fetch the pending approval record for FINANCE
        Approval financeApproval = findPendingApprovalForRole(expenseId, "FINANCE");

        if (financeApproval != null) {
            // Update the status to APPROVED for FINANCE
            financeApproval.setStatus(ApprovalStatus.APPROVED);
            approvalRepository.save(financeApproval);

            // Get the admin approver's email
            String adminEmail = getApproverEmail(expenseId, "ADMIN", accessToken);

            // Send the approval request to ADMIN
            sendNextApproval(expenseId, title, description, amount, category, expenseDate, accessToken, adminEmail, "ADMIN");
        }
    }

    private void handleAdminApproval(Long expenseId,
                                     String title,
                                     String description,
                                     BigDecimal amount,
                                     String category,
                                     String expenseDate,
                                     String accessToken,
                                     String emailId) {
        // Fetch the pending approval record for ADMIN
        Approval adminApproval = findPendingApprovalForRole(expenseId, "ADMIN");

        if (adminApproval != null) {
            // Update the status to APPROVED for ADMIN
            adminApproval.setStatus(ApprovalStatus.APPROVED);
            approvalRepository.save(adminApproval);
        }
    }


    private Approval findPendingApprovalForRole(Long expenseId, String role) {
        return approvalRepository.findByExpenseIdAndApproverRoleAndStatus(expenseId, role, ApprovalStatus.PENDING)
                .orElseThrow(() -> new RuntimeException("No pending approval found for " + role));
    }

    private String getApproverEmail(Long expenseId, String role, String accessToken) {
        // Fetch the approver's email for the given role (e.g., FINANCE or ADMIN)

        String authUrlToGetUser = "http://localhost:8081/auth-service/get-user/" + getCompanyDomain(expenseId) + "/" + role;
        List<UserResponse> userResponseList = restTemplate.exchange(authUrlToGetUser, HttpMethod.GET,
                new HttpEntity<>(createHeaders(accessToken)), new ParameterizedTypeReference<List<UserResponse>>() {
                }).getBody();

        if (userResponseList == null || userResponseList.isEmpty()) {
            throw new IllegalStateException("Unable to fetch user information for role: " + role);
        }

        UserResponse userResponse = userResponseList.getFirst();  // Get the first element from the list
        return userResponse.getEmail();  // Return the email of the first user
    }

    private String getCompanyDomain(Long expenseId) {
        // Fetch the company domain for the given expenseId from the database
        Approval approval = this.approvalRepository.findById(expenseId)
                .orElseThrow(() -> new RuntimeException("Expense not found"));

        // Assuming the company domain is part of the Expense entity
        return approval.getCompanyDomain();  // Fetch the company domain associated with the expense
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

        // Update the status to REJECTED
        currentApproval.setStatus(ApprovalStatus.REJECTED);
        approvalRepository.save(currentApproval);
        // Notify the requester that the expense has been rejected
        notifyRejection(expenseId, title, description, amount, category, expenseDate, emailId, role);
    }

    private void notifyRejection(Long expenseId,
                                 String title,
                                 String description,
                                 BigDecimal amount,
                                 String category,
                                 String expenseDate,
                                 String emailId,
                                 String role) {
        String encodedTitle = null;
        String encodedDescription = null;
        String encodedCategory = null;
        String encodedExpenseDate = null;
        String encodedEmailId = null;
        try {
            // URL-encode each parameter using UTF-8
            encodedTitle = URLEncoder.encode(title, StandardCharsets.UTF_8);
            encodedDescription = URLEncoder.encode(description, "UTF-8");
            encodedCategory = URLEncoder.encode(category, "UTF-8");
            encodedExpenseDate = URLEncoder.encode(expenseDate, "UTF-8");
            encodedEmailId = URLEncoder.encode(emailId, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            System.out.println(e.getMessage());
        }

        // Construct the rejection details string with encoded parameters
        String rejectionDetails = "expenseId=" + expenseId +
                "&amount=" + amount +
                "&category=" + encodedCategory +
                "&description=" + encodedDescription +
                "&title=" + encodedTitle +
                "&expenseDate=" + encodedExpenseDate +
                "&emailId=" + encodedEmailId;

        System.out.println("Inside notifyRejection method !!");
        System.out.println("expenseId: " + expenseId + " title: " + title + " description: " + description + " amount: " + amount + " category: " + category + " expenseDate: " + expenseDate + " email: " + emailId + " role: " + role);
        // Create the request body
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("expenseDetails", rejectionDetails);
        requestBody.put("rejectedBy", role);
        requestBody.put("rejectionReason", "Your expense request has been rejected by " + role);

//        // Notify requester via notification service
//        String notificationUrl = "http://localhost:8084/notification-service/";
//        restTemplate.postForObject(notificationUrl, requestBody, Void.class);
    }

}
