package com.zoo.care.dto;

import com.zoo.care.entity.HabitatType;
import jakarta.validation.constraints.*;

public record EnclosureRequest(
        @NotBlank
        @Size(max = 50)
        String name,

        @NotNull
        HabitatType habitatType,

        @NotNull
        @Min(1)
        @Max(1000)
        Integer capacity
) {
}