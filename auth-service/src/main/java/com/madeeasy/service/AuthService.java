package com.madeeasy.service;

import com.madeeasy.dto.request.AuthRequest;
import com.madeeasy.dto.response.AuthResponse;

public interface AuthService {
    AuthResponse singUp(AuthRequest authRequest);
}
