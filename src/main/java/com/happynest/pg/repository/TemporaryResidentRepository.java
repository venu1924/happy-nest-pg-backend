package com.happynest.pg.repository;

import com.happynest.pg.domain.entity.TemporaryResident;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TemporaryResidentRepository extends JpaRepository<TemporaryResident, String> {

    List<TemporaryResident> findAllByOrderByJoiningDateAscNameAsc();
}
