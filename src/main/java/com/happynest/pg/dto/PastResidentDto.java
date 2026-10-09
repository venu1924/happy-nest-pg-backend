package com.happynest.pg.dto;

import com.happynest.pg.domain.enums.SharingType;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
public class PastResidentDto {
    private String id;
    private String name;
    private String phone;
    private LocalDate joiningDate;
    private LocalDate checkoutDate;
    private Integer lastRoomNumber;
    private String lastBedId;
    private BigDecimal monthlyRent;
    private BigDecimal deposit;
    private BigDecimal refundAmount;
    private Boolean depositRefundable;
    private SharingType sharingType;
    private String notes;
    private String deletionReason;
}
