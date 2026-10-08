package com.happynest.pg.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AdminDto {
    private String id;
    private String name;
    private String role;
}
