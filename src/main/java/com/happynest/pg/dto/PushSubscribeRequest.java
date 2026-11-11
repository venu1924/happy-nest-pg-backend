package com.happynest.pg.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class PushSubscribeRequest {

    @NotBlank
    private String endpoint;

    @NotBlank
    private String p256dh;

    @NotBlank
    private String auth;
}
