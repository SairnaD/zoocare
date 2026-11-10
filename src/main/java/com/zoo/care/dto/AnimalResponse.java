package com.zoo.care.dto;

import com.zoo.care.entity.Animal;

import java.time.LocalDate;

public record AnimalResponse(
        Long id,
        String name,
        Long speciesId,
        String speciesName,
        Long enclosureId,
        String enclosureName,
        LocalDate birthDate,
        float weight,
        int feedingIntervalHours,
        String description
) {

    public static AnimalResponse from(Animal animal) {
        return new AnimalResponse(
                animal.getId(),
                animal.getName(),
                animal.getSpecies().getId(),
                animal.getSpecies().getName(),
                animal.getEnclosure().getId(),
                animal.getEnclosure().getName(),
                animal.getBirthDate(),
                animal.getWeight(),
                animal.getFeedingIntervalHours(),
                animal.getDescription()
        );
    }
}