package com.happynest.pg.controller;

import com.happynest.pg.dto.NotificationView;
import com.happynest.pg.dto.PushSubscribeRequest;
import com.happynest.pg.service.NotificationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping
    public NotificationView.ListResult list() {
        return notificationService.current();
    }

    @PostMapping("/{id}/read")
    public void markRead(@PathVariable String id) {
        notificationService.markRead(id);
    }

    @PostMapping("/read-all")
    public void markAllRead() {
        notificationService.markAllRead();
    }

    @PostMapping("/subscribe")
    public void subscribe(@Valid @RequestBody PushSubscribeRequest request) {
        notificationService.subscribe(request);
    }

    @DeleteMapping("/subscribe")
    public void unsubscribe(@RequestParam String endpoint) {
        notificationService.unsubscribe(endpoint);
    }
}
