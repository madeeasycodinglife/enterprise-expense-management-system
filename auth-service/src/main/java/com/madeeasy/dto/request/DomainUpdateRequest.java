package com.madeeasy.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class DomainUpdateRequest {
    @NotBlank(message = "oldDomain Cannot be Blank")
    private String oldDomain;
    @NotBlank(message = "newDomain Cannot be Blank")
    private String newDomain;
}
