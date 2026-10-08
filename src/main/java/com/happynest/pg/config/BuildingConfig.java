package com.happynest.pg.config;

import com.happynest.pg.domain.enums.RoomType;

import java.util.List;
import java.util.Map;

/**
 * Central building rules — mirrors frontend src/data/buildingConfig.js
 */
public final class BuildingConfig {

    private BuildingConfig() {}

    public static final List<Integer> FLOOR_NUMBERS = List.of(1, 2, 3, 4, 5);
    public static final int ROOMS_PER_FLOOR = 12;
    public static final List<String> BED_LABELS = List.of("A", "B", "C");

    public static final Map<Integer, RoomType> ROOM_TYPE_BY_SEQUENCE = Map.ofEntries(
            Map.entry(1, RoomType.SINGLE_BED),
            Map.entry(2, RoomType.TWO_BED),
            Map.entry(3, RoomType.TWO_BED),
            Map.entry(4, RoomType.TWO_BED),
            Map.entry(5, RoomType.TWO_BED),
            Map.entry(6, RoomType.TWO_BED),
            Map.entry(7, RoomType.TWO_BED),
            Map.entry(8, RoomType.TWO_BED),
            Map.entry(9, RoomType.TWO_BED),
            Map.entry(10, RoomType.TWO_BED),
            Map.entry(11, RoomType.TWO_BED),
            Map.entry(12, RoomType.THREE_BED)
    );

    public static int capacityFor(RoomType type) {
        return switch (type) {
            case SINGLE_BED -> 1;
            case TWO_BED -> 2;
            case THREE_BED -> 3;
        };
    }

    public static int roomNumber(int floorNumber, int sequence) {
        return floorNumber * 100 + sequence;
    }

    public static RoomType typeForSequence(int sequence) {
        return ROOM_TYPE_BY_SEQUENCE.get(sequence);
    }
}
