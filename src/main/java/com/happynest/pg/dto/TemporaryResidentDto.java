package com.happynest.pg.dto;

import com.happynest.pg.domain.enums.PaymentMethod;
import com.happynest.pg.domain.enums.PaymentStatus;
import com.happynest.pg.domain.enums.SharingType;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
public class TemporaryResidentDto {
    private String id;
    private String name;
    private String phone;
    private BigDecimal amountPerDay;
    private LocalDate joiningDate;
    private Integer stayDays;
    private LocalDate checkoutDate;
    private Integer daysLeft;
    private boolean vacateToday;
    private Integer roomNumber;
    private String bedId;
    private SharingType sharingType;
    private BigDecimal amount;
    private PaymentStatus paymentStatus;
    private PaymentMethod paymentMethod;
    private LocalDate paidDate;
    private String receivedBy;
    private String notes;
    private String stayType;
}
