package com.happynest.pg.repository;

import com.happynest.pg.domain.entity.Room;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RoomRepository extends JpaRepository<Room, Integer> {

    @EntityGraph(attributePaths = "beds")
    List<Room> findAllByOrderByRoomNumberAsc();

    @EntityGraph(attributePaths = "beds")
    List<Room> findByFloorNumberOrderByRoomNumberAsc(Integer floorNumber);

    @EntityGraph(attributePaths = "beds")
    Optional<Room> findByRoomNumber(Integer roomNumber);

    long count();
}
