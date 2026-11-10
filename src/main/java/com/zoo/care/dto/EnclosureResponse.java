package com.zoo.care.dto;

import com.zoo.care.entity.Enclosure;
import com.zoo.care.entity.HabitatType;

import java.util.List;

public record EnclosureResponse(
        Long id,
        String name,
        HabitatType habitatType,
        int capacity,
        List<String> keeperNames
) {

    public static EnclosureResponse from(Enclosure enclosure) {
        return from(enclosure, List.of());
    }

    public static EnclosureResponse from(Enclosure enclosure, List<String> keeperNames) {
        return new EnclosureResponse(
                enclosure.getId(),
                enclosure.getName(),
                enclosure.getHabitatType(),
                enclosure.getCapacity(),
                keeperNames
        );
    }
}