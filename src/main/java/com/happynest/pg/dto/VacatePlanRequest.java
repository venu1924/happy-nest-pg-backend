package com.happynest.pg.dto;

import lombok.Data;

import java.time.LocalDate;

@Data
public class VacatePlanRequest {
    private LocalDate vacateDate;
}
