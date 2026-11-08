package com.happynest.pg.service;

import com.happynest.pg.domain.entity.Bed;
import com.happynest.pg.domain.entity.Room;
import com.happynest.pg.domain.enums.*;
import com.happynest.pg.exception.ApiException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

/**
 * Occupancy rules:
 * - TWO_BED + SINGLE sharing → both beds occupied by one occupant
 * - TWO_BED + TRIPLE sharing → add Bed C; up to 3 occupants
 * - THREE_BED + limit 2 → Bed C BLOCKED; room full at 2
 */
@Service
public class BedAssignmentService {

    public SharingType resolveSharingType(Room room, SharingType requested) {
        if (requested == SharingType.SINGLE
                || requested == SharingType.DOUBLE
                || requested == SharingType.TRIPLE) {
            return requested;
        }
        if (room.getType() == RoomType.SINGLE_BED) {
            return SharingType.SINGLE;
        }
        if (room.getType() == RoomType.TWO_BED) {
            return SharingType.DOUBLE;
        }
        if (room.getType() == RoomType.THREE_BED) {
            return SharingType.TRIPLE;
        }
        return SharingType.DOUBLE;
    }

    public void occupyBed(Room room, Bed bed, String occupantId, SharingType sharingType, Integer tripleMemberLimit) {
        occupyBed(room, bed, occupantId, sharingType, tripleMemberLimit, OccupantType.MONTHLY);
    }

    public void occupyBed(
            Room room,
            Bed bed,
            String occupantId,
            SharingType sharingType,
            Integer tripleMemberLimit,
            OccupantType occupantType) {
        OccupantType kind = occupantType != null ? occupantType : OccupantType.MONTHLY;

        if (room.getType() == RoomType.THREE_BED) {
            occupyTriple(room, bed, occupantId, tripleMemberLimit, kind);
            return;
        }

        if (room.getType() == RoomType.TWO_BED && sharingType == SharingType.TRIPLE) {
            ensureTwoBedTriple(room);
            occupyOneBed(resolveBed(room, bed), occupantId, kind);
            room.setOccupancyMode(OccupancyMode.TRIPLE);
            room.setCapacity(3);
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
                if ("C".equals(b.getBedLabel())) {
                    continue;
                }
                b.setStatus(BedStatus.OCCUPIED);
                b.setResidentId(occupantId);
                b.setOccupantType(kind);
            }
            return;
        }

        occupyOneBed(bed, occupantId, kind);

