package com.zoo.care.dto;

import com.zoo.care.entity.DietType;
import com.zoo.care.entity.HabitatType;
import com.zoo.care.entity.Species;

public record SpeciesResponse(
        Long id,
        String name,
        HabitatType habitatType,
        DietType dietType
) {

    public static SpeciesResponse from(Species species) {
        return new SpeciesResponse(
                species.getId(),
                species.getName(),
                species.getHabitatType(),
                species.getDietType()
        );
    }
}