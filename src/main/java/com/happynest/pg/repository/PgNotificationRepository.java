package com.happynest.pg.repository;

import com.happynest.pg.domain.entity.PgNotification;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PgNotificationRepository extends JpaRepository<PgNotification, String> {

    boolean existsByDedupeKey(String dedupeKey);

    List<PgNotification> findTop50ByOrderByCreatedAtDesc();

    long countBySeenFalse();
}