        if (room.getType() == RoomType.TWO_BED) {
            if (room.getOccupancyMode() != OccupancyMode.TRIPLE) {
                room.setOccupancyMode(OccupancyMode.DOUBLE);
            }
        } else if (room.getType() == RoomType.SINGLE_BED) {
            room.setOccupancyMode(OccupancyMode.SINGLE);
        }
    }

    public Bed ensureBed(Room room, String bedId) {
        return room.getBeds().stream()
                .filter(b -> b.getBedId().equals(bedId))
                .findFirst()
                .orElseGet(() -> {
                    if (room.getType() == RoomType.TWO_BED && bedId != null && bedId.endsWith("-C")) {
                        ensureTwoBedTriple(room);
                        return room.getBeds().stream()
                                .filter(b -> "C".equals(b.getBedLabel()))
                                .findFirst()
                                .orElseThrow(() -> new ApiException("Bed not found.", HttpStatus.NOT_FOUND));
                    }
                    throw new ApiException("Bed not found.", HttpStatus.NOT_FOUND);
                });
    }

    public void ensureTwoBedTriple(Room room) {
        if (room.getType() != RoomType.TWO_BED) {
            return;
        }
        boolean hasC = room.getBeds().stream().anyMatch(b -> "C".equals(b.getBedLabel()));
        if (!hasC) {
            Bed bedC = Bed.builder()
                    .bedId(room.getRoomNumber() + "-C")
                    .bedLabel("C")
                    .status(BedStatus.VACANT)
                    .residentId(null)
                    .occupantType(null)
                    .room(room)
                    .build();
            room.getBeds().add(bedC);
        }
        room.setOccupancyMode(OccupancyMode.TRIPLE);
        room.setCapacity(3);
    }

    private Bed resolveBed(Room room, Bed bed) {
        if (bed != null && room.getBeds().contains(bed)) {
            return bed;
        }
        if (bed != null) {
            return ensureBed(room, bed.getBedId());
        }
        throw new ApiException("Bed not found.", HttpStatus.NOT_FOUND);
    }

    private void occupyOneBed(Bed bed, String occupantId, OccupantType kind) {
        if (bed.getStatus() == BedStatus.OCCUPIED) {
            throw new ApiException("Selected bed is already occupied.", HttpStatus.BAD_REQUEST);
        }
        if (bed.getStatus() == BedStatus.BLOCKED) {
            throw new ApiException("This bed is not available for assignment.", HttpStatus.BAD_REQUEST);
        }
        bed.setStatus(BedStatus.OCCUPIED);
        bed.setResidentId(occupantId);
        bed.setOccupantType(kind);
    }

    private void occupyTriple(Room room, Bed bed, String occupantId, Integer tripleMemberLimit, OccupantType kind) {
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

        long occupiedBefore = room.getBeds().stream()
                .filter(b -> b.getStatus() == BedStatus.OCCUPIED)
                .count();
        if (limit == 2 && occupiedBefore >= 2) {
            throw new ApiException("This triple room is full (2-member option).", HttpStatus.BAD_REQUEST);
        }
        if (limit == 3 && occupiedBefore >= 3) {
            throw new ApiException("This triple room is full.", HttpStatus.BAD_REQUEST);
        }

        occupyOneBed(bed, occupantId, kind);
    }

    public void vacateBed(Room room, String bedId) {
        Bed bed = room.getBeds().stream()
                .filter(b -> b.getBedId().equals(bedId))
                .findFirst()
                .orElse(null);

        boolean wasSingleTwoBed =
                room.getOccupancyMode() == OccupancyMode.SINGLE && room.getType() == RoomType.TWO_BED;
        String occupantId = bed != null ? bed.getResidentId() : null;

        if (wasSingleTwoBed) {
            for (Bed b : room.getBeds()) {
                if (occupantId == null || occupantId.equals(b.getResidentId())) {
                    clearBed(b);
                }
            }
            room.setOccupancyMode(OccupancyMode.DOUBLE);
            resetTwoBedIfEmpty(room);
            return;
        }

        if (bed != null) {
            clearBed(bed);
        }
        releaseTripleTwoMemberMode(room);
        resetTwoBedIfEmpty(room);
    }

    private void clearBed(Bed b) {
        b.setStatus(BedStatus.VACANT);
        b.setResidentId(null);
        b.setOccupantType(null);
    }

    private void blockTripleThirdBed(Room room) {
        room.getBeds().stream()
                .filter(b -> "C".equals(b.getBedLabel()))
                .findFirst()
                .ifPresent(bedC -> {
                    if (bedC.getStatus() != BedStatus.OCCUPIED) {
                        bedC.setStatus(BedStatus.BLOCKED);
                        bedC.setResidentId(null);
                        bedC.setOccupantType(null);
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
                clearBed(b);
            }
        }
    }

    private void resetTwoBedIfEmpty(Room room) {
        if (room.getType() != RoomType.TWO_BED) {
            return;
        }
        long occupied = room.getBeds().stream()
                .filter(b -> b.getStatus() == BedStatus.OCCUPIED)
                .count();
        if (occupied > 0) {
            return;
        }
        room.getBeds().removeIf(b -> "C".equals(b.getBedLabel()));
        room.setCapacity(2);
        room.setOccupancyMode(OccupancyMode.DOUBLE);
    }
}
