package com.happynest.pg.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AskResponse {
    private String answer;
}
