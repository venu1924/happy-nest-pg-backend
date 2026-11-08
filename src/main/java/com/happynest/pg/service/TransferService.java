package com.happynest.pg.service;

import com.happynest.pg.domain.entity.Bed;
import com.happynest.pg.domain.entity.Resident;
import com.happynest.pg.domain.entity.Room;
import com.happynest.pg.domain.entity.Transfer;
import com.happynest.pg.domain.enums.SharingType;
import com.happynest.pg.dto.TransferDto;
import com.happynest.pg.dto.TransferRequest;
import com.happynest.pg.dto.TransferResultDto;
import com.happynest.pg.exception.ApiException;
import com.happynest.pg.repository.PastResidentRepository;
import com.happynest.pg.repository.ResidentRepository;
import com.happynest.pg.repository.RoomRepository;
import com.happynest.pg.repository.TransferRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TransferService {

    private final TransferRepository transferRepository;
    private final ResidentRepository residentRepository;
    private final PastResidentRepository pastResidentRepository;
    private final RoomRepository roomRepository;
    private final BedAssignmentService bedAssignmentService;

    @Transactional(readOnly = true)
    public List<TransferDto> getTransfers() {
        return transferRepository.findAllByOrderByTransferDateDesc().stream()
                .map(t -> DtoMapper.toTransferDto(t, resolveName(t.getResidentId())))
                .toList();
    }

    @Transactional
    public TransferResultDto transfer(String residentId, TransferRequest req) {
        Resident resident = residentRepository.findById(residentId)
                .orElseThrow(() -> new ApiException("Resident not found.", HttpStatus.NOT_FOUND));

        Room fromRoom = roomRepository.findByRoomNumber(resident.getRoomNumber()).orElse(null);
        Room toRoom = roomRepository.findByRoomNumber(req.getToRoomNumber())
                .orElseThrow(() -> new ApiException("Destination room not found.", HttpStatus.NOT_FOUND));

        SharingType sharingType = bedAssignmentService.resolveSharingType(toRoom, req.getSharingType());
        if (sharingType == SharingType.TRIPLE) {
            bedAssignmentService.ensureTwoBedTriple(toRoom);
        }
        Bed toBed = bedAssignmentService.ensureBed(toRoom, req.getToBedId());
        Integer fromRoomNumber = resident.getRoomNumber();
        String fromBedId = resident.getBedId();

        bedAssignmentService.occupyBed(toRoom, toBed, residentId, sharingType, req.getTripleMemberLimit());
        if (fromRoom != null) {
            bedAssignmentService.vacateBed(fromRoom, fromBedId);
            roomRepository.save(fromRoom);
        }
        roomRepository.save(toRoom);

        resident.setRoomNumber(req.getToRoomNumber());
        resident.setBedId(req.getToBedId());
        resident.setSharingType(sharingType);
        residentRepository.save(resident);

        Transfer transfer = Transfer.builder()
                .id("tr-" + UUID.randomUUID().toString().replace("-", "").substring(0, 12))
                .residentId(residentId)
                .fromRoomNumber(fromRoomNumber)
                .fromBedId(fromBedId)
                .toRoomNumber(resident.getRoomNumber())
                .toBedId(resident.getBedId())
                .transferDate(req.getTransferDate() != null ? req.getTransferDate() : LocalDate.now())
                .note(req.getNote() != null ? req.getNote().trim() : "")
                .build();
        transferRepository.save(transfer);

        return TransferResultDto.builder()
                .resident(DtoMapper.toResidentDto(resident))
                .transfer(DtoMapper.toTransferDto(transfer, resident.getName()))
                .build();
    }

    private String resolveName(String residentId) {
        return residentRepository.findById(residentId)
                .map(Resident::getName)
                .orElseGet(() -> pastResidentRepository.findById(residentId)
                        .map(p -> p.getName())
                        .orElse("Former resident"));
    }
}
