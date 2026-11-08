package com.happynest.pg.repository;

import com.happynest.pg.domain.entity.Payment;
import com.happynest.pg.domain.enums.PaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PaymentRepository extends JpaRepository<Payment, String> {

    List<Payment> findByMonthAndYear(Integer month, Integer year);

    long countByMonthAndYearAndStatus(Integer month, Integer year, PaymentStatus status);

    boolean existsByResidentIdAndMonthAndYear(String residentId, Integer month, Integer year);

    void deleteByResidentId(String residentId);
}
