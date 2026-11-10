package com.zoo.care.controller;

import com.zoo.care.service.FeedingService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.time.ZoneId;

@Controller
@RequiredArgsConstructor
public class FeedingPageController {

    private final FeedingService feedingService;

    @GetMapping({"/admin/feedings", "/keeper/feedings"})
    public String feedingHistoryPage(Authentication authentication, Model model) {
        model.addAttribute("groups", feedingService.getFeedingOverview(authentication.getName()));
        model.addAttribute("displayZone", ZoneId.of("Europe/Riga"));
        return "feedings/history";
    }
}