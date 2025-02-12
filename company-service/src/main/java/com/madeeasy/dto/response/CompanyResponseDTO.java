package com.madeeasy.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CompanyResponseDTO {
    private Long id;
    private String emailId;
    private String name;
    private String domain;
    private Double autoApproveThreshold;
}
