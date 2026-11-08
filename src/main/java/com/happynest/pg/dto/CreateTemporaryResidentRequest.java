package com.happynest.pg.dto;

import com.happynest.pg.domain.enums.SharingType;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class CreateTemporaryResidentRequest {

    @NotBlank(message = "Name is required.")
    private String name;

    @NotBlank(message = "Phone is required.")
    @Pattern(regexp = "^[6-9]\\d{9}$", message = "Enter a valid 10-digit mobile number.")
    private String phone;

    @NotNull(message = "Enter pay amount per day.")
    @DecimalMin(value = "1", message = "Enter pay amount per day.")
    private BigDecimal amountPerDay;

    @NotNull(message = "Joining date is required.")
    private LocalDate joiningDate;

    @NotNull(message = "Enter number of days stay.")
    @Min(value = 1, message = "Stay must be at least 1 day.")
    private Integer stayDays;

    @NotNull(message = "Please select a room.")
    private Integer roomNumber;

    @NotBlank(message = "Please select an available bed.")
    private String bedId;

    private SharingType sharingType;

    private String notes;
}
