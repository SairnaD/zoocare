package com.zoo.care.controller;

import com.zoo.care.dto.EnclosureRequest;
import com.zoo.care.dto.EnclosureResponse;
import com.zoo.care.dto.KeeperAssignmentRequest;
import com.zoo.care.entity.HabitatType;
import com.zoo.care.exception.*;
import com.zoo.care.service.EnclosureService;
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
@RequestMapping("/admin/enclosures")
@RequiredArgsConstructor
public class EnclosurePageController {

    private static final String LIST_VIEW = "admin/enclosure/list";
    private static final String FORM_VIEW = "admin/enclosure/form";
    private static final String REDIRECT_TO_LIST = "redirect:/admin/enclosures";
    private static final String KEEPERS_VIEW = "admin/enclosure/keepers";

    private final EnclosureService enclosureService;

    @ModelAttribute("habitatTypes")
    public HabitatType[] habitatTypes() {
        return HabitatType.values();
    }

    @GetMapping
    public String list(Model model) {
        model.addAttribute("enclosures", enclosureService.findAll());
        return LIST_VIEW;
    }

    @GetMapping("/new")
    public String newForm(Model model) {
        model.addAttribute("enclosureRequest", new EnclosureRequest(null, null, null));
        return FORM_VIEW;
    }

    @PostMapping
    public String create(@Valid @ModelAttribute("enclosureRequest") EnclosureRequest request,
                         BindingResult bindingResult,
                         RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            return FORM_VIEW;
        }
        try {
            enclosureService.create(request);
        } catch (EnclosureAlreadyExistsException ex) {
            bindingResult.rejectValue("name", "duplicate", ex.getMessage());
            return FORM_VIEW;
        }
        redirectAttributes.addFlashAttribute("successMessage", "Enclosure created");
        return REDIRECT_TO_LIST;
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        EnclosureResponse enclosure = enclosureService.findById(id);
        model.addAttribute("enclosureId", id);
        model.addAttribute("enclosureRequest",
                new EnclosureRequest(enclosure.name(), enclosure.habitatType(), enclosure.capacity()));
        return FORM_VIEW;
    }

    @PostMapping("/{id}")
    public String update(@PathVariable Long id,
                         @Valid @ModelAttribute("enclosureRequest") EnclosureRequest request,
                         BindingResult bindingResult,
                         Model model,
                         RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("enclosureId", id);
            return FORM_VIEW;
        }
        try {
            enclosureService.update(id, request);
        } catch (EnclosureAlreadyExistsException ex) {
            bindingResult.rejectValue("name", "duplicate", ex.getMessage());
            model.addAttribute("enclosureId", id);
            return FORM_VIEW;
        } catch (EnclosureHabitatLockedException ex) {
            bindingResult.rejectValue("habitatType", "locked", ex.getMessage());
            model.addAttribute("enclosureId", id);
            return FORM_VIEW;
        } catch (EnclosureCapacityTooLowException ex) {
            bindingResult.rejectValue("capacity", "tooLow", ex.getMessage());
            model.addAttribute("enclosureId", id);
            return FORM_VIEW;
        }
        redirectAttributes.addFlashAttribute("successMessage", "Enclosure updated");
        return REDIRECT_TO_LIST;
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            enclosureService.delete(id);
            redirectAttributes.addFlashAttribute("successMessage", "Enclosure deleted");
        } catch (EnclosureNotEmptyException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return REDIRECT_TO_LIST;
    }

    @GetMapping("/{id}/keepers")
    public String keepers(@PathVariable Long id, Model model) {
        model.addAttribute("keeperAssignmentRequest", new KeeperAssignmentRequest(null));
        fillKeepersPage(model, id);
        return KEEPERS_VIEW;
    }

    @PostMapping("/{id}/keepers")
    public String assignKeeper(@PathVariable Long id,
                               @Valid @ModelAttribute("keeperAssignmentRequest") KeeperAssignmentRequest request,
                               BindingResult bindingResult,
                               Model model,
                               RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            fillKeepersPage(model, id);
            return KEEPERS_VIEW;
        }
        try {
            enclosureService.assignKeeper(id, request.keeperId());
        } catch (UserIsNotKeeperException | KeeperAlreadyAssignedException ex) {
            bindingResult.reject("assign.failed", ex.getMessage());
            fillKeepersPage(model, id);
            return KEEPERS_VIEW;
        }
        redirectAttributes.addFlashAttribute("successMessage", "Keeper assigned");
        return "redirect:/admin/enclosures/" + id + "/keepers";
    }

    @PostMapping("/{id}/keepers/{keeperId}/remove")
    public String removeKeeper(@PathVariable Long id,
                               @PathVariable Long keeperId,
                               RedirectAttributes redirectAttributes) {
        enclosureService.unassignKeeper(id, keeperId);
        redirectAttributes.addFlashAttribute("successMessage", "Keeper removed");
        return "redirect:/admin/enclosures/" + id + "/keepers";
    }

    private void fillKeepersPage(Model model, Long enclosureId) {
        model.addAttribute("enclosure", enclosureService.findById(enclosureId));
        model.addAttribute("assignments", enclosureService.findActiveKeepers(enclosureId));
        model.addAttribute("allKeepers", enclosureService.findAllKeepers());
    }
}