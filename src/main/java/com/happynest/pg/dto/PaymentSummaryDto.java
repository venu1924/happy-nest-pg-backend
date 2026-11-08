package com.happynest.pg.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PaymentSummaryDto {
    private int month;
    private int year;
    private long paid;
    private long pending;
    private long total;
    private long temporaryPaid;
    private long temporaryUnpaid;
}
