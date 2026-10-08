package com.happynest.pg.dto;

import com.happynest.pg.domain.enums.SharingType;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class CreateResidentRequest {

    @NotBlank(message = "Name is required.")
    private String name;

    @NotBlank(message = "Phone is required.")
    @Pattern(regexp = "^[6-9]\\d{9}$", message = "Enter a valid 10-digit mobile number.")
    private String phone;

    @NotNull(message = "Joining date is required.")
    private LocalDate joiningDate;

    @NotNull(message = "Please select a room.")
    private Integer roomNumber;

    @NotBlank(message = "Please select an available bed.")
    private String bedId;

    @NotNull(message = "Enter a valid monthly rent.")
    @DecimalMin(value = "1", message = "Enter a valid monthly rent.")
    private BigDecimal monthlyRent;

    @NotNull(message = "Enter deposit amount (0 if none).")
    @DecimalMin(value = "0", message = "Enter deposit amount (0 if none).")
    private BigDecimal deposit;

    private Boolean depositRefundable = true;

    private SharingType sharingType;

    /** Triple rooms: 2 = Bed C unused / full at 2; 3 = up to 3. */
    private Integer tripleMemberLimit;

    private String notes;
}
