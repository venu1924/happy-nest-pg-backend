package com.happynest.pg.dto;

import com.happynest.pg.domain.enums.SharingType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

@Data
public class TransferRequest {

    @NotNull(message = "Select a room with a vacant bed.")
    private Integer toRoomNumber;

    @NotBlank(message = "Please select an available bed.")
    private String toBedId;

    private SharingType sharingType;

    private Integer tripleMemberLimit;

    @NotNull(message = "Transfer date is required.")
    private LocalDate transferDate;

    private String note;
}
