package com.happynest.pg.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class DashboardDto {
    private long totalResidents;
    private long temporaryResidents;
    private long totalRooms;
    private long vacantBeds;
    private long vacantBedsPT;
    private long pendingPayments;
    private PaymentSummaryDto paymentSummary;
    private List<TransferDto> recentTransfers;
    private List<TemporaryResidentDto> temporaryStayAlerts;
}
