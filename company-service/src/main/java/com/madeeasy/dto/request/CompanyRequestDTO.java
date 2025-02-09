package com.madeeasy.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

@Data
public class CompanyRequestDTO {

    @NotBlank(message = "Company name is required and cannot be blank")
    private String name;

    @NotBlank(message = "Company domain is required and cannot be blank")
    private String domain;

    @NotNull(message = "Auto-approve threshold is required")
    @Positive(message = "Auto-approve threshold must be a positive value")
    private Double autoApproveThreshold;
}
