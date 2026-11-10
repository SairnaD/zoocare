package com.zoo.care.controller;

import com.zoo.care.dto.EnclosureRequest;
import com.zoo.care.dto.EnclosureResponse;
import com.zoo.care.dto.KeeperAssignmentRequest;
import com.zoo.care.dto.KeeperAssignmentResponse;
import com.zoo.care.service.EnclosureService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
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

@Tag(name = "Enclosures", description = "Manage enclosures and their keepers (ADMIN only)")
@RestController
@RequestMapping("/api/enclosures")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class EnclosureApiController {

    private final EnclosureService enclosureService;

    @Operation(summary = "List all enclosures, sorted by name")
    @GetMapping
    public List<EnclosureResponse> findAll() {
        return enclosureService.findAll();
    }

    @Operation(summary = "Get an enclosure by id")
    @ApiResponse(responseCode = "404", description = "Enclosure not found")
    @GetMapping("/{id}")
    public EnclosureResponse findById(@PathVariable Long id) {
        return enclosureService.findById(id);
    }

    @Operation(summary = "Create an enclosure")
    @ApiResponse(responseCode = "400", description = "Validation failed")
    @ApiResponse(responseCode = "409", description = "Name already exists")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public EnclosureResponse create(@Valid @RequestBody EnclosureRequest request) {
        return enclosureService.create(request);
    }

    @Operation(summary = "Update an enclosure")
    @ApiResponse(responseCode = "400", description = "Validation failed")
    @ApiResponse(responseCode = "404", description = "Enclosure not found")
    @ApiResponse(responseCode = "409", description = "Name taken, or habitat/capacity conflicts with its animals")
    @PutMapping("/{id}")
    public EnclosureResponse update(@PathVariable Long id, @Valid @RequestBody EnclosureRequest request) {
        return enclosureService.update(id, request);
    }

    @Operation(summary = "Delete an enclosure")
    @ApiResponse(responseCode = "404", description = "Enclosure not found")
    @ApiResponse(responseCode = "409", description = "Enclosure still has animals")
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        enclosureService.delete(id);
    }

    @Operation(summary = "List keepers currently assigned to an enclosure")
    @ApiResponse(responseCode = "404", description = "Enclosure not found")
    @GetMapping("/{id}/keepers")
    public List<KeeperAssignmentResponse> findKeepers(@PathVariable Long id) {
        return enclosureService.findActiveKeepers(id);
    }

    @Operation(summary = "Assign a keeper to an enclosure")
    @ApiResponse(responseCode = "400", description = "Validation failed")
    @ApiResponse(responseCode = "404", description = "Enclosure or user not found")
    @ApiResponse(responseCode = "409", description = "User is not a keeper or is already assigned")
    @PostMapping("/{id}/keepers")
    @ResponseStatus(HttpStatus.CREATED)
    public KeeperAssignmentResponse assignKeeper(@PathVariable Long id,
                                                 @Valid @RequestBody KeeperAssignmentRequest request) {
        return enclosureService.assignKeeper(id, request.keeperId());
    }

    @Operation(summary = "Remove a keeper from an enclosure")
    @ApiResponse(responseCode = "404", description = "Enclosure not found or keeper is not assigned")
    @DeleteMapping("/{id}/keepers/{keeperId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void unassignKeeper(@PathVariable Long id, @PathVariable Long keeperId) {
        enclosureService.unassignKeeper(id, keeperId);
    }
}