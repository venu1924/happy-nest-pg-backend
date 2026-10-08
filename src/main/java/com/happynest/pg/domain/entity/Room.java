package com.happynest.pg.domain.entity;

import com.happynest.pg.domain.enums.OccupancyMode;
import com.happynest.pg.domain.enums.RoomType;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "rooms")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Room {

    @Id
    @Column(name = "room_number")
    private Integer roomNumber;

    @Column(nullable = false)
    private Integer floorNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private RoomType type;

    @Column(nullable = false)
    private Integer capacity;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private OccupancyMode occupancyMode;

    /** Triple rooms only: 2 = Bed C blocked / full at 2; 3 = normal. Null for non-triple. */
    private Integer tripleMemberLimit;

    @OneToMany(mappedBy = "room", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("bedLabel ASC")
    @Builder.Default
    private List<Bed> beds = new ArrayList<>();
}
