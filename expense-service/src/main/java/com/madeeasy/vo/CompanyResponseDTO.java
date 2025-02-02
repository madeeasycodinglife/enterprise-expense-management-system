package com.madeeasy.vo;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CompanyResponseDTO {
    private Long id;
    private String name;
    private String domain;
}
