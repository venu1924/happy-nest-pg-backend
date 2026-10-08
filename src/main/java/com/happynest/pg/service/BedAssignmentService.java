package com.happynest.pg.service;

import com.happynest.pg.domain.entity.Bed;
import com.happynest.pg.domain.entity.Room;
import com.happynest.pg.domain.enums.*;
import com.happynest.pg.exception.ApiException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

/**
 * Occupancy rules matching frontend mockStore:
 * - TWO_BED + SINGLE sharing → both beds occupied by one resident
 * - THREE_BED + limit 2 → Bed C BLOCKED; room full at 2 residents
 */
@Service
public class BedAssignmentService {

    public SharingType resolveSharingType(Room room, SharingType requested) {
        if (requested == SharingType.SINGLE || requested == SharingType.DOUBLE) {
            return requested;
        }
        if (room.getType() == RoomType.SINGLE_BED) {
            return SharingType.SINGLE;
        }
        if (room.getType() == RoomType.TWO_BED) {
            return SharingType.DOUBLE;
        }
        return null;
    }

    public void occupyBed(Room room, Bed bed, String residentId, SharingType sharingType, Integer tripleMemberLimit) {
        if (room.getType() == RoomType.THREE_BED) {
            occupyTriple(room, bed, residentId, tripleMemberLimit);
            return;
        }

        if (room.getType() == RoomType.TWO_BED && sharingType == SharingType.SINGLE) {
            long alreadyOccupied = room.getBeds().stream()
                    .filter(b -> b.getStatus() == BedStatus.OCCUPIED)
                    .count();
            if (alreadyOccupied > 0) {
                throw new ApiException(
                        "Room already has a resident. Single sharing needs the whole room empty.",
                        HttpStatus.BAD_REQUEST);
            }
            room.setOccupancyMode(OccupancyMode.SINGLE);
            for (Bed b : room.getBeds()) {
                b.setStatus(BedStatus.OCCUPIED);
                b.setResidentId(residentId);
            }
            return;
        }

        if (bed.getStatus() == BedStatus.OCCUPIED) {
            throw new ApiException("Selected bed is already occupied.", HttpStatus.BAD_REQUEST);
        }
        if (bed.getStatus() == BedStatus.BLOCKED) {
            throw new ApiException("This bed is not available for assignment.", HttpStatus.BAD_REQUEST);
        }

        bed.setStatus(BedStatus.OCCUPIED);
        bed.setResidentId(residentId);

        if (room.getType() == RoomType.TWO_BED) {
            room.setOccupancyMode(OccupancyMode.DOUBLE);
        } else if (room.getType() == RoomType.SINGLE_BED) {
            room.setOccupancyMode(OccupancyMode.SINGLE);
        }
    }

    private void occupyTriple(Room room, Bed bed, String residentId, Integer tripleMemberLimit) {
        int limit = tripleMemberLimit != null
                ? tripleMemberLimit
                : (room.getTripleMemberLimit() != null ? room.getTripleMemberLimit() : 3);
        room.setTripleMemberLimit(limit);

        if (limit == 2) {
            blockTripleThirdBed(room);
        }

        if (bed.getStatus() == BedStatus.BLOCKED) {
            throw new ApiException(
                    "This bed is not used — room is set for 2 members only.",
                    HttpStatus.BAD_REQUEST);
        }
        if ("C".equals(bed.getBedLabel()) && limit == 2) {
            throw new ApiException(
                    "Bed C is not used when triple room is for 2 members only.",
                    HttpStatus.BAD_REQUEST);
        }
        if (bed.getStatus() == BedStatus.OCCUPIED) {
            throw new ApiException("Selected bed is already occupied.", HttpStatus.BAD_REQUEST);
        }

        long occupiedBefore = room.getBeds().stream()
                .filter(b -> b.getStatus() == BedStatus.OCCUPIED)
                .count();
        if (limit == 2 && occupiedBefore >= 2) {
            throw new ApiException("This triple room is full (2-member option).", HttpStatus.BAD_REQUEST);
        }
        if (limit == 3 && occupiedBefore >= 3) {
            throw new ApiException("This triple room is full.", HttpStatus.BAD_REQUEST);
        }

        bed.setStatus(BedStatus.OCCUPIED);
        bed.setResidentId(residentId);
    }

    public void vacateBed(Room room, String bedId) {
        Bed bed = room.getBeds().stream()
                .filter(b -> b.getBedId().equals(bedId))
                .findFirst()
                .orElse(null);

        boolean wasSingleTwoBed =
                room.getOccupancyMode() == OccupancyMode.SINGLE && room.getType() == RoomType.TWO_BED;
        String residentId = bed != null ? bed.getResidentId() : null;

        if (wasSingleTwoBed) {
            for (Bed b : room.getBeds()) {
                if (residentId == null || residentId.equals(b.getResidentId())) {
                    b.setStatus(BedStatus.VACANT);
                    b.setResidentId(null);
                }
            }
            room.setOccupancyMode(OccupancyMode.DOUBLE);
            return;
        }

        if (bed != null) {
            bed.setStatus(BedStatus.VACANT);
            bed.setResidentId(null);
        }
        releaseTripleTwoMemberMode(room);
    }

    private void blockTripleThirdBed(Room room) {
        room.getBeds().stream()
                .filter(b -> "C".equals(b.getBedLabel()))
                .findFirst()
                .ifPresent(bedC -> {
                    if (bedC.getStatus() != BedStatus.OCCUPIED) {
                        bedC.setStatus(BedStatus.BLOCKED);
                        bedC.setResidentId(null);
                    }
                });
    }

    private void releaseTripleTwoMemberMode(Room room) {
        if (room.getType() != RoomType.THREE_BED) {
            return;
        }
        long occupied = room.getBeds().stream()
                .filter(b -> b.getStatus() == BedStatus.OCCUPIED)
                .count();
        if (occupied > 0) {
            return;
        }
        room.setTripleMemberLimit(3);
        for (Bed b : room.getBeds()) {
            if (b.getStatus() == BedStatus.BLOCKED) {
                b.setStatus(BedStatus.VACANT);
                b.setResidentId(null);
            }
        }
    }
}
