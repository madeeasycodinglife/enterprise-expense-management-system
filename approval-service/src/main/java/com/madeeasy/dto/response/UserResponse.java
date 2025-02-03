package com.madeeasy.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;
import org.springframework.http.HttpStatus;

import java.io.Serializable;

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UserResponse implements Serializable {
    private Long id;
    private String fullName;
    private String email;
    private String companyDomain;
    private String phone;
    private String role;
    private String accessToken;
    private String refreshToken;
    private HttpStatus status;
    private String message;
}