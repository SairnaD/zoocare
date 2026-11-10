package com.zoo.care.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

public record AnimalRequest(
        @NotBlank
        @Size(min = 2, max = 50)
        String name,

        @NotNull
        Long speciesId,

        @NotNull
        Long enclosureId,

        @NotNull
        @PastOrPresent
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        LocalDate birthDate,

        @NotNull @Positive
        Float weight,

        @NotNull @Min(1) @Max(720)
        Integer feedingIntervalHours,

        @Size(max = 500)
        String description
) {
}