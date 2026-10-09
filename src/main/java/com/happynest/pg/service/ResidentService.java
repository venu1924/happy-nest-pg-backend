package com.happynest.pg.service;

import com.happynest.pg.domain.entity.*;
import com.happynest.pg.domain.enums.PaymentStatus;
import com.happynest.pg.domain.enums.SharingType;
import com.happynest.pg.dto.*;
import com.happynest.pg.exception.ApiException;
import com.happynest.pg.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ResidentService {

    private final ResidentRepository residentRepository;
    private final PastResidentRepository pastResidentRepository;
    private final RoomRepository roomRepository;
    private final PaymentRepository paymentRepository;
    private final BedAssignmentService bedAssignmentService;

    @Transactional(readOnly = true)
    public List<ResidentDto> getResidents() {
        return residentRepository.findAllByOrderByNameAsc().stream()
                .map(DtoMapper::toResidentDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public ResidentDto getResident(String id) {
        return DtoMapper.toResidentDto(findResident(id));
    }

    @Transactional(readOnly = true)
    public List<PastResidentDto> getPastResidents() {
        return pastResidentRepository.findAllByOrderByCheckoutDateDesc().stream()
                .map(DtoMapper::toPastResidentDto)
                .toList();
    }

    @Transactional
    public ResidentDto create(CreateResidentRequest req) {
        Room room = roomRepository.findByRoomNumber(req.getRoomNumber())
                .orElseThrow(() -> new ApiException("Room not found.", HttpStatus.NOT_FOUND));

        Bed bed = room.getBeds().stream()
                .filter(b -> b.getBedId().equals(req.getBedId()))
                .findFirst()
                .orElseThrow(() -> new ApiException("Bed not found.", HttpStatus.NOT_FOUND));

        SharingType sharingType = bedAssignmentService.resolveSharingType(room, req.getSharingType());
        String id = "res-" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);

        bedAssignmentService.occupyBed(room, bed, id, sharingType, req.getTripleMemberLimit());
        roomRepository.save(room);

        BigDecimal deposit = req.getDeposit() != null ? req.getDeposit() : BigDecimal.ZERO;
        BigDecimal refundAmount = resolveRefundAmount(req.getRefundAmount(), deposit, req.getDepositRefundable());

        Resident resident = Resident.builder()
                .id(id)
                .name(req.getName().trim())
                .phone(req.getPhone().replaceAll("\\s", ""))
                .joiningDate(req.getJoiningDate())
                .roomNumber(req.getRoomNumber())
                .bedId(req.getBedId())
                .monthlyRent(req.getMonthlyRent())
                .deposit(deposit)
                .refundAmount(refundAmount)
                .depositRefundable(refundAmount.compareTo(BigDecimal.ZERO) > 0)
                .sharingType(sharingType)
                .notes(req.getNotes() != null ? req.getNotes().trim() : "")
                .build();
        residentRepository.save(resident);

        LocalDate now = LocalDate.now();
        Payment payment = Payment.builder()
                .id("pay-" + id + "-" + now.getYear() + "-" + now.getMonthValue())
                .residentId(id)
                .month(now.getMonthValue())
                .year(now.getYear())
                .amount(resident.getMonthlyRent())
                .status(PaymentStatus.PENDING)
                .build();
        paymentRepository.save(payment);

        return DtoMapper.toResidentDto(resident);
    }

    @Transactional
    public ResidentDto update(String id, UpdateResidentRequest req) {
        Resident resident = findResident(id);
        resident.setName(req.getName().trim());
        resident.setPhone(req.getPhone().replaceAll("\\s", ""));
        resident.setJoiningDate(req.getJoiningDate());
        resident.setMonthlyRent(req.getMonthlyRent());
        BigDecimal deposit = req.getDeposit() != null ? req.getDeposit() : BigDecimal.ZERO;
        BigDecimal refundAmount = resolveRefundAmount(req.getRefundAmount(), deposit, req.getDepositRefundable());
        resident.setDeposit(deposit);
        resident.setRefundAmount(refundAmount);
        resident.setDepositRefundable(refundAmount.compareTo(BigDecimal.ZERO) > 0);
        if (req.getNotes() != null) {
            resident.setNotes(req.getNotes());
        }
        return DtoMapper.toResidentDto(residentRepository.save(resident));
    }

    @Transactional
    public MapId delete(String id, DeleteResidentRequest req) {
        Resident resident = findResident(id);
        Room room = roomRepository.findByRoomNumber(resident.getRoomNumber())
                .orElse(null);
        if (room != null) {
            bedAssignmentService.vacateBed(room, resident.getBedId());
            roomRepository.save(room);
        }

        String reason = req != null && req.getNotes() != null ? req.getNotes().trim() : "";

        PastResident past = PastResident.builder()
                .id(resident.getId())
                .name(resident.getName())
                .phone(resident.getPhone())
                .joiningDate(resident.getJoiningDate())
                .checkoutDate(LocalDate.now())
                .lastRoomNumber(resident.getRoomNumber())
                .lastBedId(resident.getBedId())
                .monthlyRent(resident.getMonthlyRent())
                .deposit(resident.getDeposit())
                .refundAmount(DtoMapper.resolveRefundAmount(
                        resident.getRefundAmount(), resident.getDeposit(), resident.isDepositRefundable()))
                .depositRefundable(resident.isDepositRefundable())
                .sharingType(resident.getSharingType())
                .notes(resident.getNotes())
                .deletionReason(reason)
                .build();
        pastResidentRepository.save(past);

        paymentRepository.deleteByResidentId(id);
        residentRepository.delete(resident);
        return new MapId(id);
    }

    private static BigDecimal resolveRefundAmount(
            BigDecimal refundAmount, BigDecimal deposit, Boolean depositRefundable) {
        if (refundAmount != null) {
            return refundAmount;
        }
        return DtoMapper.resolveRefundAmount(null, deposit, depositRefundable);
    }

    private Resident findResident(String id) {
        return residentRepository.findById(id)
                .orElseThrow(() -> new ApiException("Resident not found.", HttpStatus.NOT_FOUND));
    }

    public record MapId(String id) {}
}
