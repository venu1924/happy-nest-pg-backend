package com.happynest.pg.service;

import com.happynest.pg.domain.entity.*;
import com.happynest.pg.dto.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;

public final class DtoMapper {

    private DtoMapper() {}

    public static BedDto toBedDto(Bed bed) {
        return BedDto.builder()
                .bedId(bed.getBedId())
                .bedLabel(bed.getBedLabel())
                .status(bed.getStatus())
                .residentId(bed.getResidentId())
                .occupantType(bed.getOccupantType())
                .build();
    }

    public static RoomDto toRoomDto(Room room) {
        List<BedDto> beds = room.getBeds().stream()
                .sorted(Comparator.comparing(Bed::getBedLabel))
                .map(DtoMapper::toBedDto)
                .toList();
        return RoomDto.builder()
                .roomNumber(room.getRoomNumber())
                .floorNumber(room.getFloorNumber())
                .type(room.getType())
                .capacity(room.getCapacity())
                .occupancyMode(room.getOccupancyMode())
                .tripleMemberLimit(room.getTripleMemberLimit())
                .beds(beds)
                .build();
    }

    public static BigDecimal resolveRefundAmount(BigDecimal refundAmount, BigDecimal deposit, Boolean refundable) {
        if (refundAmount != null) {
            return refundAmount;
        }
        if (Boolean.FALSE.equals(refundable)) {
            return BigDecimal.ZERO;
        }
        return deposit != null ? deposit : BigDecimal.ZERO;
    }

    public static ResidentDto toResidentDto(Resident r) {
        BigDecimal refund = resolveRefundAmount(r.getRefundAmount(), r.getDeposit(), r.isDepositRefundable());
        return ResidentDto.builder()
                .id(r.getId())
                .name(r.getName())
                .phone(r.getPhone())
                .joiningDate(r.getJoiningDate())
                .roomNumber(r.getRoomNumber())
                .bedId(r.getBedId())
                .monthlyRent(r.getMonthlyRent())
                .deposit(r.getDeposit())
                .refundAmount(refund)
                .depositRefundable(refund.compareTo(BigDecimal.ZERO) > 0)
                .sharingType(r.getSharingType())
                .notes(r.getNotes())
                .build();
    }

    public static PastResidentDto toPastResidentDto(PastResident r) {
        BigDecimal refund = resolveRefundAmount(r.getRefundAmount(), r.getDeposit(), r.getDepositRefundable());
        return PastResidentDto.builder()
                .id(r.getId())
                .name(r.getName())
                .phone(r.getPhone())
                .joiningDate(r.getJoiningDate())
                .checkoutDate(r.getCheckoutDate())
                .lastRoomNumber(r.getLastRoomNumber())
                .lastBedId(r.getLastBedId())
                .monthlyRent(r.getMonthlyRent())
                .deposit(r.getDeposit())
                .refundAmount(refund)
                .depositRefundable(refund.compareTo(BigDecimal.ZERO) > 0)
                .sharingType(r.getSharingType())
                .notes(r.getNotes())
                .deletionReason(r.getDeletionReason())
                .build();
    }

    public static TemporaryResidentDto toTemporaryResidentDto(TemporaryResident r) {
        LocalDate checkout = r.checkoutDate();
        long daysLeft = checkout != null ? ChronoUnit.DAYS.between(LocalDate.now(), checkout) : 0;
        return TemporaryResidentDto.builder()
                .id(r.getId())
                .name(r.getName())
                .phone(r.getPhone())
                .amountPerDay(r.getAmountPerDay())
                .joiningDate(r.getJoiningDate())
                .stayDays(r.getStayDays())
                .checkoutDate(checkout)
                .daysLeft((int) daysLeft)
                .vacateToday(daysLeft < 0)
                .roomNumber(r.getRoomNumber())
                .bedId(r.getBedId())
                .sharingType(r.getSharingType())
                .amount(r.getAmount())
                .paymentStatus(r.getPaymentStatus())
                .paymentMethod(r.getPaymentMethod())
                .paidDate(r.getPaidDate())
                .receivedBy(r.getReceivedBy())
                .notes(r.getNotes())
                .stayType("TEMPORARY STAY")
                .build();
    }

    public static TransferDto toTransferDto(Transfer t, String residentName) {
        return TransferDto.builder()
                .id(t.getId())
                .residentId(t.getResidentId())
                .fromRoomNumber(t.getFromRoomNumber())
                .fromBedId(t.getFromBedId())
                .toRoomNumber(t.getToRoomNumber())
                .toBedId(t.getToBedId())
                .transferDate(t.getTransferDate())
                .note(t.getNote())
                .residentName(residentName)
                .build();
    }
}
