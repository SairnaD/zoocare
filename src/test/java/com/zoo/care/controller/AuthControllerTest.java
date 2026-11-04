package com.zoo.care.controller;

import com.zoo.care.config.SecurityConfig;
import com.zoo.care.dto.RegisterRequest;
import com.zoo.care.exception.BusinessRuleException;
import com.zoo.care.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AuthController.class)
@Import(SecurityConfig.class)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserService userService;

    @Test
    void getLogin_returnsLoginPage() throws Exception {
        mockMvc.perform(get("/login"))
                .andExpect(status().isOk())
                .andExpect(view().name("login"));
    }

    @Test
    void getRegister_returnsRegisterPage() throws Exception {
        mockMvc.perform(get("/register"))
                .andExpect(status().isOk())
                .andExpect(view().name("register"));
    }

    @Test
    void getRegister_shouldAddEmptyForm() throws Exception {
        mockMvc.perform(get("/register"))
                .andExpect(model().attribute("registerRequest", new RegisterRequest(null, null, null, null)));
    }

    @Test
    void postRegister_shouldRedirectAfterRegistrationToLogin() throws Exception {
        mockMvc.perform(post("/register")
                        .with(csrf())
                        .param("username", "Ben")
                        .param("password", "password123")
                        .param("email", "ben@test.com")
                        .param("fullName", "Ben Ten"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?registered"));
        verify(userService).register(any());
    }

    @Test
    void postRegister_invalidEmailShowsFormWithError() throws Exception {
        mockMvc.perform(post("/register")
                        .with(csrf())
                        .param("username", "Ben")
                        .param("password", "password123")
                        .param("email", "ben")
                        .param("fullName", "Ben Ten"))
                .andExpect(model().attributeHasFieldErrors("registerRequest", "email"))
                .andExpect(view().name("register"));
    }

    @Test
    void postRegister_invalidPasswordShowsFormWithError() throws Exception {
        mockMvc.perform(post("/register")
                        .with(csrf())
                        .param("username", "Ben")
                        .param("password", "123")
                        .param("email", "ben@test.com")
                        .param("fullName", "Ben Ten"))
                .andExpect(model().attributeHasFieldErrors("registerRequest", "password"))
                .andExpect(view().name("register"));
    }

    @Test
    void postRegister_invalidDataDoesntCallService() throws Exception {
        mockMvc.perform(post("/register")
                        .with(csrf())
                        .param("username", "")
                        .param("password", "password123")
                        .param("email", "ben@test.com")
                        .param("fullName", "Ben Ten"))
                .andExpect(model().attributeHasFieldErrors("registerRequest", "username"))
                .andExpect(view().name("register"));
        verifyNoInteractions(userService);
    }

    @Test
    void postRegister_takenUsernameShowsFormWithError() throws Exception {
        when(userService.register(any())).thenThrow(new BusinessRuleException("Username already taken"));
        mockMvc.perform(post("/register")
                        .with(csrf())
                        .param("username", "Ben")
                        .param("password", "password123")
                        .param("email", "ben@test.com")
                        .param("fullName", "Ben Ten"))
                .andExpect(model().attributeHasErrors("registerRequest"))
                .andExpect(view().name("register"));
    }
}