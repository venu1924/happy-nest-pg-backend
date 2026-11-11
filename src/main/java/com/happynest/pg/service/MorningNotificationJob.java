package com.happynest.pg.service;

import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class MorningNotificationJob {

    private final NotificationService notificationService;

    @Scheduled(cron = "0 0 8 * * *", zone = "Asia/Kolkata")
    public void morning() {
        notificationService.refresh();
    }
}
