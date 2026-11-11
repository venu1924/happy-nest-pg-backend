package com.happynest.pg.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class AskRequest {

    @NotBlank(message = "Type a question.")
    @Size(max = 500, message = "Keep the question under 500 characters.")
    private String question;
}
