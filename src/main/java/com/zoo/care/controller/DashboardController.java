package com.zoo.care.controller;

import com.zoo.care.dto.FeedingRequest;
import com.zoo.care.exception.BusinessRuleException;
import com.zoo.care.exception.FeedingTooEarlyException;
import com.zoo.care.service.FeedingService;
import com.zoo.care.service.KeeperDashboardService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.*;
import java.time.temporal.ChronoUnit;

@Controller
@RequiredArgsConstructor
public class DashboardController {

    private final KeeperDashboardService keeperDashboardService;
    private final FeedingService feedingService;
    private final Clock clock;

    @Value("${app.display-zone:Europe/Riga}")
    private ZoneId displayZone;

    @GetMapping("/dashboard")
    public String dashboardPage(Authentication authentication) {
        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals("ROLE_ADMIN"));
        return isAdmin ? "redirect:/admin/enclosures" : "redirect:/keeper";
    }

    @GetMapping("/keeper")
    public String keeperDashboardPage(Authentication authentication, Model model) {
        model.addAttribute("animals", keeperDashboardService.findAnimalsForKeeper(authentication.getName()));
        model.addAttribute("displayZone", displayZone);
        model.addAttribute("currentTime", LocalTime.now(clock.withZone(displayZone)).truncatedTo(ChronoUnit.MINUTES));
        return "keeper/dashboard";
    }

    @PostMapping("/keeper/animals/{animalId}/feedings")
    public String recordFeeding(@PathVariable Long animalId,
                                @Valid @ModelAttribute FeedingRequest request,
                                BindingResult bindingResult,
                                @RequestParam @DateTimeFormat(pattern = "HH:mm") LocalTime fedAt,
                                Authentication authentication,
                                RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("feedError", "Please enter the food and a positive amount.");
            return "redirect:/keeper";
        }
        Instant fedAtInstant = LocalDate.now(clock.withZone(displayZone)).atTime(fedAt)
                .atZone(displayZone).toInstant();
        try {
            feedingService.recordFeeding(animalId, request, authentication.getName(), fedAtInstant);
            redirectAttributes.addFlashAttribute("fedAnimalId", animalId);
        } catch (FeedingTooEarlyException e) {
            redirectAttributes.addFlashAttribute("tooEarlyAnimalId", animalId);
        } catch (BusinessRuleException e) {
            redirectAttributes.addFlashAttribute("feedError", e.getMessage());
        }
        return "redirect:/keeper";
    }

    @GetMapping("/access-denied")
    public String accessDeniedPage() {
        return "access-denied";
    }
}