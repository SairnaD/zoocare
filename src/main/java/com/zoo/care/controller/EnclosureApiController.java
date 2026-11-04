package com.zoo.care.controller;

import com.zoo.care.dto.EnclosureRequest;
import com.zoo.care.dto.EnclosureResponse;
import com.zoo.care.service.EnclosureService;
import io.swagger.v3.oas.annotations.Operation;
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

@Tag(name = "Enclosures", description = "Manage zoo enclosures (ADMIN only)")
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
    @GetMapping("/{id}")
    public EnclosureResponse findById(@PathVariable Long id) {
        return enclosureService.findById(id);
    }

    @Operation(summary = "Create a new enclosure")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public EnclosureResponse create(@Valid @RequestBody EnclosureRequest request) {
        return enclosureService.create(request);
    }

    @Operation(summary = "Update an existing enclosure")
    @PutMapping("/{id}")
    public EnclosureResponse update(@PathVariable Long id, @Valid @RequestBody EnclosureRequest request) {
        return enclosureService.update(id, request);
    }

    @Operation(summary = "Delete an enclosure")
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        enclosureService.delete(id);
    }
}