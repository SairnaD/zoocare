package com.zoo.care.controller;

import com.zoo.care.dto.AnimalRequest;
import com.zoo.care.dto.AnimalResponse;
import com.zoo.care.dto.SpeciesRequest;
import com.zoo.care.exception.BusinessRuleException;
import com.zoo.care.service.AnimalService;
import com.zoo.care.service.EnclosureService;
import com.zoo.care.service.SpeciesService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
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

    @GetMapping
    public String list(Model model) {
        model.addAttribute("animals", animalService.findAll());
        return LIST_VIEW;
    }

    @GetMapping("/new")
    public String newForm(Model model) {
        model.addAttribute("animalRequest",
                new AnimalRequest(null, null, null, null, null, null, null));
        populateOptions(model);
        return FORM_VIEW;
    }

    @PostMapping
    public String create(@Valid @ModelAttribute("animalRequest") AnimalRequest request,
                         BindingResult bindingResult,
                         Model model,
                         RedirectAttributes redirectAttributes) {
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
        model.addAttribute("enclosures", enclosureService.findAll());   // for the "move" dropdown
        return PROFILE_VIEW;
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        AnimalResponse animal = animalService.findById(id);
        model.addAttribute("animalRequest", toRequest(animal));
        model.addAttribute("animalId", id);          // its presence switches the form into edit mode
        populateOptions(model);
        return FORM_VIEW;
    }

    @PostMapping("/{id}")
    public String update(@PathVariable Long id,
                         @Valid @ModelAttribute("animalRequest") AnimalRequest request,
                         BindingResult bindingResult,
                         Model model,
                         RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("animalId", id);
            populateOptions(model);
            return FORM_VIEW;
        }
        try {
            animalService.update(id, request);
        } catch (BusinessRuleException ex) {
            bindingResult.reject("business", ex.getMessage());
            model.addAttribute("animalId", id);
            populateOptions(model);
            return FORM_VIEW;
        }
        redirectAttributes.addFlashAttribute("successMessage", "Animal updated");
        return redirectToProfile(id);
    }

    @PostMapping("/{id}/transfer")
    public String transfer(@PathVariable Long id,
                           @RequestParam(required = false) Long enclosureId,
                           RedirectAttributes redirectAttributes) {
        if (enclosureId == null) {
            redirectAttributes.addFlashAttribute("errorMessage", "Choose an enclosure to move the animal to");
            return redirectToProfile(id);
        }
        try {
            AnimalResponse moved = animalService.transferAnimal(id, enclosureId);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Animal moved to " + moved.enclosureName());
        } catch (BusinessRuleException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return redirectToProfile(id);
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            animalService.delete(id);
        } catch (DataIntegrityViolationException ex) {
            redirectAttributes.addFlashAttribute("errorMessage",
                    "This animal can't be deleted because other records refer to it");
            return redirectToProfile(id);
        }
        redirectAttributes.addFlashAttribute("successMessage", "Animal deleted");
        return REDIRECT_TO_LIST;
    }

    private String redirectToProfile(Long id) {
        return "redirect:/admin/animals/" + id;
    }

    private AnimalRequest toRequest(AnimalResponse animal) {
        return new AnimalRequest(
                animal.name(),
                animal.speciesId(),
                animal.enclosureId(),
                animal.birthDate(),
                animal.weight(),
                animal.feedingIntervalHours(),
                animal.description());
    }

    private void populateOptions(Model model) {
        model.addAttribute("speciesList", speciesService.findAll());
        model.addAttribute("enclosures", enclosureService.findAll());
    }
}