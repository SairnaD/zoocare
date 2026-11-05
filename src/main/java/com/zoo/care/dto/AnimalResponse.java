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
        Float weight,
        Integer size,
        int feedingInterval,
        String description,
        String photo,
        String healthStatus
) {
    public static final String DEFAULT_PHOTO_URL = "/images/no-photo.svg";
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
                animal.getSize(),
                animal.getFeedingIntervalHours(),
                animal.getDescription(),
                animal.getPhoto(),
                animal.getHealthStatus().name()
        );
    }
    public String photoUrl() {
        return (photo == null || photo.isEmpty()) ? DEFAULT_PHOTO_URL : "/photos/" + photo;
    }
}