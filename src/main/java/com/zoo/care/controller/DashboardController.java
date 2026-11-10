package com.zoo.care.controller;

import com.zoo.care.service.KeeperDashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.time.ZoneId;

@Controller
@RequiredArgsConstructor
public class DashboardController {

    private final KeeperDashboardService keeperDashboardService;

    @Value("${app.display-zone:Europe/Riga}")
    private String displayZone;

    @GetMapping("/dashboard")
    public String dashboardPage(Authentication authentication) {
        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals("ROLE_ADMIN"));
        return isAdmin ? "redirect:/admin/enclosures" : "redirect:/keeper";
    }

    @GetMapping("/keeper")
    public String keeperDashboardPage(Authentication authentication, Model model) {
        model.addAttribute("dueAnimals", keeperDashboardService.findDueForKeeper(authentication.getName()));
        model.addAttribute("displayZone", ZoneId.of(displayZone));
        return "keeper/dashboard";
    }

    @GetMapping("/access-denied")
    public String accessDeniedPage() {
        return "access-denied";
    }
}