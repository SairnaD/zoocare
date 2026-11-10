package com.zoo.care.dto;

import com.zoo.care.entity.Feedings;

import java.math.BigDecimal;
import java.time.Instant;

public record FeedingResponse(
        Long id,
        Long animalId,
        Long recordedById,
        String food,
        BigDecimal amountKg,
        Instant fedAt,
        String note
) {
    public static FeedingResponse from(Feedings feeding) {
        return new FeedingResponse(
                feeding.getId(),
                feeding.getAnimal().getId(),
                feeding.getRecordedBy().getId(),
                feeding.getFood(),
                feeding.getAmountKg(),
                feeding.getFedAt(),
                feeding.getNote()
        );
    }
}
