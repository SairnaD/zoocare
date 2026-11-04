package com.zoo.care.dto;

import com.zoo.care.entity.Enclosure;
import com.zoo.care.entity.HabitatType;

public record EnclosureResponse(
        Long id,
        String name,
        HabitatType habitatType,
        int capacity
) {

    public static EnclosureResponse from(Enclosure enclosure) {
        return new EnclosureResponse(
                enclosure.getId(),
                enclosure.getName(),
                enclosure.getHabitatType(),
                enclosure.getCapacity()
        );
    }
}