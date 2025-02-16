package com.madeeasy.service;

import com.madeeasy.dto.request.*;
import com.madeeasy.dto.response.AuthResponse;
import com.madeeasy.entity.User;

import java.util.List;

public interface AuthService {
    AuthResponse singUp(AuthRequest authRequest);

    AuthResponse singIn(SignInRequestDTO signInRequest);

    void revokeAllPreviousValidTokens(User user);

    void logOut(LogOutRequest logOutRequest);

    AuthResponse refreshToken(String refreshToken);

    boolean validateAccessToken(String accessToken);

    AuthResponse partiallyUpdateUser(String emailId, UserRequest userRequest);

    AuthResponse getUserDetailsByEmailId(String emailId);

    void updateEmployeeDomains(DomainUpdateRequest domainUpdateRequest);
    List<AuthResponse> getUserDetailsByCompanyDomainAndRole(String companyDomain, String role);
}
