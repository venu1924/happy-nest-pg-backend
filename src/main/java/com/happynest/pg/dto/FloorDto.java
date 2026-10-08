package com.happynest.pg.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class FloorDto {
    private Integer floorNumber;
    private List<RoomDto> rooms;
}
