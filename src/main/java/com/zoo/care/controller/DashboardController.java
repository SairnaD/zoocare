package com.zoo.care.controller;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;


@Controller
public class DashboardController {

    @GetMapping("/dashboard")
    public String dashboardPage(Authentication authentication) {
       if(authentication.getAuthorities().stream().anyMatch(n -> n.getAuthority().equals("ROLE_ADMIN"))){
           return "redirect:/admin";
       }
        return "redirect:/keeper";
    }

    @GetMapping("/keeper")
    public String keeperDashboardPage(){
        return "keeper/dashboard";
    }

    @GetMapping("/admin")
    public String adminDashboardPage(){
        return "admin/dashboard";
    }
}

