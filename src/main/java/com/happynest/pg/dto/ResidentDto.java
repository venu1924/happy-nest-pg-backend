package com.happynest.pg.dto;

import com.happynest.pg.domain.enums.SharingType;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
public class ResidentDto {
    private String id;
    private String name;
    private String phone;
    private LocalDate joiningDate;
    private Integer roomNumber;
    private String bedId;
    private BigDecimal monthlyRent;
    private BigDecimal deposit;
    private Boolean depositRefundable;
    private SharingType sharingType;
    private String notes;
}
