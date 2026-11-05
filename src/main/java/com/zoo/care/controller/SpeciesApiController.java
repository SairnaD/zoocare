package com.zoo.care.controller;

import com.zoo.care.dto.SpeciesRequest;
import com.zoo.care.dto.SpeciesResponse;
import com.zoo.care.service.SpeciesService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import com.zoo.care.exception.ErrorResponse;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "Species", description = "Animal species (reading: any logged-in user, changes: admin only)")
@RestController
@RequestMapping("/api/species")
@RequiredArgsConstructor
public class SpeciesApiController {

    private final SpeciesService speciesService;

    @Operation(summary = "List all species")
    @GetMapping
    public List<SpeciesResponse> findAll() {
        return speciesService.findAll();
    }

    @Operation(summary = "Get a species by id")
    @ApiResponse(responseCode = "404", description = "Species not found",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @GetMapping("/{id}")
    public SpeciesResponse findById(@PathVariable Long id) {
        return speciesService.findById(id);
    }

    @Operation(summary = "Create a species (admin)")
    @ApiResponse(responseCode = "400", description = "Validation failed",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "409", description = "Name already exists",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN')")
    public SpeciesResponse create(@Valid @RequestBody SpeciesRequest request) {
        return speciesService.create(request);
    }

    @Operation(summary = "Update a species (admin)")
    @ApiResponse(responseCode = "404", description = "Species not found",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "409", description = "Name already exists",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public SpeciesResponse update(@PathVariable Long id, @Valid @RequestBody SpeciesRequest request) {
        return speciesService.update(id, request);
    }

    @Operation(summary = "Delete a species (admin)")
    @ApiResponse(responseCode = "404", description = "Species not found",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "409", description = "Species is in use",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('ADMIN')")
    public void delete(@PathVariable Long id) {
        speciesService.delete(id);
    }
}