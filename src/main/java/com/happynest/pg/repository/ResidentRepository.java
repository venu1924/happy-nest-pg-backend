package com.happynest.pg.repository;

import com.happynest.pg.domain.entity.Resident;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ResidentRepository extends JpaRepository<Resident, String> {

    List<Resident> findAllByOrderByNameAsc();

    long count();
}
