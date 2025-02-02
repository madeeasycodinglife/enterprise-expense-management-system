package com.madeeasy.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CompanyResponseDTO {
    private String name;
    private String domain;
}
