package com.madeeasy.service.impl;

import com.madeeasy.dto.request.AuthRequest;
import com.madeeasy.dto.request.SignInRequestDTO;
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
}
