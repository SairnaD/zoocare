package com.zoo.care.controller;

import com.zoo.care.dto.AnimalResponse;
import com.zoo.care.exception.ErrorResponse;
import com.zoo.care.service.AnimalService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "Animals", description = "Zoo animals (ADMIN only for now)")
@RestController
@RequestMapping("/api/animals")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AnimalApiController {

    private final AnimalService animalService;

    @Operation(summary = "List all animals, sorted by name")
    @GetMapping
    public List<AnimalResponse> findAll() {
        return animalService.findAll();
    }

    @Operation(summary = "Get an animal by id")
    @ApiResponse(responseCode = "404", description = "Animal not found",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @GetMapping("/{id}")
    public AnimalResponse findById(@PathVariable Long id) {
        return animalService.findById(id);
    }
}