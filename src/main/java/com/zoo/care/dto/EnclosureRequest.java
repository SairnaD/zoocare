package com.zoo.care.dto;

import com.zoo.care.entity.HabitatType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record EnclosureRequest(
        @NotBlank
        @Size(max = 50)
        String name,

        @NotNull
        HabitatType habitatType,

        @NotNull
        @Positive
        Integer capacity
) {
}