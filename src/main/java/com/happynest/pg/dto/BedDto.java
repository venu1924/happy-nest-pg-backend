package com.happynest.pg.dto;

import com.happynest.pg.domain.enums.BedStatus;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class BedDto {
    private String bedId;
    private String bedLabel;
    private BedStatus status;
    private String residentId;
}
