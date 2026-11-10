package com.zoo.care.dto;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record FeedingRequest(
        @NotBlank
        @Size(max = 100)
        String food,

        @NotNull
        @Positive
        @Digits(integer = 7, fraction = 3)
        BigDecimal amountKg,

        @Size(max = 500)
        String note
) {
}
