package com.madeeasy.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.madeeasy.dto.request.AuthRequest;
import com.madeeasy.dto.request.LogOutRequest;
import com.madeeasy.dto.request.SignInRequestDTO;
import com.madeeasy.dto.request.UserRequest;
import com.madeeasy.dto.response.AuthResponse;
import com.madeeasy.entity.Role;
import com.madeeasy.entity.Token;
import com.madeeasy.entity.TokenType;
import com.madeeasy.entity.User;
import com.madeeasy.exception.ClientException;
import com.madeeasy.exception.TokenException;
import com.madeeasy.repository.TokenRepository;
import com.madeeasy.repository.UserRepository;
import com.madeeasy.service.AuthService;
import com.madeeasy.util.JwtUtils;
import com.madeeasy.vo.Company;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final JwtUtils jwtUtils;
    private final PasswordEncoder passwordEncoder;
    private final TokenRepository tokenRepository;
    private final AuthenticationManager authenticationManager;
    private final RestTemplate restTemplate;
    private final HttpServletRequest httpServletRequest;

    @Override
    public AuthResponse singUp(AuthRequest authRequest) {
        String normalizedRole = authRequest.getRole().toUpperCase();

        // Check if roles contain valid enum names
        if (!normalizedRole.contains(Role.EMPLOYEE.name()) &&
                !normalizedRole.contains(Role.MANAGER.name()) &&
                !normalizedRole.contains(Role.FINANCE.name()) &&
                !normalizedRole.contains(Role.ADMIN.name())) {
            return AuthResponse.builder()
                    .message("Invalid roles provided. Allowed roles are " + Arrays.toString(Role.values()))
                    .status(HttpStatus.BAD_REQUEST)
                    .build();
        }

        if (authRequest.getCompanyDomain() != null && !authRequest.getCompanyDomain().isBlank()) {
            // Rest Call To Company Service to check if Company exists or Not
            String url = "http://company-service/company-service/domain-name/" + authRequest.getCompanyDomain();
            boolean isCompanyExists = false;

            System.out.println("Company Service is being called.....");
            ResponseEntity<Company> responseEntity = null;
            Company company = null;
            try {
                // Get the access token from request headers
                String authHeader = httpServletRequest.getHeader(HttpHeaders.AUTHORIZATION);
                String accessToken = authHeader.substring("Bearer ".length());

                // Set up the authorization header with Bearer token
                HttpHeaders headers = new HttpHeaders();
                headers.set("Authorization", "Bearer " + accessToken);

                // Create an HttpEntity object with headers (no body for GET request)
                HttpEntity<String> entity = new HttpEntity<>(headers);

                // Perform the HTTP GET request and map the response to Company
                responseEntity = this.restTemplate.exchange(url, HttpMethod.GET, entity, Company.class);

                System.out.println("Company Service has been called.....");

                if (responseEntity.getStatusCode().is2xxSuccessful()) {
                    company = responseEntity.getBody();
                    if (company != null) {
                        isCompanyExists = true;
                    } else {
                        System.out.println("No company data found.");
                    }
                } else {
                    // Handle non-2xx HTTP status codes
                    System.out.println("Failed to fetch company data. HTTP status: " + responseEntity.getStatusCode());
                    handleNon2xxStatus((HttpStatus) responseEntity.getStatusCode());
                }
            } catch (HttpClientErrorException | HttpServerErrorException e) {
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
                    String status = jsonNode.get("status").asText().substring(0, 3);

                    HttpStatus httpStatus = null;
                    try {
                        // Check if the extracted status is numeric, then convert it to HttpStatus
                        if (status.matches("\\d{3}")) {
                            int statusCode = Integer.parseInt(status);  // Convert to integer
                            httpStatus = HttpStatus.resolve(statusCode);  // Get HttpStatus from status code
                        } else {
                            httpStatus = HttpStatus.valueOf(status);  // If it's a name like "NOT_FOUND"
                        }
                    } catch (IllegalArgumentException e2) {
                        log.error("Invalid status '{}' found", status);
                    }

                    log.error("Parsed message: {}", message);
                    log.error("Parsed status: {}", status);
                    log.error(message, httpStatus);

                    // Re-throw ClientException so that it will be caught by the GlobalExceptionHandler
                    throw new ClientException(message, httpStatus);
                } else {
                    log.error("Failed to parse JSON response body.");
                    throw new ClientException("Authorization failed, unable to parse error response", HttpStatus.INTERNAL_SERVER_ERROR);
                }
            }
            User user = User.builder()
                    .fullName(authRequest.getFullName())
                    .email(authRequest.getEmail())
                    .password(passwordEncoder.encode(authRequest.getPassword()))
                    .phone(authRequest.getPhone())
                    .isAccountNonExpired(true)
                    .isAccountNonLocked(true)
                    .isCredentialsNonExpired(true)
                    .isEnabled(true)
                    .role(Role.valueOf(normalizedRole))
                    .build();
            if (isCompanyExists) {
                user.setCompanyDomain(company.getDomain());
                User savedUser = userRepository.save(user);


                String accessToken = jwtUtils.generateAccessTokenWithCompanyDomain(user.getEmail(), user.getRole().name(), user.getCompanyDomain());
                String refreshToken = jwtUtils.generateRefreshTokenWithCompanyDomain(user.getEmail(), user.getRole().name(), user.getCompanyDomain());

                Token token = Token.builder()
                        .id(UUID.randomUUID().toString())
                        .user(savedUser)
                        .token(accessToken)
                        .isRevoked(false)
                        .isExpired(false)
                        .tokenType(TokenType.BEARER)
                        .build();

                tokenRepository.save(token);

                return AuthResponse.builder()
                        .accessToken(accessToken)
                        .refreshToken(refreshToken)
                        .status(HttpStatus.CREATED)
                        .build();
            }
            return AuthResponse.builder()
                    .message("Company with domain name: " + authRequest.getCompanyDomain() + " does not exist.")
                    .status(HttpStatus.NOT_FOUND)
                    .build();

        }

        User user = User.builder()
                .fullName(authRequest.getFullName())
                .email(authRequest.getEmail())
                .password(passwordEncoder.encode(authRequest.getPassword()))
                .phone(authRequest.getPhone())
                .isAccountNonExpired(true)
                .isAccountNonLocked(true)
                .isCredentialsNonExpired(true)
                .isEnabled(true)
                .role(Role.valueOf(normalizedRole))
                .build();

        boolean emailExists = userRepository.existsByEmail(authRequest.getEmail());
        boolean phoneExists = userRepository.existsByPhone(authRequest.getPhone());

        if (emailExists && phoneExists) {
            return AuthResponse.builder()
                    .message("User with Email: " + authRequest.getEmail() + " and Phone: " + authRequest.getPhone() + " already exists.")
                    .status(HttpStatus.CONFLICT)
                    .build();
        } else if (emailExists) {
            return AuthResponse.builder()
                    .message("User with Email: " + authRequest.getEmail() + " already exists.")
                    .status(HttpStatus.CONFLICT)
                    .build();
        } else if (phoneExists) {
            return AuthResponse.builder()
                    .message("User with Phone: " + authRequest.getPhone() + " already exists.")
                    .status(HttpStatus.CONFLICT)
                    .build();
        }

        String accessToken = jwtUtils.generateAccessToken(user.getEmail(), normalizedRole);
        String refreshToken = jwtUtils.generateRefreshToken(user.getEmail(), normalizedRole);

        Token token = Token.builder()
                .id(UUID.randomUUID().toString())
                .user(user)
                .token(accessToken)
                .isRevoked(false)
                .isExpired(false)
                .tokenType(TokenType.BEARER)
                .build();

        userRepository.save(user);
        tokenRepository.save(token);

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .status(HttpStatus.CREATED)
                .build();
    }


    public AuthResponse singIn(SignInRequestDTO signInRequest) {
        Authentication authentication = authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(signInRequest.getEmail(), signInRequest.getPassword()));
        if (authentication.isAuthenticated()) {
            User user = userRepository.findByEmail(signInRequest.getEmail()).orElseThrow(() -> new UsernameNotFoundException("Email not found"));
            revokeAllPreviousValidTokens(user);
            String accessToken = jwtUtils.generateAccessTokenWithCompanyDomain(user.getEmail(), user.getRole().name(), user.getCompanyDomain());
            String refreshToken = jwtUtils.generateRefreshTokenWithCompanyDomain(user.getEmail(), user.getRole().name(), user.getCompanyDomain());


            Token token = Token.builder()
                    .id(UUID.randomUUID().toString())
                    .user(user)
                    .token(accessToken)
                    .isRevoked(false)
                    .isExpired(false)
                    .tokenType(TokenType.BEARER)
                    .build();

            tokenRepository.save(token);

            return AuthResponse.builder()
                    .accessToken(accessToken)
                    .refreshToken(refreshToken)
                    .build();
        } else {
            throw new ClientException("Bad Credential Exception !!");
        }
    }

    @Override
    public void revokeAllPreviousValidTokens(User user) {
        List<Token> tokens = tokenRepository.findAllValidTokens(user.getId());
        tokens.forEach(token -> {
            token.setRevoked(true);
            token.setExpired(true);
        });
        tokenRepository.saveAll(tokens);
    }

    public void logOut(LogOutRequest logOutRequest) {

        String email = logOutRequest.getEmail();
        User user = userRepository.findByEmail(email).orElseThrow(() -> new UsernameNotFoundException("Email not found"));

        jwtUtils.validateToken(logOutRequest.getAccessToken(), jwtUtils.getUserName(logOutRequest.getAccessToken()));
        // Fetch all valid tokens for the user
        List<Token> validTokens = tokenRepository.findAllValidTokens(user.getId());

        // Check if the provided access token is in the list of valid tokens
        validTokens.stream()
                .filter(t -> t.getToken().equals(logOutRequest.getAccessToken()))
                .findFirst()
                .orElseThrow(() -> new TokenException("Token not found or is expired/revoked"));

        revokeAllPreviousValidTokens(user);
    }

    public AuthResponse refreshToken(String refreshToken) {

        boolean isValid = jwtUtils.validateToken(refreshToken, jwtUtils.getUserName(refreshToken));

        if (!isValid) {
            throw new TokenException("Token is invalid");
        }
        User user = userRepository.findByEmail(jwtUtils.getUserName(refreshToken)).orElseThrow(() -> new UsernameNotFoundException("Email not found"));

        revokeAllPreviousValidTokens(user);

        String accessToken = jwtUtils.generateAccessTokenWithCompanyDomain(user.getEmail(), user.getRole().name(), user.getCompanyDomain());
        String newRefreshToken = jwtUtils.generateRefreshTokenWithCompanyDomain(user.getEmail(), user.getRole().name(), user.getCompanyDomain());


        Token token = Token.builder()
                .id(UUID.randomUUID().toString())
                .user(user)
                .token(accessToken)
                .isRevoked(false)
                .isExpired(false)
                .tokenType(TokenType.BEARER)
                .build();

        tokenRepository.save(token);


        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(newRefreshToken)
                .build();
    }

    public boolean validateAccessToken(String accessToken) {

        // Fetch the token from the database by the access token
        Token token = tokenRepository.findValidTokenByAccessToken(accessToken)
                .orElseThrow(() -> new TokenException("Token is either expired, revoked, or invalid"));

        // Check if the token is expired or revoked
        if (token.isExpired()) {
            throw new TokenException("Token is expired");
        }
        if (token.isRevoked()) {
            throw new TokenException("Token is revoked");
        }

        // If neither expired nor revoked, return true indicating the token is valid
        return true;
    }


    @CircuitBreaker(name = "companyServiceCircuitBreaker", fallbackMethod = "companyServiceFallback")
    public AuthResponse partiallyUpdateUser(String emailId, UserRequest userRequest) {

        log.info("UserRequest : {}", userRequest);
        // Check if an ADMIN user already exists with the same companyDomain
        userRepository.findByCompanyDomainAndAdminRole(userRequest.getCompanyDomain(), Role.ADMIN)
                .ifPresent(existingAdmin -> {
                    throw new ClientException("An ADMIN user already exists with the same company domain.", HttpStatus.CONFLICT);
                });

        User user = userRepository.findByEmail(emailId).orElseThrow(() -> new UsernameNotFoundException("Email not found"));

        if (userRequest.getFullName() != null && !userRequest.getFullName().isBlank()) {
            user.setFullName(userRequest.getFullName());
        }


        boolean emailExists = false;
        boolean phoneExists = false;

        // Check if the new email already exists and belongs to another user
        if (userRequest.getEmail() != null && !userRequest.getEmail().equals(user.getEmail())) {
            emailExists = userRepository.existsByEmail(userRequest.getEmail());
        }

        // Check if the new phone number already exists and belongs to another user
        if (userRequest.getPhone() != null && !userRequest.getPhone().equals(user.getPhone())) {
            phoneExists = userRepository.existsByPhone(userRequest.getPhone());
        }

        // Handle the case where both email and phone already exist
        if (emailExists && phoneExists) {
            return AuthResponse.builder()
                    .status(HttpStatus.CONFLICT)
                    .message("User with Email: " + userRequest.getEmail() + " and Phone: " + userRequest.getPhone() + " already exist.")
                    .build();
        }

        // Handle the case where only email exists
        if (emailExists) {
            return AuthResponse.builder()
                    .status(HttpStatus.CONFLICT)
                    .message("User with Email: " + userRequest.getEmail() + " already exists.")
                    .build();
        }

        // Handle the case where only phone exists
        if (phoneExists) {
            return AuthResponse.builder()
                    .status(HttpStatus.CONFLICT)
                    .message("User with Phone: " + userRequest.getPhone() + " already exists.")
                    .build();
        }

        if (userRequest.getEmail() != null && !userRequest.getEmail().isBlank()) {
            user.setEmail(userRequest.getEmail());
        }
        if (userRequest.getPhone() != null && !userRequest.getPhone().isBlank()) {
            user.setPhone(userRequest.getPhone());
        }
        if (userRequest.getPassword() != null && !userRequest.getPassword().isBlank()) {
            user.setPassword(passwordEncoder.encode(userRequest.getPassword()));
        }
        if (userRequest.getRole() != null && !userRequest.getRole().isBlank()) {
            // Convert all roles to uppercase
            String normalizedRole = userRequest.getRole().toUpperCase();

            // Check if roles contain valid enum names
            if (!normalizedRole.contains(Role.EMPLOYEE.name()) &&
                    !normalizedRole.contains(Role.MANAGER.name()) &&
                    !normalizedRole.contains(Role.FINANCE.name()) &&
                    !normalizedRole.contains(Role.ADMIN.name())) {
                return AuthResponse.builder()
                        .message("Invalid roles provided. Allowed roles are " + Arrays.toString(Role.values()))
                        .status(HttpStatus.BAD_REQUEST)
                        .build();
            }
            user.setRole(Role.valueOf(userRequest.getRole()));
        }


        // Check for the custom header to skip company service check
        String companyServiceHeader = httpServletRequest.getHeader("X-Company-Service");

        if (companyServiceHeader != null && companyServiceHeader.equals("expense-tracker-app")) {
            user.setCompanyDomain(userRequest.getCompanyDomain());
            userRepository.save(user);

            return AuthResponse.builder()
                    .status(HttpStatus.OK)
                    .message("User Updated Successfully !")
                    .build();
        }
        // Rest Call To Company Service to check if Company exists or Not
        String url = "http://company-service/company-service/domain-name/" + userRequest.getCompanyDomain();
        boolean isCompanyExists = false;

        System.out.println("Company Service is being called.....");
        ResponseEntity<Company> responseEntity = null;
        Company company = null;
        try {
            // Get the access token from request headers
            String authHeader = httpServletRequest.getHeader(HttpHeaders.AUTHORIZATION);
            String accessToken = authHeader.substring("Bearer ".length());

            // Set up the authorization header with Bearer token
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + accessToken);

            // Create an HttpEntity object with headers (no body for GET request)
            HttpEntity<String> entity = new HttpEntity<>(headers);

            // Perform the HTTP GET request and map the response to Company
            responseEntity = this.restTemplate.exchange(url, HttpMethod.GET, entity, Company.class);
            System.out.println("Company Service has been called.....");

            if (responseEntity.getStatusCode().is2xxSuccessful()) {
                company = responseEntity.getBody();
                if (company != null) {
                    isCompanyExists = true;
                } else {
                    System.out.println("No company data found.");
                }
            } else {
                // Handle non-2xx HTTP status codes
                System.out.println("Failed to fetch company data. HTTP status: " + responseEntity.getStatusCode());
                handleNon2xxStatus((HttpStatus) responseEntity.getStatusCode());
            }
        } catch (HttpClientErrorException | HttpServerErrorException e) {
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
                String status = jsonNode.get("status").asText().substring(0, 3);

                HttpStatus httpStatus = null;
                try {
                    // Check if the extracted status is numeric, then convert it to HttpStatus
                    if (status.matches("\\d{3}")) {
                        int statusCode = Integer.parseInt(status);  // Convert to integer
                        httpStatus = HttpStatus.resolve(statusCode);  // Get HttpStatus from status code
                    } else {
                        httpStatus = HttpStatus.valueOf(status);  // If it's a name like "NOT_FOUND"
                    }
                } catch (IllegalArgumentException e2) {
                    log.error("Invalid status '{}' found", status);
                }

                log.error("Parsed message: {}", message);
                log.error("Parsed status: {}", status);
                log.error(message, httpStatus);

                // Re-throw ClientException so that it will be caught by the GlobalExceptionHandler
                throw new ClientException(message, httpStatus);
            } else {
                log.error("Failed to parse JSON response body.");
                throw new ClientException("Authorization failed, unable to parse error response", HttpStatus.INTERNAL_SERVER_ERROR);
            }
        }

        if (isCompanyExists) {
            user.setCompanyDomain(company.getDomain());
            User savedUser = userRepository.save(user);

            if (userRequest.getRole() != null || userRequest.getEmail() != null) {

                revokeAllPreviousValidTokens(savedUser);

                String accessToken = jwtUtils.generateAccessTokenWithCompanyDomain(user.getEmail(), user.getRole().name(), user.getCompanyDomain());
                String refreshToken = jwtUtils.generateRefreshTokenWithCompanyDomain(user.getEmail(), user.getRole().name(), user.getCompanyDomain());

                Token token = Token.builder()
                        .id(UUID.randomUUID().toString())
                        .user(savedUser)
                        .token(accessToken)
                        .isRevoked(false)
                        .isExpired(false)
                        .tokenType(TokenType.BEARER)
                        .build();

                tokenRepository.save(token);

                return AuthResponse.builder()
                        .accessToken(accessToken)
                        .refreshToken(refreshToken)
                        .status(HttpStatus.CREATED)
                        .build();
            }
            return AuthResponse.builder()
                    .status(HttpStatus.OK)
                    .message("User Updated Successfully !")
                    .build();
        }


        return AuthResponse.builder()
                .message("Company with domain name: " + userRequest.getCompanyDomain() + " does not exist.")
                .status(HttpStatus.NOT_FOUND)
                .build();
    }


    // Fallback method for when the company service call fails
    private AuthResponse companyServiceFallback(String emailId, UserRequest userRequest, Throwable throwable) {
        // If the cause of the failure is a ClientException, rethrow it
        if (throwable instanceof ClientException) {
            throw (ClientException) throwable;  // Propagate the original ClientException
        }
        return AuthResponse.builder()
                .message("Company service is unavailable at the moment. Please try again later.")
                .status(HttpStatus.SERVICE_UNAVAILABLE)
                .build();
    }

    // Handle non-2xx HTTP status codes
    private AuthResponse handleNon2xxStatus(HttpStatus status) {
        if (status.is4xxClientError()) {
            return AuthResponse.builder()
                    .message("Client error occurred: " + status.getReasonPhrase())
                    .status(status)
                    .build();
        } else if (status.is5xxServerError()) {
            return AuthResponse.builder()
                    .message("Server error occurred: " + status.getReasonPhrase())
                    .status(status)
                    .build();
        } else {
            return AuthResponse.builder()
                    .message("Unexpected error occurred: " + status.getReasonPhrase())
                    .status(status)
                    .build();
        }
    }


    @Override
    public AuthResponse getUserDetailsByEmailId(String emailId) {
        User user = this.userRepository.findByEmail(emailId).orElseThrow(() -> new UsernameNotFoundException("User not found !"));
        return AuthResponse.builder()
                .id(user.getId())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .phone(user.getPhone())
                .companyDomain(user.getCompanyDomain())
                .role(user.getRole().name())
                .build();
    }

    @Override
    public List<AuthResponse> getUserDetailsByCompanyDomainAndRole(String companyDomain, String role) {
        List<User> existingUser = this.userRepository.findByCompanyDomainAndRole(companyDomain, Role.valueOf(role));

        if (existingUser.isEmpty()) {
            throw new UsernameNotFoundException("User not found");
        }

        return existingUser.stream()
                .map(user -> AuthResponse.builder()
                        .id(user.getId())
                        .fullName(user.getFullName())
                        .email(user.getEmail())
                        .phone(user.getPhone())
                        .companyDomain(user.getCompanyDomain())
                        .role(user.getRole().name())
                        .build())
                .collect(Collectors.toList());
    }
}
