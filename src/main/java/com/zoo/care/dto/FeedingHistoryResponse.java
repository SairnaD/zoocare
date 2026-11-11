package com.zoo.care.dto;

import com.zoo.care.entity.Feedings;
import com.zoo.care.entity.HabitatType;

import java.math.BigDecimal;
import java.time.Instant;

public record FeedingHistoryResponse(
        Long id,
        String animalName,
        String speciesName,
        String enclosureName,
        HabitatType enclosureHabitat,
        Instant fedAt,
        String food,
        BigDecimal amountKg,
        String note,
        String recordedByName) {

    public static FeedingHistoryResponse from(Feedings feeding) {
        return new FeedingHistoryResponse(
                feeding.getId(),
                feeding.getAnimal().getName(),
                feeding.getAnimal().getSpecies().getName(),
                feeding.getAnimal().getEnclosure().getName(),
                feeding.getAnimal().getEnclosure().getHabitatType(),
                feeding.getFedAt(),
                feeding.getFood(),
                feeding.getAmountKg(),
                feeding.getNote(),
                feeding.getRecordedBy().getFullName()
        );
    }
}
