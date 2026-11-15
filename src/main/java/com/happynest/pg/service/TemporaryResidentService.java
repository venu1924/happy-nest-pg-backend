package com.happynest.pg.service;

import com.happynest.pg.domain.entity.Bed;
import com.happynest.pg.domain.entity.Resident;
import com.happynest.pg.domain.entity.Room;
import com.happynest.pg.domain.entity.TemporaryResident;
import com.happynest.pg.domain.enums.BedStatus;
import com.happynest.pg.domain.enums.OccupantType;
import com.happynest.pg.domain.enums.PaymentStatus;
import com.happynest.pg.domain.enums.SharingType;
import com.happynest.pg.dto.CreateTemporaryResidentRequest;
import com.happynest.pg.dto.ExtendStayRequest;
import com.happynest.pg.dto.MarkPaidRequest;
import com.happynest.pg.dto.TemporaryResidentDto;
import com.happynest.pg.exception.ApiException;
import com.happynest.pg.repository.ResidentRepository;
import com.happynest.pg.repository.RoomRepository;
import com.happynest.pg.repository.TemporaryResidentRepository;
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
public class TemporaryResidentService {

    private final TemporaryResidentRepository temporaryResidentRepository;
    private final ResidentRepository residentRepository;
    private final RoomRepository roomRepository;
    private final BedAssignmentService bedAssignmentService;

    @Transactional
    public List<TemporaryResidentDto> list() {
        settleArrivals();
        return temporaryResidentRepository.findAllByOrderByJoiningDateAscNameAsc().stream()
                .map(DtoMapper::toTemporaryResidentDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public TemporaryResidentDto get(String id) {
        return DtoMapper.toTemporaryResidentDto(find(id));
    }

    @Transactional
    public TemporaryResidentDto create(CreateTemporaryResidentRequest req) {
        Room room = roomRepository.findByRoomNumber(req.getRoomNumber())
                .orElseThrow(() -> new ApiException("Room not found.", HttpStatus.NOT_FOUND));
        SharingType sharingType = bedAssignmentService.resolveSharingType(room, req.getSharingType());
        if (sharingType == SharingType.TRIPLE && room.getType() == com.happynest.pg.domain.enums.RoomType.TWO_BED) {
            bedAssignmentService.ensureTwoBedTriple(room);
        }
        Bed bed = bedAssignmentService.ensureBed(room, req.getBedId());
        String id = "tmp-" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        if (bed.getStatus() == BedStatus.OCCUPIED) {
            if (!freeLater(bed, req.getJoiningDate())) {
                throw new ApiException("Selected bed is already occupied.", HttpStatus.BAD_REQUEST);
            }
        } else {
            bedAssignmentService.occupyBed(room, bed, id, sharingType, null, OccupantType.TEMPORARY);
            roomRepository.save(room);
        }

        int days = req.getStayDays();
        BigDecimal perDay = req.getAmountPerDay();
        TemporaryResident resident = TemporaryResident.builder()
                .id(id)
                .name(req.getName().trim())
                .phone(req.getPhone().replaceAll("\\s", ""))
                .amountPerDay(perDay)
                .joiningDate(req.getJoiningDate())
                .stayDays(days)
                .roomNumber(req.getRoomNumber())
                .bedId(bed.getBedId())
                .sharingType(sharingType)
                .amount(perDay.multiply(BigDecimal.valueOf(days)))
                .paymentStatus(PaymentStatus.PENDING)
                .notes(req.getNotes() != null ? req.getNotes().trim() : "")
                .build();
        return DtoMapper.toTemporaryResidentDto(temporaryResidentRepository.save(resident));
    }

    @Transactional
    public TemporaryResidentDto extend(String id, ExtendStayRequest req) {
        TemporaryResident resident = find(id);
        int extra = req.getExtraDays();
        resident.setStayDays(resident.getStayDays() + extra);
        resident.setAmount(resident.getAmountPerDay().multiply(BigDecimal.valueOf(resident.getStayDays())));
        return DtoMapper.toTemporaryResidentDto(temporaryResidentRepository.save(resident));
    }

    @Transactional
    public TemporaryResidentDto markPaid(String id, MarkPaidRequest req) {
        TemporaryResident resident = find(id);
        if (resident.getPaymentStatus() == PaymentStatus.PAID) {
            throw new ApiException("Payment is already marked as paid.", HttpStatus.BAD_REQUEST);
        }
        resident.setPaymentStatus(PaymentStatus.PAID);
        resident.setPaymentMethod(req.getPaymentMethod());
        resident.setPaidDate(req.getPaidDate() != null ? req.getPaidDate() : LocalDate.now());
        resident.setReceivedBy(req.getReceivedBy() != null && !req.getReceivedBy().isBlank()
                ? req.getReceivedBy()
                : "Admin");
        return DtoMapper.toTemporaryResidentDto(temporaryResidentRepository.save(resident));
    }

    @Transactional
    public MapId delete(String id) {
        TemporaryResident resident = find(id);
        Room room = roomRepository.findByRoomNumber(resident.getRoomNumber()).orElse(null);
        if (room != null) {
            bedAssignmentService.vacateBed(room, resident.getBedId());
            roomRepository.save(room);
        }
        temporaryResidentRepository.delete(resident);
        return new MapId(id);
    }

    private void settleArrivals() {
        LocalDate today = LocalDate.now();
        for (TemporaryResident stay : temporaryResidentRepository.findAll()) {
            if (stay.getJoiningDate() == null || stay.getJoiningDate().isAfter(today)) {
                continue;
            }
            Room room = roomRepository.findByRoomNumber(stay.getRoomNumber()).orElse(null);
            if (room == null) {
                continue;
            }
            Bed bed;
            try {
                bed = bedAssignmentService.ensureBed(room, stay.getBedId());
            } catch (ApiException ex) {
                continue;
            }
            if (stay.getId().equals(bed.getResidentId())) {
                continue;
            }
            if (bed.getStatus() == BedStatus.OCCUPIED && !leftBy(bed, today)) {
                continue;
            }
            if (bed.getStatus() == BedStatus.OCCUPIED) {
                bedAssignmentService.vacateBed(room, bed.getBedId());
            }
            bedAssignmentService.occupyBed(room, bed, stay.getId(), stay.getSharingType(), null, OccupantType.TEMPORARY);
            roomRepository.save(room);
        }
    }

    private boolean freeLater(Bed bed, LocalDate joining) {
        return joining != null && joining.isAfter(LocalDate.now()) && leftBy(bed, joining);
    }

    private boolean leftBy(Bed bed, LocalDate day) {
        if (bed.getResidentId() == null) {
            return true;
        }
        if (bed.getOccupantType() == OccupantType.TEMPORARY) {
            return temporaryResidentRepository.findById(bed.getResidentId())
                    .map(stay -> stay.getJoiningDate() != null
                            && stay.getJoiningDate().plusDays(Math.max(stay.getStayDays(), 1) - 1L).isBefore(day))
                    .orElse(true);
        }
        Resident holder = residentRepository.findById(bed.getResidentId()).orElse(null);
        return holder != null && holder.getVacateDate() != null && !holder.getVacateDate().isAfter(day);
    }

    private TemporaryResident find(String id) {
        return temporaryResidentRepository.findById(id)
                .orElseThrow(() -> new ApiException("Temporary resident not found.", HttpStatus.NOT_FOUND));
    }

    public record MapId(String id) {}
}
