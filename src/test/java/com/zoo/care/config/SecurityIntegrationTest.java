package com.zoo.care.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.authenticated;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.unauthenticated;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@SpringBootTest
class SecurityIntegrationTest {

    @Autowired
    private WebApplicationContext context;

    @Value("${app.admin.password}")
    private String adminPassword;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(springSecurity())
                .build();
    }

    @Test
    void publicPagesAreOpenToAnonymousUsers() throws Exception {
        mockMvc.perform(get("/")).andExpect(status().isOk()).andExpect(view().name("index"));
        mockMvc.perform(get("/login")).andExpect(status().isOk());
        mockMvc.perform(get("/register")).andExpect(status().isOk());
        mockMvc.perform(get("/access-denied")).andExpect(status().isOk());
    }

    @Test
    void protectedPageRedirectsAnonymousUserToLogin() throws Exception {
        mockMvc.perform(get("/admin/anything"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
    }

    @Test
    void defaultAdminCanLogIn() throws Exception {
        mockMvc.perform(formLogin("/login").user("admin").password(adminPassword))
                .andExpect(authenticated().withUsername("admin").withRoles("ADMIN"))
                .andExpect(redirectedUrl("/dashboard"));
    }

    @Test
    void wrongPasswordIsRejected() throws Exception {
        mockMvc.perform(formLogin("/login").user("admin").password("wrong-password"))
                .andExpect(unauthenticated())
                .andExpect(redirectedUrl("/login?error"));
    }

    @Test
    @WithMockUser(roles = "KEEPER")
    void keeperCannotOpenAdminArea() throws Exception {
        mockMvc.perform(get("/admin/anything")).andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void adminCannotOpenKeeperArea() throws Exception {
        mockMvc.perform(get("/keeper/anything")).andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void adminPassesTheAdminRule() throws Exception {
        mockMvc.perform(get("/admin/anything")).andExpect(status().isNotFound());
    }
}