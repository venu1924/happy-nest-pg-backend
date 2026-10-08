package com.happynest.pg.repository;

import com.happynest.pg.domain.entity.Transfer;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TransferRepository extends JpaRepository<Transfer, String> {

    List<Transfer> findAllByOrderByTransferDateDesc();
}
