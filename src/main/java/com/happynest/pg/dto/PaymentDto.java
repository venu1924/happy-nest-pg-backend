package com.happynest.pg.dto;

import com.happynest.pg.domain.enums.PaymentMethod;
import com.happynest.pg.domain.enums.PaymentStatus;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
public class PaymentDto {
    private String id;
    private String residentId;
    private Integer month;
    private Integer year;
    private BigDecimal amount;
    private PaymentStatus status;
    private PaymentMethod paymentMethod;
    private LocalDate paidDate;
    private String receivedBy;
    private String residentName;
    private Integer roomNumber;
    private String phone;
}
