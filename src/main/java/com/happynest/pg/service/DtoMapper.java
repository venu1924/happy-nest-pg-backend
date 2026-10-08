package com.happynest.pg.service;

import com.happynest.pg.domain.entity.*;
import com.happynest.pg.dto.*;

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

    public static ResidentDto toResidentDto(Resident r) {
        return ResidentDto.builder()
                .id(r.getId())
                .name(r.getName())
                .phone(r.getPhone())
                .joiningDate(r.getJoiningDate())
                .roomNumber(r.getRoomNumber())
                .bedId(r.getBedId())
                .monthlyRent(r.getMonthlyRent())
                .deposit(r.getDeposit())
                .depositRefundable(r.isDepositRefundable())
                .sharingType(r.getSharingType())
                .notes(r.getNotes())
                .build();
    }

    public static PastResidentDto toPastResidentDto(PastResident r) {
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
                .depositRefundable(r.getDepositRefundable())
                .sharingType(r.getSharingType())
                .notes(r.getNotes())
                .deletionReason(r.getDeletionReason())
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
