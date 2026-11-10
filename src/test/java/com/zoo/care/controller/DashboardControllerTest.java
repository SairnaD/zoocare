package com.zoo.care.controller;

import com.zoo.care.config.SecurityConfig;
import com.zoo.care.dto.DueFeedingItem;
import com.zoo.care.service.KeeperDashboardService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.time.ZoneId;
import java.util.List;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
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
        verifyNoInteractions(keeperDashboardService);
    }

    @Test
    @WithMockUser(username = "ben", roles = "KEEPER")
    void keeperPage_showsDueAnimalsOfLoggedInKeeperAndFormatsRigaTime() throws Exception {
        DueFeedingItem item = new DueFeedingItem(10L, "Leo", "Lion", "Savannah", 6,
                Instant.parse("2026-11-10T06:00:00Z"), Instant.parse("2026-11-10T12:00:00Z"));
        when(keeperDashboardService.findDueForKeeper("ben")).thenReturn(List.of(item));

        mockMvc.perform(get("/keeper"))
                .andExpect(status().isOk())
                .andExpect(view().name("keeper/dashboard"))
                .andExpect(model().attribute("dueAnimals", List.of(item)))
                .andExpect(model().attribute("displayZone", ZoneId.of("Europe/Riga")))
                .andExpect(content().string(containsString("Leo")))
                .andExpect(content().string(containsString("Lion")))
                .andExpect(content().string(containsString("Savannah")))
                .andExpect(content().string(containsString("10.11.2026 08:00")))
                .andExpect(content().string(containsString("10.11.2026 14:00")))
                .andExpect(content().string(not(containsString("No animals are due"))));

        verify(keeperDashboardService).findDueForKeeper("ben");
    }

    @Test
    @WithMockUser(username = "ben", roles = "KEEPER")
    void keeperPage_emptyListShowsEmptyState() throws Exception {
        when(keeperDashboardService.findDueForKeeper("ben")).thenReturn(List.of());
        mockMvc.perform(get("/keeper"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("No animals are due for feeding right now.")))
                .andExpect(content().string(not(containsString("<table"))));
    }

    @Test
    @WithMockUser(username = "ben", roles = "KEEPER")
    void keeperPage_neverFedAnimalShowsLabelsAndEscapesItsName() throws Exception {
        when(keeperDashboardService.findDueForKeeper("ben")).thenReturn(List.of(
                new DueFeedingItem(10L, "<script>alert(1)</script>", "Lion", "Savannah", 6, null, null)));
        mockMvc.perform(get("/keeper"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Never fed")))
                .andExpect(content().string(containsString("Ready now")))
                .andExpect(content().string(containsString("&lt;script&gt;")))
                .andExpect(content().string(not(containsString("<script>alert(1)</script>"))));
    }

    @Test
    void keeperPage_guestIsRedirectedToLogin() throws Exception {
        mockMvc.perform(get("/keeper"))
                .andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/login"));
        verifyNoInteractions(keeperDashboardService);
    }
}
