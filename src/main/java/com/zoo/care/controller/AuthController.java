package com.zoo.care.controller;

import com.zoo.care.dto.RegisterRequest;
import com.zoo.care.exception.EmailAlreadyRegisteredException;
import com.zoo.care.exception.UsernameAlreadyTakenException;
import com.zoo.care.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;


@Controller
@RequiredArgsConstructor
public class AuthController {
    private final UserService userService;

    @GetMapping("/login")
    public String loginPage() {
        return "login";
    }

    @GetMapping("/register")
    public String registerPage(Model model) {
        model.addAttribute("registerRequest", new RegisterRequest(null, null, null, null));
        return "register";
    }

    @PostMapping("/register")
    public String registerUser(@Valid @ModelAttribute("registerRequest") RegisterRequest regRequest, BindingResult binResult) {
        if (binResult.hasErrors()) {
            return "register";
        }
        try {
            userService.register(regRequest);
            return "redirect:/login?registered";
        } catch (UsernameAlreadyTakenException e) {
            binResult.rejectValue("username", "duplicate", e.getMessage());
            return "register";
        } catch (EmailAlreadyRegisteredException e) {
            binResult.rejectValue("email", "duplicate", e.getMessage());
            return "register";
        }
    }
}

