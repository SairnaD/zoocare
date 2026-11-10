package com.zoo.care.controller;


import com.zoo.care.dto.FeedingRequest;
import com.zoo.care.dto.FeedingResponse;
import com.zoo.care.exception.ErrorResponse;
import com.zoo.care.service.FeedingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;


@Tag(name = "Feedings", description = "Record animal feedings")
@RestController
@RequestMapping("/api/animals/{animalId}/feedings")
@RequiredArgsConstructor
public class FeedingApiController {

    private final FeedingService feedingService;

    @Operation(summary = "Record a feeding for an assigned animal")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "201",
                    description = "Feeding recorded",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = FeedingResponse.class))),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid request data",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(
                    responseCode = "403",
                    description = "User is not a keeper or is not assigned to the enclosure",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(
                    responseCode = "404",
                    description = "Animal not found",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(
                    responseCode = "409",
                    description = "Feeding interval has not passed or animal is deleted",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping
    @PreAuthorize("hasRole('KEEPER')")
    @ResponseStatus(HttpStatus.CREATED)
    public FeedingResponse recordFeeding(
            @PathVariable Long animalId,
            @Valid @RequestBody FeedingRequest request,
            Authentication authentication) {

        return feedingService.recordFeeding(
                animalId,
                request,
                authentication.getName());
    }
}
