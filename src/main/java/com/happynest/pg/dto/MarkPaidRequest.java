package com.happynest.pg.dto;

import com.happynest.pg.domain.enums.PaymentMethod;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

@Data
public class MarkPaidRequest {

    @NotNull(message = "Payment method is required.")
    private PaymentMethod paymentMethod;

    private String receivedBy = "Admin";

    private LocalDate paidDate;
}
