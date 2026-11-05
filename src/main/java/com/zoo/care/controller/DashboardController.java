package com.zoo.care.controller;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class DashboardController {

    @GetMapping("/dashboard")
    public String dashboardPage(Authentication authentication) {
        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals("ROLE_ADMIN"));
        return isAdmin ? "redirect:/admin" : "redirect:/keeper";
    }

    @GetMapping("/keeper")
    public String keeperDashboardPage() {
        return "keeper/dashboard";
    }

    @GetMapping("/admin")
    public String adminDashboardPage() {
        return "admin/dashboard";
    }

    @GetMapping("/access-denied")
    public String accessDeniedPage() {
        return "access-denied";
    }
}