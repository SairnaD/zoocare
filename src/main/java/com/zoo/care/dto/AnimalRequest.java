package com.zoo.care.dto;

import jakarta.validation.constraints.*;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.multipart.MultipartFile;

import java.time.Duration;
import java.time.LocalDate;

public record AnimalRequest(
    @NotBlank
    @Size(min = 3, max = 50)
    String name,

    @NotNull
    Long speciesId,

    @NotNull
    Long enclosureId,

    @PastOrPresent
    @NotNull
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    LocalDate birthDate,

    @NotNull @Positive
    Float weight,

    @NotNull @Positive
    Integer size,

    @NotNull @Min(1) @Max(24)
    Integer feedingIntervalHours,

    @Size(max = 500)
    String description,

    MultipartFile photo
) {

}

