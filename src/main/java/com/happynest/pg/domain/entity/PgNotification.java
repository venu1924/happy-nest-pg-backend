package com.happynest.pg.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "pg_notifications")
@Getter
@Setter
@NoArgsConstructor
public class PgNotification {

    @Id
    @Column(length = 64)
    private String id;

    @Column(nullable = false, unique = true, length = 180)
    private String dedupeKey;

    @Column(nullable = false, length = 500)
    private String message;

    @Column(nullable = false)
    private LocalDate noticeDate;

    @Column(nullable = false)
    private boolean seen;

    @Column(nullable = false)
    private Instant createdAt;
}
