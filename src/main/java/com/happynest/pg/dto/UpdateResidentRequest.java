package com.happynest.pg.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class UpdateResidentRequest {

    @NotBlank(message = "Name is required.")
    private String name;

    @NotBlank(message = "Phone is required.")
    @Pattern(regexp = "^[6-9]\\d{9}$", message = "Enter a valid 10-digit mobile number.")
    private String phone;

    @NotNull(message = "Joining date is required.")
    private LocalDate joiningDate;

    @NotNull(message = "Enter a valid monthly rent.")
    @DecimalMin(value = "1", message = "Enter a valid monthly rent.")
    private BigDecimal monthlyRent;

    @NotNull(message = "Enter deposit amount (0 if none).")
    @DecimalMin(value = "0", message = "Enter deposit amount (0 if none).")
    private BigDecimal deposit;

    private Boolean depositRefundable = true;

    private String notes;
}
