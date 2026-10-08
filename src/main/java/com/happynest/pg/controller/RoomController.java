package com.happynest.pg.controller;

import com.happynest.pg.dto.FloorDto;
import com.happynest.pg.dto.RoomDto;
import com.happynest.pg.service.RoomService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class RoomController {

    private final RoomService roomService;

    @GetMapping("/floors")
    public List<FloorDto> getFloors() {
        return roomService.getFloors();
    }

    @GetMapping("/floors/{floorNumber}")
    public FloorDto getFloor(@PathVariable Integer floorNumber) {
        return roomService.getFloor(floorNumber);
    }

    @GetMapping("/rooms")
    public List<RoomDto> getRooms() {
        return roomService.getRooms();
    }

    @GetMapping("/rooms/{roomNumber}")
    public RoomDto getRoom(@PathVariable Integer roomNumber) {
        return roomService.getRoom(roomNumber);
    }
}
