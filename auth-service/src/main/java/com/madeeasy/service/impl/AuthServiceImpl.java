package com.madeeasy.service.impl;

import com.madeeasy.dto.request.AuthRequest;
import com.madeeasy.dto.request.LogOutRequest;
import com.madeeasy.dto.request.SignInRequestDTO;
import com.madeeasy.dto.request.UserRequest;
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
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

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
                .build();
    }

    public AuthResponse singIn(SignInRequestDTO signInRequest) {
        Authentication authentication = authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(signInRequest.getEmail(), signInRequest.getPassword()));
        if (authentication.isAuthenticated()) {
            User user = userRepository.findByEmail(signInRequest.getEmail()).orElseThrow(() -> new UsernameNotFoundException("Email not found"));
            revokeAllPreviousValidTokens(user);
            String accessToken = jwtUtils.generateAccessToken(user.getEmail(), user.getRole().name());
            String refreshToken = jwtUtils.generateRefreshToken(user.getEmail(), user.getRole().name());


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
            throw new RuntimeException("Bad Credential Exception !!");
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
                .orElseThrow(() -> new IllegalArgumentException("Token not found or is expired/revoked"));

        revokeAllPreviousValidTokens(user);
    }

    public AuthResponse refreshToken(String refreshToken) {

        boolean isValid = jwtUtils.validateToken(refreshToken, jwtUtils.getUserName(refreshToken));

        if (!isValid) {
            throw new RuntimeException("Token is invalid");
        }
        User user = userRepository.findByEmail(jwtUtils.getUserName(refreshToken)).orElseThrow(() -> new UsernameNotFoundException("Email not found"));
        revokeAllPreviousValidTokens(user);
        String accessToken = jwtUtils.generateAccessToken(user.getEmail(), user.getRole().name());
        String newRefreshToken = jwtUtils.generateRefreshToken(user.getEmail(), user.getRole().name());


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
                .orElseThrow(() -> new IllegalArgumentException("Token is either expired, revoked, or invalid"));

        // Check if the token is expired or revoked
        if (token.isExpired()) {
            throw new RuntimeException("Token is expired");
        }
        if (token.isRevoked()) {
            throw new RuntimeException("Token is revoked");
        }

        // If neither expired nor revoked, return true indicating the token is valid
        return true;
    }

    public AuthResponse partiallyUpdateUser(String emailId, UserRequest userRequest) {

        log.info("UserRequest : {}", userRequest);

        User user = userRepository.findByEmail(emailId).orElseThrow(() -> new UsernameNotFoundException("Email not found"));

        if (userRequest.getFullName() != null) {
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

        if (userRequest.getEmail() != null) {
            user.setEmail(userRequest.getEmail());
        }
        if (userRequest.getPhone() != null) {
            user.setPhone(userRequest.getPhone());
        }
        if (userRequest.getPassword() != null) {
            user.setPassword(passwordEncoder.encode(userRequest.getPassword()));
        }
        if (userRequest.getRole() != null) {
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


        User savedUser = userRepository.save(user);

        if (userRequest.getRole() != null || userRequest.getEmail() != null) {
            revokeAllPreviousValidTokens(savedUser);
            String accessToken = jwtUtils.generateAccessToken(savedUser.getEmail(), savedUser.getRole().name());
            String refreshToken = jwtUtils.generateRefreshToken(savedUser.getEmail(), savedUser.getRole().name());


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
                    .build();
        }

        return AuthResponse.builder()
                .status(HttpStatus.OK)
                .message("User updated successfully")
                .build();
    }
}
