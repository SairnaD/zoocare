package com.zoo.care.controller;

import com.zoo.care.dto.AnimalRequest;
import com.zoo.care.exception.BusinessRuleException;
import com.zoo.care.repository.EnclosureRepository;
import com.zoo.care.repository.SpeciesRepository;
import com.zoo.care.service.AnimalService;
import com.zoo.care.service.FileStorageService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

@Controller
@RequiredArgsConstructor
public class AnimalPageController {

    private final AnimalService animalService;
    private final FileStorageService fileStorageService;
    private final SpeciesRepository speciesRepository;
    private final EnclosureRepository enclosureRepository;

    @GetMapping("/animals")
    public String listAnimals(Model model) {
        model.addAttribute("animals", animalService.getAllAnimals());
        return "animals/animals";
    }

    @GetMapping("/addAnimal")
    public String showForm(Model model) {
        model.addAttribute("createAnimalRequest",
                new AnimalRequest(null, null, null, null, null, null, null, null, null));
        populateOptions(model);
        return "animals/addAnimal";
    }

    @PostMapping("/addAnimal")
    public String createAnimal(@Valid @ModelAttribute("createAnimalRequest") AnimalRequest animalRequest,
                               BindingResult bindingResult, Model model) {
        String photoError = fileStorageService.validate(animalRequest.photo());
        if (photoError != null) {
            bindingResult.rejectValue("photo", "invalid", photoError);
        }
        if (bindingResult.hasErrors()) {
            populateOptions(model);
            return "animals/addAnimal";
        }
        try {
            animalService.createAnimal(animalRequest);
            return "redirect:/animals?created";
        } catch (BusinessRuleException e) {
            bindingResult.rejectValue("name", "duplicate", e.getMessage());
            populateOptions(model);
            return "animals/addAnimal";
        }
    }

    private void populateOptions(Model model) {
        model.addAttribute("speciesList", speciesRepository.findAll());
        model.addAttribute("enclosures", enclosureRepository.findAll());
    }
}