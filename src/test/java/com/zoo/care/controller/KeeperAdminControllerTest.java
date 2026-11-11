package com.zoo.care.controller;

import com.zoo.care.config.SecurityConfig;
import com.zoo.care.exception.ResourceNotFoundException;
import com.zoo.care.exception.UserIsNotKeeperException;
import com.zoo.care.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(KeeperAdminController.class)
@Import(SecurityConfig.class)
@WithMockUser(roles = "ADMIN")
class KeeperAdminControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserService userService;

    @Test
    void delete_redirectsWithSuccessMessage() throws Exception {
        mockMvc.perform(post("/admin/keepers/7/delete").with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/keepers"))
                .andExpect(flash().attribute("successMessage", "Keeper deleted"));
        verify(userService).deleteKeeper(7L);
    }

    @Test
    void delete_unknownKeeper_redirectsWithError() throws Exception {
        doThrow(new ResourceNotFoundException("User", 99L)).when(userService).deleteKeeper(99L);

        mockMvc.perform(post("/admin/keepers/99/delete").with(csrf()))
                .andExpect(redirectedUrl("/admin/keepers"))
                .andExpect(flash().attribute("errorMessage", "User with id 99 not found"));
    }

    @Test
    void makeAdmin_redirectsWithSuccessMessage() throws Exception {
        mockMvc.perform(post("/admin/keepers/7/make-admin").with(csrf()))
                .andExpect(redirectedUrl("/admin/keepers"))
                .andExpect(flash().attribute("successMessage", "Keeper promoted to admin"));
        verify(userService).makeAdmin(7L);
    }

    @Test
    void makeAdmin_notKeeper_redirectsWithError() throws Exception {
        doThrow(new UserIsNotKeeperException()).when(userService).makeAdmin(1L);

        mockMvc.perform(post("/admin/keepers/1/make-admin").with(csrf()))
                .andExpect(redirectedUrl("/admin/keepers"))
                .andExpect(flash().attributeExists("errorMessage"))
                .andExpect(flash().attributeCount(1));
    }

    @Test
    @WithMockUser(roles = "KEEPER")
    void keeper_isForbidden() throws Exception {
        mockMvc.perform(post("/admin/keepers/7/delete").with(csrf()))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/admin/keepers/7/make-admin").with(csrf()))
                .andExpect(status().isForbidden());
        verifyNoInteractions(userService);
    }

    @Test
    void withoutCsrf_isForbidden() throws Exception {
        mockMvc.perform(post("/admin/keepers/7/delete"))
                .andExpect(status().isForbidden());
        verifyNoInteractions(userService);
    }
}