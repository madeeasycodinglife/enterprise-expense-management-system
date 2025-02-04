package com.madeeasy.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ExpenseTrend {

    private Integer month;
    private Integer year;
    private BigDecimal totalAmount;
}
