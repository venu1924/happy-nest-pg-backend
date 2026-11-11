package com.happynest.pg.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
public class AskRequest {

    @NotBlank(message = "Type a question.")
    @Size(max = 500, message = "Keep the question under 500 characters.")
    private String question;

    @Size(max = 8)
    private List<ChatLine> history;

    @Data
    public static class ChatLine {
        private String role;
        private String text;
    }
}
