package com.zoo.care.controller;

import com.zoo.care.dto.AnimalRequest;
import com.zoo.care.exception.BusinessRuleException;
import com.zoo.care.service.AnimalService;
import com.zoo.care.service.EnclosureService;
import com.zoo.care.service.FileStorageService;
import com.zoo.care.service.SpeciesService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin/animals")
@RequiredArgsConstructor
public class AnimalPageController {

    private static final String LIST_VIEW = "admin/animals/list";
    private static final String FORM_VIEW = "admin/animals/form";
    private static final String PROFILE_VIEW = "admin/animals/profile";
    private static final String REDIRECT_TO_LIST = "redirect:/admin/animals";

    private final AnimalService animalService;
    private final SpeciesService speciesService;
    private final EnclosureService enclosureService;
    private final FileStorageService fileStorageService;

    @GetMapping
    public String list(Model model) {
        model.addAttribute("animals", animalService.findAll());
        return LIST_VIEW;
    }

    @GetMapping("/new")
    public String newForm(Model model) {
        model.addAttribute("animalRequest",
                new AnimalRequest(null, null, null, null, null, null, null, null, null));
        populateOptions(model);
        return FORM_VIEW;
    }

    @PostMapping
    public String create(@Valid @ModelAttribute("animalRequest") AnimalRequest request,
                         BindingResult bindingResult,
                         Model model,
                         RedirectAttributes redirectAttributes) {
        String photoError = fileStorageService.validate(request.photo());
        if (photoError != null) {
            bindingResult.rejectValue("photo", "invalid", photoError);
        }
        if (bindingResult.hasErrors()) {
            populateOptions(model);
            return FORM_VIEW;
        }
        try {
            animalService.create(request);
        } catch (BusinessRuleException ex) {
            bindingResult.reject("business", ex.getMessage());
            populateOptions(model);
            return FORM_VIEW;
        }
        redirectAttributes.addFlashAttribute("successMessage", "Animal registered");
        return REDIRECT_TO_LIST;
    }

    @GetMapping("/{id}")
    public String profile(@PathVariable Long id, Model model) {
        model.addAttribute("animal", animalService.findById(id));
        return PROFILE_VIEW;
    }

    private void populateOptions(Model model) {
        model.addAttribute("speciesList", speciesService.findAll());
        model.addAttribute("enclosures", enclosureService.findAll());
    }
}