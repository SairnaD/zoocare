package com.zoo.care.controller;

import com.zoo.care.exception.BusinessRuleException;
import com.zoo.care.exception.ResourceNotFoundException;
import com.zoo.care.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequiredArgsConstructor
@RequestMapping("/admin/keepers")
public class KeeperAdminController {

    private static final String REDIRECT_TO_KEEPERS = "redirect:/admin/keepers";

    private final UserService userService;

    @PostMapping("/{id}/delete")
    public String deleteKeeper(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            userService.deleteKeeper(id);
            redirectAttributes.addFlashAttribute("successMessage", "Keeper deleted");
        } catch (ResourceNotFoundException | BusinessRuleException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return REDIRECT_TO_KEEPERS;
    }

    @PostMapping("/{id}/make-admin")
    public String makeAdmin(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            userService.makeAdmin(id);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Keeper promoted to admin");
        } catch (ResourceNotFoundException | BusinessRuleException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return REDIRECT_TO_KEEPERS;
    }
}