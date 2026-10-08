package com.happynest.pg.domain.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Entity
@Table(name = "transfers")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Transfer {

    @Id
    @Column(length = 64)
    private String id;

    @Column(nullable = false, length = 64)
    private String residentId;

    @Column(nullable = false)
    private Integer fromRoomNumber;

    @Column(nullable = false, length = 32)
    private String fromBedId;

    @Column(nullable = false)
    private Integer toRoomNumber;

    @Column(nullable = false, length = 32)
    private String toBedId;

    @Column(nullable = false)
    private LocalDate transferDate;

    @Column(length = 500)
    private String note;
}
