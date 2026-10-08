package com.happynest.pg.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;

@Data
@Builder
public class TransferDto {
    private String id;
    private String residentId;
    private Integer fromRoomNumber;
    private String fromBedId;
    private Integer toRoomNumber;
    private String toBedId;
    private LocalDate transferDate;
    private String note;
    private String residentName;
}
