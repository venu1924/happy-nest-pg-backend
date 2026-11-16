package com.happynest.pg.service;

import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class MorningNotificationJob {

    private final NotificationService notificationService;

    @Scheduled(cron = "0 0 9 * * *", zone = "Asia/Kolkata")
    public void morning() {
        notificationService.refresh();
    }

    @Scheduled(cron = "0 0 19 * * *", zone = "Asia/Kolkata")
    public void evening() {
        notificationService.refresh();
    }
}
