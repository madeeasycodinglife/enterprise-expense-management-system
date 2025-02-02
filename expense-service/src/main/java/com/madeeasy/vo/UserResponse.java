package com.madeeasy.vo;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;

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
}