package com.zoo.care.controller;

import com.zoo.care.config.SecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@WebMvcTest(DashboardController.class)
@Import(SecurityConfig.class)
class DashboardControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @WithMockUser(roles = "ADMIN")
    void dashboard_adminIsRedirectedToEnclosures() throws Exception {
        mockMvc.perform(get("/dashboard"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/enclosures"));
    }

    @Test
    @WithMockUser(roles = "KEEPER")
    void dashboard_keeperIsRedirectedToKeeperPage() throws Exception {
        mockMvc.perform(get("/dashboard"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/keeper"));
    }

    @Test
    void dashboard_anonymousIsRedirectedToLogin() throws Exception {
        mockMvc.perform(get("/dashboard"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
    }

    @Test
    @WithMockUser(roles = "KEEPER")
    void adminArea_keeperIsForbidden() throws Exception {
        mockMvc.perform(get("/admin/enclosures"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "KEEPER")
    void keeperPage_keeperSeesKeeperDashboard() throws Exception {
        mockMvc.perform(get("/keeper"))
                .andExpect(status().isOk())
                .andExpect(view().name("keeper/dashboard"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void keeperPage_adminIsForbidden() throws Exception {
        mockMvc.perform(get("/keeper"))
                .andExpect(status().isForbidden());
    }
}