package com.happynest.pg.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ExtendStayRequest {

    @NotNull(message = "Enter extra days.")
    @Min(value = 1, message = "Extension must be at least 1 day.")
    private Integer extraDays;
}
