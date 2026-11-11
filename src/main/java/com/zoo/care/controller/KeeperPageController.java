package com.zoo.care.controller;

import com.zoo.care.service.KeeperService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/admin/keepers")
@RequiredArgsConstructor
public class KeeperPageController {

    private final KeeperService keeperService;

    @GetMapping
    public String list(Model model) {
        model.addAttribute("keepers", keeperService.findAllWithOverview());
        return "admin/keepers/list";
    }
}