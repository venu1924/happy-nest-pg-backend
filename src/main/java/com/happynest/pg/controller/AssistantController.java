package com.happynest.pg.controller;

import com.happynest.pg.dto.AskRequest;
import com.happynest.pg.service.AssistantService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;

@RestController
@RequestMapping("/api/assistant")
@RequiredArgsConstructor
public class AssistantController {

    private final AssistantService assistantService;

    @PostMapping(value = "/ask", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter ask(@Valid @RequestBody AskRequest request) {
        SseEmitter emitter = new SseEmitter(120_000L);
        assistantService.stream(request.getQuestion(), request.getHistory()).subscribe(
                token -> {
                    try {
                        emitter.send(SseEmitter.event().data(token));
                    } catch (IOException ex) {
                        emitter.completeWithError(ex);
                    }
                },
                emitter::completeWithError,
                emitter::complete
        );
        return emitter;
    }
}
