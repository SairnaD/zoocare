package com.zoo.care.controller;

import com.zoo.care.service.FeedingService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.time.ZoneId;

@Controller
@RequiredArgsConstructor
public class FeedingPageController {

    private final FeedingService feedingService;
    @Value("${app.display-zone:Europe/Riga}")
    private ZoneId displayZone;

    @GetMapping({"/admin/feedings", "/keeper/feedings"})
    public String feedingHistoryPage(Authentication authentication, Model model) {
        model.addAttribute("groups", feedingService.getFeedingOverview(authentication.getName()));
        model.addAttribute("displayZone", displayZone);
        return "feedings/history";
    }
}