package com.zoo.care.dto;

import com.zoo.care.entity.FeedingStatus;
import com.zoo.care.entity.HabitatType;

import java.time.Instant;
import java.time.LocalDate;

public record KeeperAnimalView(
        Long animalId,
        String animalName,
        String speciesName,
        String enclosureName,
        HabitatType enclosureHabitat,
        LocalDate birthDate,
        int ageYears,
        float weight,
        String description,
        int feedingIntervalHours,
        Instant lastFedAt,
        String lastFedBy,
        Instant nextFeedingAt,
        FeedingStatus status
) {
}