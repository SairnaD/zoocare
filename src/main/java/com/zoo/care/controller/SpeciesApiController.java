package com.zoo.care.controller;

import com.zoo.care.dto.SpeciesRequest;
import com.zoo.care.dto.SpeciesResponse;
import com.zoo.care.service.SpeciesService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
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

@RestController
@RequestMapping("/api/species")
@RequiredArgsConstructor
public class SpeciesApiController {

    private final SpeciesService speciesService;

    @GetMapping
    public List<SpeciesResponse> findAll() {
        return speciesService.findAll();
    }

    @GetMapping("/{id}")
    public SpeciesResponse findById(@PathVariable Long id) {
        return speciesService.findById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public SpeciesResponse create(@Valid @RequestBody SpeciesRequest request) {
        return speciesService.create(request);
    }

    @PutMapping("/{id}")
    public SpeciesResponse update(@PathVariable Long id, @Valid @RequestBody SpeciesRequest request) {
        return speciesService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        speciesService.delete(id);
    }
}