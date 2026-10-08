package com.happynest.pg.repository;

import com.happynest.pg.domain.entity.Bed;
import com.happynest.pg.domain.enums.BedStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface BedRepository extends JpaRepository<Bed, String> {

    Optional<Bed> findByBedId(String bedId);

    long countByStatus(BedStatus status);
}
