package com.happynest.pg.dto;

import com.happynest.pg.domain.enums.BedStatus;
import com.happynest.pg.domain.enums.OccupantType;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class BedDto {
    private String bedId;
    private String bedLabel;
    private BedStatus status;
    private String residentId;
    private OccupantType occupantType;
}
