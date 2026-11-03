package com.zoo.care.controller;

import com.zoo.care.dto.SpeciesRequest;
import com.zoo.care.dto.SpeciesResponse;
import com.zoo.care.entity.DietType;
import com.zoo.care.entity.HabitatType;
import com.zoo.care.exception.BusinessRuleException;
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
@RequestMapping("/admin/species")
@RequiredArgsConstructor
public class SpeciesPageController {

    private static final String LIST_VIEW = "species/list";
    private static final String FORM_VIEW = "species/form";
    private static final String REDIRECT_TO_LIST = "redirect:/admin/species";

    private final SpeciesService speciesService;

    @GetMapping
    public String list(Model model) {
        model.addAttribute("speciesList", speciesService.findAll());
        return LIST_VIEW;
    }

    @GetMapping("/new")
    public String newForm(Model model) {
        model.addAttribute("speciesRequest", new SpeciesRequest(null, null, null));
        return formView(model, null);
    }

    @PostMapping
    public String create(@Valid @ModelAttribute("speciesRequest") SpeciesRequest request,
                         BindingResult bindingResult,
                         Model model,
                         RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            return formView(model, null);
        }
        try {
            speciesService.create(request);
        } catch (BusinessRuleException ex) {
            bindingResult.rejectValue("name", "duplicate", ex.getMessage());
            return formView(model, null);
        }
        redirectAttributes.addFlashAttribute("successMessage", "Species created");
        return REDIRECT_TO_LIST;
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        SpeciesResponse species = speciesService.findById(id);
        model.addAttribute("speciesRequest",
                new SpeciesRequest(species.name(), species.habitatType(), species.dietType()));
        return formView(model, id);
    }

    @PostMapping("/{id}")
    public String update(@PathVariable Long id,
                         @Valid @ModelAttribute("speciesRequest") SpeciesRequest request,
                         BindingResult bindingResult,
                         Model model,
                         RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            return formView(model, id);
        }
        try {
            speciesService.update(id, request);
        } catch (BusinessRuleException ex) {
            bindingResult.rejectValue("name", "duplicate", ex.getMessage());
            return formView(model, id);
        }
        redirectAttributes.addFlashAttribute("successMessage", "Species updated");
        return REDIRECT_TO_LIST;
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            speciesService.delete(id);
            redirectAttributes.addFlashAttribute("successMessage", "Species deleted");
        } catch (BusinessRuleException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return REDIRECT_TO_LIST;
    }

    private String formView(Model model, Long speciesId) {
        model.addAttribute("speciesId", speciesId);
        model.addAttribute("habitatTypes", HabitatType.values());
        model.addAttribute("dietTypes", DietType.values());
        return FORM_VIEW;
    }
}