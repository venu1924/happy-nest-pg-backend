package com.happynest.pg.service;

import com.happynest.pg.domain.entity.Bed;
import com.happynest.pg.domain.enums.BedStatus;
import com.happynest.pg.domain.enums.OccupantType;
import com.happynest.pg.domain.enums.PaymentStatus;
import com.happynest.pg.dto.AdminDto;
import com.happynest.pg.dto.DashboardDto;
import com.happynest.pg.dto.PaymentSummaryDto;
import com.happynest.pg.dto.TemporaryResidentDto;
import com.happynest.pg.dto.TransferDto;
import com.happynest.pg.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DashboardService {

    private final ResidentRepository residentRepository;
    private final RoomRepository roomRepository;
    private final BedRepository bedRepository;
    private final PaymentRepository paymentRepository;
    private final TransferRepository transferRepository;
    private final PastResidentRepository pastResidentRepository;
    private final TemporaryResidentRepository temporaryResidentRepository;
    private final PaymentService paymentService;
    private final AuthService authService;

    @Transactional
    public DashboardDto getDashboard() {
        LocalDate now = LocalDate.now();
        int month = now.getMonthValue();
        int year = now.getYear();
        paymentService.ensureMonthPayments(month, year);

        long paid = paymentRepository.countByMonthAndYearAndStatus(month, year, PaymentStatus.PAID);
        long pending = paymentRepository.countByMonthAndYearAndStatus(month, year, PaymentStatus.PENDING);

        List<Bed> beds = bedRepository.findAll();
        long vacantPT = beds.stream().filter(b -> b.getStatus() == BedStatus.VACANT).count();
        long vacantIgnoringTemp = beds.stream()
                .filter(b -> b.getStatus() == BedStatus.VACANT
                        || (b.getStatus() == BedStatus.OCCUPIED && b.getOccupantType() == OccupantType.TEMPORARY))
                .count();

        List<TemporaryResidentDto> temps = temporaryResidentRepository.findAllByOrderByJoiningDateAscNameAsc().stream()
                .map(DtoMapper::toTemporaryResidentDto)
                .sorted(Comparator.comparingInt(TemporaryResidentDto::getDaysLeft))
                .toList();
        long tempUnpaid = temps.stream().filter(t -> t.getPaymentStatus() == PaymentStatus.PENDING).count();
        long tempPaid = temps.stream().filter(t -> t.getPaymentStatus() == PaymentStatus.PAID).count();

        List<TransferDto> recent = transferRepository.findAllByOrderByTransferDateDesc().stream()
                .limit(5)
                .map(t -> DtoMapper.toTransferDto(t, resolveName(t.getResidentId())))
                .toList();

        return DashboardDto.builder()
                .totalResidents(residentRepository.count())
                .temporaryResidents(temps.size())
                .totalRooms(roomRepository.count())
                .vacantBeds(vacantIgnoringTemp)
                .vacantBedsPT(vacantPT)
                .pendingPayments(pending)
                .paymentSummary(PaymentSummaryDto.builder()
                        .month(month)
                        .year(year)
                        .paid(paid)
                        .pending(pending)
                        .total(paid + pending)
                        .temporaryPaid(tempPaid)
                        .temporaryUnpaid(tempUnpaid)
                        .build())
                .recentTransfers(recent)
                .temporaryStayAlerts(temps)
                .build();
    }

    public AdminDto getCurrentAdmin() {
        return authService.currentAdmin();
    }

    private String resolveName(String residentId) {
        return residentRepository.findById(residentId)
                .map(r -> r.getName())
                .orElseGet(() -> pastResidentRepository.findById(residentId)
                        .map(p -> p.getName())
                        .orElse("Former resident"));
    }
}
