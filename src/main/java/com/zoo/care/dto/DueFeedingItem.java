package com.zoo.care.dto;

import java.time.Instant;

public record DueFeedingItem(
        Long animalId,
        String animalName,
        String speciesName,
        String enclosureName,
        int feedingIntervalHours,
        Instant lastFedAt,
        Instant nextAllowedAt
) {
}
