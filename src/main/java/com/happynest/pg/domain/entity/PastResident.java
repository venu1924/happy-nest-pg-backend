package com.happynest.pg.domain.entity;

import com.happynest.pg.domain.enums.SharingType;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "past_residents")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PastResident {

    @Id
    @Column(length = 64)
    private String id;

    @Column(nullable = false, length = 120)
    private String name;

    @Column(nullable = false, length = 15)
    private String phone;

    private LocalDate joiningDate;

    private LocalDate checkoutDate;

    private Integer lastRoomNumber;

    @Column(length = 32)
    private String lastBedId;

    @Column(precision = 12, scale = 2)
    private BigDecimal monthlyRent;

    @Column(precision = 12, scale = 2)
    private BigDecimal deposit;

    private Boolean depositRefundable;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private SharingType sharingType;

    @Column(length = 500)
    private String notes;

    @Column(length = 500)
    private String deletionReason;
}
