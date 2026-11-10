package com.zoo.care.controller;

import com.zoo.care.dto.AnimalRequest;
import com.zoo.care.dto.AnimalResponse;
import com.zoo.care.exception.*;
import com.zoo.care.service.AnimalService;
import com.zoo.care.service.EnclosureService;
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
        return formView(model, null);
    }

    @PostMapping
    public String create(@Valid @ModelAttribute("animalRequest") AnimalRequest request,
                         BindingResult bindingResult,
                         Model model,
                         RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            return formView(model, null);
        }
        try {
            animalService.create(request);
        } catch (AnimalAlreadyExistsException ex) {
            bindingResult.rejectValue("name", "duplicate", ex.getMessage());
            return formView(model, null);
        } catch (HabitatMismatchException | EnclosureFullException ex) {
            bindingResult.rejectValue("enclosureId", "placement", ex.getMessage());
            return formView(model, null);
        }
        redirectAttributes.addFlashAttribute("successMessage", "Animal registered");
        return REDIRECT_TO_LIST;
    }

    @GetMapping("/{id}")
    public String profile(@PathVariable Long id, Model model) {
        model.addAttribute("animal", animalService.findById(id));
        model.addAttribute("enclosures", enclosureService.findAll());
        return PROFILE_VIEW;
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        model.addAttribute("animalRequest", toRequest(animalService.findById(id)));
        return formView(model, id);
    }

    @PostMapping("/{id}")
    public String update(@PathVariable Long id,
                         @Valid @ModelAttribute("animalRequest") AnimalRequest request,
                         BindingResult bindingResult,
                         Model model,
                         RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            return formView(model, id);
        }
        try {
            animalService.update(id, request);
        } catch (AnimalAlreadyExistsException ex) {
            bindingResult.rejectValue("name", "duplicate", ex.getMessage());
            return formView(model, id);
        } catch (HabitatMismatchException | EnclosureFullException ex) {
            bindingResult.rejectValue("enclosureId", "placement", ex.getMessage());
            return formView(model, id);
        } catch (AnimalDeletedException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
            return REDIRECT_TO_LIST;
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
            redirectAttributes.addFlashAttribute("successMessage", "Animal moved to " + moved.enclosureName());
        } catch (SameEnclosureException | HabitatMismatchException
                 | EnclosureFullException | AnimalDeletedException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return redirectToProfile(id);
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            animalService.delete(id);
        } catch (AnimalInUseException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
            return redirectToProfile(id);
        }
        redirectAttributes.addFlashAttribute("successMessage", "Animal deleted");
        return REDIRECT_TO_LIST;
    }

    private String formView(Model model, Long animalId) {
        model.addAttribute("animalId", animalId); // null on the "new animal" form
        model.addAttribute("speciesList", speciesService.findAll());
        model.addAttribute("enclosures", enclosureService.findAll());
        return FORM_VIEW;
    }

    private String redirectToProfile(Long id) {
        return "redirect:/admin/animals/" + id;
    }

    private AnimalRequest toRequest(AnimalResponse animal) {
        return new AnimalRequest(animal.name(), animal.speciesId(), animal.enclosureId(),
                animal.birthDate(), animal.weight(), animal.feedingIntervalHours(), animal.description());
    }
}