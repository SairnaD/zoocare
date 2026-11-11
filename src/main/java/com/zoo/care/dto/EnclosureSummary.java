package com.zoo.care.dto;

import com.zoo.care.entity.Enclosure;
import com.zoo.care.entity.HabitatType;

public record EnclosureSummary(Long id, String name, HabitatType habitatType) {

    public static EnclosureSummary from(Enclosure enclosure) {
        return new EnclosureSummary(enclosure.getId(), enclosure.getName(), enclosure.getHabitatType());
    }
}