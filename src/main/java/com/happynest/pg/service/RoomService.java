package com.happynest.pg.service;

import com.happynest.pg.config.BuildingConfig;
import com.happynest.pg.domain.entity.Room;
import com.happynest.pg.dto.FloorDto;
import com.happynest.pg.dto.RoomDto;
import com.happynest.pg.exception.ApiException;
import com.happynest.pg.repository.RoomRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RoomService {

    private final RoomRepository roomRepository;

    public List<FloorDto> getFloors() {
        List<Room> rooms = roomRepository.findAllByOrderByRoomNumberAsc();
        Map<Integer, List<Room>> byFloor = rooms.stream()
                .collect(Collectors.groupingBy(Room::getFloorNumber));

        return BuildingConfig.FLOOR_NUMBERS.stream()
                .map(floor -> FloorDto.builder()
                        .floorNumber(floor)
                        .rooms(byFloor.getOrDefault(floor, List.of()).stream()
                                .sorted(Comparator.comparing(Room::getRoomNumber))
                                .map(DtoMapper::toRoomDto)
                                .toList())
                        .build())
                .toList();
    }

    public FloorDto getFloor(Integer floorNumber) {
        List<Room> rooms = roomRepository.findByFloorNumberOrderByRoomNumberAsc(floorNumber);
        if (rooms.isEmpty() && !BuildingConfig.FLOOR_NUMBERS.contains(floorNumber)) {
            throw new ApiException("Floor not found.", HttpStatus.NOT_FOUND);
        }
        return FloorDto.builder()
                .floorNumber(floorNumber)
                .rooms(rooms.stream().map(DtoMapper::toRoomDto).toList())
                .build();
    }

    public RoomDto getRoom(Integer roomNumber) {
        Room room = roomRepository.findByRoomNumber(roomNumber)
                .orElseThrow(() -> new ApiException("Room not found.", HttpStatus.NOT_FOUND));
        return DtoMapper.toRoomDto(room);
    }

    public List<RoomDto> getRooms() {
        return roomRepository.findAllByOrderByRoomNumberAsc().stream()
                .map(DtoMapper::toRoomDto)
                .toList();
    }
}
