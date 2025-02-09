package com.madeeasy.controller;

import com.madeeasy.dto.request.AuthRequest;
import com.madeeasy.dto.request.LogOutRequest;
import com.madeeasy.dto.request.SignInRequestDTO;
import com.madeeasy.dto.request.UserRequest;
import com.madeeasy.dto.response.AuthResponse;
import com.madeeasy.service.AuthService;
import com.madeeasy.util.ValidationUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping(path = "/auth-service")
public class AuthController {

    private final AuthService authService;

    @Tag(
            name = "Authentication",
            description = "Handles user authentication-related operations such as registration, login, logout, token validation, and token refresh. These endpoints enable secure access to the system by authenticating users with their credentials and managing their session tokens."
    )
    @Operation(
            summary = "User Registration",
            description = "Register a new user with email, password, and role.",
            tags = {"Authentication"}
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "User successfully registered"),
            @ApiResponse(responseCode = "409", description = "User already exists", content = @Content(schema = @Schema(implementation = String.class))),
            @ApiResponse(responseCode = "400", description = "Invalid request data", content = @Content(schema = @Schema(implementation = String.class)))
    })
    @PostMapping("/sign-up")
    public ResponseEntity<AuthResponse> signUp(@Valid @RequestBody AuthRequest authRequest) {
        AuthResponse response = authService.singUp(authRequest);
        return ResponseEntity.status(response.getStatus()).body(response);
    }

    @Tag(
            name = "Authentication",
            description = "Handles user authentication-related operations such as registration, login, logout, token validation, and token refresh. These endpoints enable secure access to the system by authenticating users with their credentials and managing their session tokens."
    )
    @Operation(
            summary = "User Login",
            description = "Authenticate user with email and password.",
            tags = {"Authentication"}
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "User authenticated successfully"),
            @ApiResponse(responseCode = "401", description = "Invalid credentials")
    })
    @PostMapping("/sign-in")
    public ResponseEntity<AuthResponse> signIn(@Valid @RequestBody SignInRequestDTO request) {
        return ResponseEntity.ok(authService.singIn(request));
    }

    @Tag(
            name = "Authentication",
            description = "Handles user authentication-related operations such as registration, login, logout, token validation, and token refresh. These endpoints enable secure access to the system by authenticating users with their credentials and managing their session tokens."
    )
    @Operation(
            summary = "User Logout",
            description = "Logs out the currently authenticated user.",
            tags = {"Authentication"}
    )
    @SecurityRequirement(name = "Bearer Authentication")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Successfully logged out")
    })
    @PostMapping("/log-out")
    public ResponseEntity<String> logOut(@Valid @RequestBody LogOutRequest logOutRequest) {
        authService.logOut(logOutRequest);
        return ResponseEntity.ok("Logged out successfully");
    }

    @Tag(
            name = "Authentication",
            description = "Handles user authentication-related operations such as registration, login, logout, token validation, and token refresh. These endpoints enable secure access to the system by authenticating users with their credentials and managing their session tokens."
    )
    @Operation(
            summary = "Refresh Access Token",
            description = "Refreshes the user's access token using a valid refresh token.",
            tags = {"Authentication"}
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Token refreshed successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid refresh token")
    })
    @PostMapping("/refresh-token/{refreshToken}")
    public ResponseEntity<?> refreshToken(@PathVariable String refreshToken) {
        Map<String, String> validationErrors = ValidationUtils.validateRefreshToken(refreshToken);
        if (!validationErrors.isEmpty()) {
            return ResponseEntity.badRequest().body(validationErrors);
        }
        return ResponseEntity.ok(authService.refreshToken(refreshToken));
    }

    @Tag(
            name = "Authentication",
            description = "Handles user authentication-related operations such as registration, login, logout, token validation, and token refresh. These endpoints enable secure access to the system by authenticating users with their credentials and managing their session tokens."
    )
    @Operation(
            summary = "Validate Access Token",
            description = "Checks if the given access token is valid.",
            tags = {"Authentication"}
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Token is valid"),
            @ApiResponse(responseCode = "401", description = "Token is invalid or expired")
    })
    @PostMapping("/validate-access-token/{accessToken}")
    public ResponseEntity<Boolean> validateAccessToken(@PathVariable String accessToken) {
        Map<String, String> validationErrors = ValidationUtils.validateAccessToken(accessToken);
        if (!validationErrors.isEmpty()) {
            return ResponseEntity.badRequest().body(false);
        }
        return authService.validateAccessToken(accessToken)
                ? ResponseEntity.ok(true)
                : ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(false);
    }

    @Tag(
            name = "User Management",
            description = "Manages user-related operations such as fetching user details, updating profiles, and retrieving users based on company and role. These endpoints allow administrators to handle user profiles, including partial updates and retrieval of users for a specific company or role."
    )
    @Operation(
            summary = "Update User Profile",
            description = "Partially updates the user profile based on email ID.",
            tags = {"User Management"}
    )
    @SecurityRequirement(name = "Bearer Authentication")
    @PatchMapping("/partial-update/{emailId}")
    public ResponseEntity<AuthResponse> updateUserProfile(
            @Parameter(description = "User's email ID", required = true)
            @PathVariable String emailId,
            @Valid @RequestBody UserRequest userRequest) {

        Map<String, String> validationErrors = ValidationUtils.validateEmail(emailId);
        if (!validationErrors.isEmpty()) {
            return ResponseEntity.badRequest().body(null);
        }

        AuthResponse response = authService.partiallyUpdateUser(emailId, userRequest);
        return ResponseEntity.status(response.getStatus()).body(response);
    }

    @Tag(
            name = "User Management",
            description = "Manages user-related operations such as fetching user details, updating profiles, and retrieving users based on company and role. These endpoints allow administrators to handle user profiles, including partial updates and retrieval of users for a specific company or role."
    )
    @Operation(
            summary = "Get User Details",
            description = "Fetch user details by email ID.",
            tags = {"User Management"}
    )
    @SecurityRequirement(name = "Bearer Authentication")
    @GetMapping("/get-user/{emailId}")
    public ResponseEntity<?> getUser(@PathVariable String emailId) {
        Map<String, String> validationErrors = ValidationUtils.validateEmail(emailId);
        if (!validationErrors.isEmpty()) {
            return ResponseEntity.badRequest().body(validationErrors);
        }
        return ResponseEntity.ok(authService.getUserDetailsByEmailId(emailId));
    }

    @Tag(
            name = "User Management",
            description = "Manages user-related operations such as fetching user details, updating profiles, and retrieving users based on company and role. These endpoints allow administrators to handle user profiles, including partial updates and retrieval of users for a specific company or role."
    )
    @Operation(
            summary = "Get Users by Company and Role",
            description = "Fetch users based on company domain and role.",
            tags = {"User Management"}
    )
    @SecurityRequirement(name = "Bearer Authentication")
    @GetMapping("/get-user/{companyDomain}/{role}")
    public ResponseEntity<?> getUsersByCompanyAndRole(
            @Parameter(description = "Company domain", required = true)
            @PathVariable String companyDomain,
            @Parameter(description = "User role", required = true)
            @PathVariable String role) {
        return ResponseEntity.ok(authService.getUserDetailsByCompanyDomainAndRole(companyDomain, role.toUpperCase()));
    }
}
