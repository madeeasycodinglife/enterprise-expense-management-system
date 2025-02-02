package com.madeeasy.service.impl;

import com.madeeasy.dto.request.AuthRequest;
import com.madeeasy.dto.response.AuthResponse;
import com.madeeasy.entity.Role;
import com.madeeasy.entity.Token;
import com.madeeasy.entity.TokenType;
import com.madeeasy.entity.User;
import com.madeeasy.repository.TokenRepository;
import com.madeeasy.repository.UserRepository;
import com.madeeasy.service.AuthService;
import com.madeeasy.util.JwtUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.UUID;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final JwtUtils jwtUtils;
    private final TokenRepository tokenRepository;

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

        // Rest Call To Company Service to check if Company exists or Not
        String url = "http://localhost:8082/company-service/domain-name/" + authRequest.getCompanyDomain();
        ResponseEntity<Company> responseEntity = null;
        boolean isCompanyFound = false;
        try {
            // Perform the HTTP GET request and map the response to Company
            responseEntity = this.restTemplate.exchange(url, HttpMethod.GET, null, Company.class);

            if (responseEntity.getStatusCode().is2xxSuccessful()) {
                Company company = responseEntity.getBody();

                if (company != null) {
                    // Do something with the company object
                    isCompanyFound = true;
                } else {
                    // Handle the case where the body is null (company not found or empty response)
                    System.out.println("No company data found.");
                }
            } else {
                // Handle non-2xx HTTP status codes (error responses from the server)
                System.out.println("Failed to fetch company data. HTTP status: " + responseEntity.getStatusCode());
            }
        } catch (HttpClientErrorException | HttpServerErrorException e) {
            // Handle specific HTTP client/server errors
            System.out.println("HTTP error occurred: " + e.getMessage());
        } catch (Exception e) {
            // Handle other unexpected exceptions
            System.out.println("An error occurred: " + e.getMessage());
        }

        if (isCompanyFound) {
            User user = User.builder()
                    .fullName(authRequest.getFullName())
                    .email(authRequest.getEmail())
                    .password(authRequest.getPassword())
                    .companyDomain(authRequest.getCompanyDomain())
                    .phone(authRequest.getPhone())
                    .isAccountNonExpired(true)
                    .isAccountNonLocked(true)
                    .isCredentialsNonExpired(true)
                    .isEnabled(true)
                    .role(Role.valueOf(normalizedRole))
                    .build();


            // Check if a user with the given email or phone already exists
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


            String accessToken = jwtUtils.generateAccessTokenWithCompanyDomain(user.getEmail(), normalizedRole, authRequest.getCompanyDomain());
            String refreshToken = jwtUtils.generateRefreshTokenWithCompanyDomain(user.getEmail(), normalizedRole, authRequest.getCompanyDomain());

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
                    .build();
        }

        return AuthResponse.builder()
                .message("Company with domain name: " + authRequest.getCompanyDomain() + " not found.")
                .status(HttpStatus.NOT_FOUND)
                .build();
    }

}
