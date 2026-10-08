package com.happynest.pg.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class DashboardDto {
    private long totalResidents;
    private long totalRooms;
    private long vacantBeds;
    private long pendingPayments;
    private PaymentSummaryDto paymentSummary;
    private List<TransferDto> recentTransfers;
}
