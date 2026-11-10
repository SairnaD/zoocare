package com.zoo.care.controller;

import com.zoo.care.service.KeeperDashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
@RequiredArgsConstructor
public class DashboardController {

    private final KeeperDashboardService keeperDashboardService;

    @GetMapping("/dashboard")
    public String dashboardPage(Authentication authentication) {
        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals("ROLE_ADMIN"));
        return isAdmin ? "redirect:/admin/enclosures" : "redirect:/keeper";
    }

    @GetMapping("/keeper")
    public String keeperDashboardPage(Authentication authentication, Model model) {
        model.addAttribute("enclosures", keeperDashboardService.findForKeeper(authentication.getName()));
        return "keeper/dashboard";
    }

    @GetMapping("/access-denied")
    public String accessDeniedPage() {
        return "access-denied";
    }
}