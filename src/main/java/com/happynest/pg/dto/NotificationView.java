package com.happynest.pg.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class NotificationView {

    private final String id;
    private final String message;
    private final String noticeDate;
    private final boolean read;
    private final String createdAt;

    @Getter
    @Builder
    public static class ListResult {
        private final List<NotificationView> items;
        private final long unread;
        private final String publicKey;
        private final String morningTime;
    }
}
