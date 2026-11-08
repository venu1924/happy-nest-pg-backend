package com.happynest.pg.domain.entity;

import com.happynest.pg.domain.enums.PaymentMethod;
import com.happynest.pg.domain.enums.PaymentStatus;
import com.happynest.pg.domain.enums.SharingType;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "temporary_residents")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TemporaryResident {

    @Id
    @Column(length = 64)
    private String id;

    @Column(nullable = false, length = 120)
    private String name;

    @Column(nullable = false, length = 15)
    private String phone;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amountPerDay;

    @Column(nullable = false)
    private LocalDate joiningDate;

    @Column(nullable = false)
    private Integer stayDays;

    @Column(nullable = false)
    private Integer roomNumber;

    @Column(nullable = false, length = 32)
    private String bedId;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private SharingType sharingType;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PaymentStatus paymentStatus;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private PaymentMethod paymentMethod;

    private LocalDate paidDate;

    @Column(length = 80)
    private String receivedBy;

    @Column(length = 500)
    private String notes;

    public LocalDate checkoutDate() {
        if (joiningDate == null || stayDays == null || stayDays < 1) {
            return joiningDate;
        }
        return joiningDate.plusDays(stayDays - 1L);
    }
}
