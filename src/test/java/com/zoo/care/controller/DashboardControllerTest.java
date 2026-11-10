package com.zoo.care.controller;

import com.zoo.care.config.SecurityConfig;
import com.zoo.care.dto.AnimalResponse;
import com.zoo.care.dto.EnclosureResponse;
import com.zoo.care.dto.KeeperEnclosureView;
import com.zoo.care.entity.HabitatType;
import com.zoo.care.service.KeeperDashboardService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.List;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(DashboardController.class)
@Import(SecurityConfig.class)
class DashboardControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private KeeperDashboardService keeperDashboardService;

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

    @Test
    @WithMockUser(username = "ben", roles = "KEEPER")
    void keeperPage_showsEnclosuresOfLoggedInKeeper() throws Exception {
        KeeperEnclosureView enclosureView = new KeeperEnclosureView(
                new EnclosureResponse(2L, "Savannah", HabitatType.SAVANNA, 5),
                List.of(new AnimalResponse(10L, "Leo", 1L, "Lion", 2L, "Savannah",
                        LocalDate.of(2020, 5, 1), 180.5f, 6, "A lion")));
        when(keeperDashboardService.findForKeeper("ben")).thenReturn(List.of(enclosureView));

        mockMvc.perform(get("/keeper"))
                .andExpect(status().isOk())
                .andExpect(view().name("keeper/dashboard"))
                .andExpect(model().attribute("enclosures", List.of(enclosureView)));

        verify(keeperDashboardService).findForKeeper("ben");
    }
}