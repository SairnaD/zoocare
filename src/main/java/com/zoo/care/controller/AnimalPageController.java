package com.zoo.care.controller;

import com.zoo.care.dto.AnimalRequest;
import com.zoo.care.exception.BusinessRuleException;
import com.zoo.care.service.AnimalService;
import com.zoo.care.service.EnclosureService;
import com.zoo.care.service.FileStorageService;
import com.zoo.care.service.SpeciesService;
import jakarta.servlet.http.HttpSession;
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
    private final HttpSession httpSession;

    @GetMapping
    public String list(Model model) {
        model.addAttribute("animals", animalService.findAll());
        return LIST_VIEW;
    }

    @GetMapping("/new")
    public String newForm(Model model) {
        // Start with a clean form: drop a photo left over from an abandoned attempt
        fileStorageService.delete((String) httpSession.getAttribute("pendingPhoto"));
        httpSession.removeAttribute("pendingPhoto");
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
        // Browsers can't refill file inputs after a failed submit, so a valid photo
        // is stored right away and kept in the session for the next attempt.
        // A newly chosen photo replaces the one from the previous attempt.
        if (request.photo() != null && !request.photo().isEmpty() && photoError == null) {
            // The previous pending photo is deleted when a new one replaces it.
            fileStorageService.delete((String) httpSession.getAttribute("pendingPhoto"));
            httpSession.setAttribute("pendingPhoto", fileStorageService.store(request.photo()));
        }
        // The new photo, the one kept from a previous attempt or null
        String photoFile = (String) httpSession.getAttribute("pendingPhoto");
        if (bindingResult.hasErrors()) {
            populateOptions(model);
            model.addAttribute("pendingPhoto", photoFile);
            return FORM_VIEW;
        }
        try {
            animalService.create(request, photoFile);
            // The photo now belongs to the saved animal, so it must not carry over to the next one
            httpSession.removeAttribute("pendingPhoto");
        } catch (BusinessRuleException ex) {
            bindingResult.reject("business", ex.getMessage());
            populateOptions(model);
            model.addAttribute("pendingPhoto", photoFile);
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