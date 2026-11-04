package com.zoo.care.controller;

import com.zoo.care.dto.EnclosureRequest;
import com.zoo.care.dto.EnclosureResponse;
import com.zoo.care.entity.HabitatType;
import com.zoo.care.exception.BusinessRuleException;
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

    private final EnclosureService enclosureService;

    @GetMapping
    public String list(Model model) {
        model.addAttribute("enclosures", enclosureService.findAll());
        return LIST_VIEW;
    }

    @GetMapping("/new")
    public String newForm(Model model) {
        model.addAttribute("enclosureRequest", new EnclosureRequest(null, null, null));
        return formView(model, null);
    }

    @PostMapping
    public String create(@Valid @ModelAttribute("enclosureRequest") EnclosureRequest request,
                         BindingResult bindingResult,
                         Model model,
                         RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            return formView(model, null);
        }
        try {
            enclosureService.create(request);
        } catch (BusinessRuleException ex) {
            bindingResult.rejectValue("name", "duplicate", ex.getMessage());
            return formView(model, null);
        }
        redirectAttributes.addFlashAttribute("successMessage", "Enclosure created");
        return REDIRECT_TO_LIST;
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        EnclosureResponse enclosure = enclosureService.findById(id);
        model.addAttribute("enclosureRequest",
                new EnclosureRequest(enclosure.name(), enclosure.habitatType(), enclosure.capacity()));
        return formView(model, id);
    }

    @PostMapping("/{id}")
    public String update(@PathVariable Long id,
                         @Valid @ModelAttribute("enclosureRequest") EnclosureRequest request,
                         BindingResult bindingResult,
                         Model model,
                         RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            return formView(model, id);
        }
        try {
            enclosureService.update(id, request);
        } catch (BusinessRuleException ex) {
            bindingResult.rejectValue("name", "duplicate", ex.getMessage());
            return formView(model, id);
        }
        redirectAttributes.addFlashAttribute("successMessage", "Enclosure updated");
        return REDIRECT_TO_LIST;
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            enclosureService.delete(id);
            redirectAttributes.addFlashAttribute("successMessage", "Enclosure deleted");
        } catch (BusinessRuleException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return REDIRECT_TO_LIST;
    }

    private String formView(Model model, Long enclosureId) {
        model.addAttribute("enclosureId", enclosureId);
        model.addAttribute("habitatTypes", HabitatType.values());
        return FORM_VIEW;
    }
}