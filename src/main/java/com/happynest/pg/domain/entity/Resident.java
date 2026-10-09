package com.happynest.pg.domain.entity;

import com.happynest.pg.domain.enums.SharingType;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "residents")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Resident {

    @Id
    @Column(length = 64)
    private String id;

    @Column(nullable = false, length = 120)
    private String name;

    @Column(nullable = false, length = 15)
    private String phone;

    @Column(nullable = false)
    private LocalDate joiningDate;

    @Column(nullable = false)
    private Integer roomNumber;

    @Column(nullable = false, length = 32)
    private String bedId;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal monthlyRent;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal deposit;

    /** Amount returned on leaving (0 = non-refundable). Null on old rows → derive from deposit. */
    @Column(precision = 12, scale = 2)
    private BigDecimal refundAmount;

    @Column(nullable = false)
    private boolean depositRefundable;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private SharingType sharingType;

    @Column(length = 500)
    private String notes;
}
