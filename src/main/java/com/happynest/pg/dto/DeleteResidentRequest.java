package com.happynest.pg.dto;

import lombok.Data;

@Data
public class DeleteResidentRequest {
    /** Optional leave reason (frontend sends as notes). */
    private String notes;
}
