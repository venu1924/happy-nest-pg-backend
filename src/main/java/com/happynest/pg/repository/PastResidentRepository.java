package com.happynest.pg.repository;

import com.happynest.pg.domain.entity.PastResident;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PastResidentRepository extends JpaRepository<PastResident, String> {

    List<PastResident> findAllByOrderByCheckoutDateDesc();
}
