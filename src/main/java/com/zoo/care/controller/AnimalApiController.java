package com.zoo.care.controller;
import com.zoo.care.dto.AnimalResponse;
import com.zoo.care.service.AnimalService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/animals")

public class AnimalApiController {
    private final AnimalService animalService;
    @GetMapping
    public List<AnimalResponse> getAllAnimals() {
        return animalService.getAllAnimals();
    }
}
