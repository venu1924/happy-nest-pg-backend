package com.happynest.pg.domain.entity;

import com.happynest.pg.domain.enums.PaymentMethod;
import com.happynest.pg.domain.enums.PaymentStatus;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(
        name = "payments",
        indexes = {
                @Index(name = "idx_payments_month_year", columnList = "year, month"),
                @Index(name = "idx_payments_resident", columnList = "residentId")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Payment {

    @Id
    @Column(length = 80)
    private String id;

    @Column(nullable = false, length = 64)
    private String residentId;

    @Column(nullable = false)
    private Integer month;

    @Column(nullable = false)
    private Integer year;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PaymentStatus status;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private PaymentMethod paymentMethod;

    private LocalDate paidDate;

    @Column(length = 80)
    private String receivedBy;
}
