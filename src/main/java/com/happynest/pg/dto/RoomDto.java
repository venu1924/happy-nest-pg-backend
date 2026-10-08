package com.happynest.pg.dto;

import com.happynest.pg.domain.enums.OccupancyMode;
import com.happynest.pg.domain.enums.RoomType;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class RoomDto {
    private Integer roomNumber;
    private Integer floorNumber;
    private RoomType type;
    private Integer capacity;
    private OccupancyMode occupancyMode;
    private Integer tripleMemberLimit;
    private List<BedDto> beds;
}
