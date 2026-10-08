package com.happynest.pg.config;

import com.happynest.pg.domain.entity.Bed;
import com.happynest.pg.domain.entity.Room;
import com.happynest.pg.domain.enums.BedStatus;
import com.happynest.pg.domain.enums.OccupancyMode;
import com.happynest.pg.domain.enums.RoomType;
import com.happynest.pg.repository.RoomRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataSeeder implements ApplicationRunner {

    private final RoomRepository roomRepository;

    @Value("${app.seed-building:true}")
    private boolean seedBuilding;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (!seedBuilding) {
            return;
        }
        if (roomRepository.count() > 0) {
            log.info("Rooms already present — skip building seed.");
            return;
        }
        log.info("Seeding Happy Nest building (floors 1–5, rooms 101–512)...");
        List<Room> rooms = new ArrayList<>();
        for (int floor : BuildingConfig.FLOOR_NUMBERS) {
            for (int seq = 1; seq <= BuildingConfig.ROOMS_PER_FLOOR; seq++) {
                RoomType type = BuildingConfig.typeForSequence(seq);
                int capacity = BuildingConfig.capacityFor(type);
                int roomNumber = BuildingConfig.roomNumber(floor, seq);

                Room room = Room.builder()
                        .roomNumber(roomNumber)
                        .floorNumber(floor)
                        .type(type)
                        .capacity(capacity)
                        .occupancyMode(type == RoomType.SINGLE_BED ? OccupancyMode.SINGLE : OccupancyMode.DOUBLE)
                        .tripleMemberLimit(type == RoomType.THREE_BED ? 3 : null)
                        .beds(new ArrayList<>())
                        .build();

                for (int i = 0; i < capacity; i++) {
                    String label = BuildingConfig.BED_LABELS.get(i);
                    Bed bed = Bed.builder()
                            .bedId(roomNumber + "-" + label)
                            .bedLabel(label)
                            .status(BedStatus.VACANT)
                            .residentId(null)
                            .room(room)
                            .build();
                    room.getBeds().add(bed);
                }
                rooms.add(room);
            }
        }
        roomRepository.saveAll(rooms);
        log.info("Seeded {} rooms (all vacant).", rooms.size());
    }
}
