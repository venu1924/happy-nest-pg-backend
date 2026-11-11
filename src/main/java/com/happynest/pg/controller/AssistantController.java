package com.happynest.pg.controller;

import com.happynest.pg.dto.AskRequest;
import com.happynest.pg.dto.AskResponse;
import com.happynest.pg.service.AssistantService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/assistant")
@RequiredArgsConstructor
public class AssistantController {

    private final AssistantService assistantService;

    @PostMapping("/ask")
    public AskResponse ask(@Valid @RequestBody AskRequest request) {
        return AskResponse.builder()
                .answer(assistantService.ask(request.getQuestion()))
                .build();
    }
}
