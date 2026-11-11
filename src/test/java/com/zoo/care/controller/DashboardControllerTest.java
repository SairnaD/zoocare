package com.zoo.care.controller;

import com.zoo.care.config.SecurityConfig;
import com.zoo.care.dto.FeedingRequest;
import com.zoo.care.dto.KeeperAnimalView;
import com.zoo.care.entity.FeedingStatus;
import com.zoo.care.entity.HabitatType;
import com.zoo.care.exception.FeedingTimeInFutureException;
import com.zoo.care.exception.FeedingTooEarlyException;
import com.zoo.care.service.FeedingService;
import com.zoo.care.service.KeeperDashboardService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.*;
import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(DashboardController.class)
@Import({SecurityConfig.class, DashboardControllerTest.FixedClock.class})
class DashboardControllerTest {

    // 10:00 UTC = 12:00 in Riga
    private static final Instant NOW = Instant.parse("2026-11-10T10:00:00Z");

    @TestConfiguration(proxyBeanMethods = false)
    static class FixedClock {
        @Bean
        Clock clock() {
            return Clock.fixed(NOW, ZoneOffset.UTC);
        }
    }

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private KeeperDashboardService keeperDashboardService;

    @MockitoBean
    private FeedingService feedingService;

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
    @WithMockUser(username = "ben", roles = "KEEPER")
    void keeperPage_showsAnimalsWithStatusAndDialogs() throws Exception {
        KeeperAnimalView leo = animalView(10L, "Leo", FeedingStatus.NEED_TO_FEED,
                Instant.parse("2026-11-10T04:00:00Z"), Instant.parse("2026-11-10T10:00:00Z"));
        when(keeperDashboardService.findAnimalsForKeeper("ben")).thenReturn(List.of(leo));

        mockMvc.perform(get("/keeper"))
                .andExpect(status().isOk())
                .andExpect(view().name("keeper/dashboard"))
                .andExpect(model().attribute("animals", List.of(leo)))
                .andExpect(model().attribute("displayZone", ZoneId.of("Europe/Riga")))
                .andExpect(model().attribute("currentTime", LocalTime.of(12, 0)))
                .andExpect(content().string(containsString("Leo")))
                .andExpect(content().string(containsString("chip-savanna")))
                .andExpect(content().string(containsString("Every 6 h")))
                .andExpect(content().string(containsString("Need to feed")))
                .andExpect(content().string(containsString("feed-10")))
                .andExpect(content().string(containsString("details-10")))
                .andExpect(content().string(containsString("Ann Lee")))
                .andExpect(content().string(not(containsString("Too early to feed Leo"))));
    }

    @Test
    @WithMockUser(username = "ben", roles = "KEEPER")
    void keeperPage_fedAnimalShowsTooEarlyWarningInFeedDialog() throws Exception {
        when(keeperDashboardService.findAnimalsForKeeper("ben")).thenReturn(List.of(animalView(10L, "Leo",
                FeedingStatus.FED, Instant.parse("2026-11-10T09:00:00Z"), Instant.parse("2026-11-10T15:00:00Z"))));

        mockMvc.perform(get("/keeper"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("badge-fed")))
                .andExpect(content().string(containsString("Too early to feed Leo.")))
                .andExpect(content().string(containsString("17:00")));
    }

    @Test
    @WithMockUser(username = "ben", roles = "KEEPER")
    void keeperPage_emptyListShowsEmptyState() throws Exception {
        when(keeperDashboardService.findAnimalsForKeeper("ben")).thenReturn(List.of());

        mockMvc.perform(get("/keeper"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("No animals yet")))
                .andExpect(content().string(not(containsString("<table"))));
    }

    @Test
    @WithMockUser(username = "ben", roles = "KEEPER")
    void keeperPage_escapesAnimalName() throws Exception {
        when(keeperDashboardService.findAnimalsForKeeper("ben")).thenReturn(List.of(
                animalView(10L, "<script>alert(1)</script>", FeedingStatus.NEED_TO_FEED, null, null)));

        mockMvc.perform(get("/keeper"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("&lt;script&gt;")))
                .andExpect(content().string(not(containsString("<script>alert(1)</script>"))));
    }

    @Test
    @WithMockUser(username = "ben", roles = "KEEPER")
    void keeperPage_showsSuccessMessageAfterFeeding() throws Exception {
        when(keeperDashboardService.findAnimalsForKeeper("ben")).thenReturn(List.of(animalView(10L, "Leo",
                FeedingStatus.FED, Instant.parse("2026-11-10T10:00:00Z"), Instant.parse("2026-11-10T16:00:00Z"))));

        mockMvc.perform(get("/keeper").flashAttr("fedAnimalId", 10L))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Leo fed")));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void keeperPage_adminIsForbidden() throws Exception {
        mockMvc.perform(get("/keeper"))
                .andExpect(status().isForbidden());
        verifyNoInteractions(keeperDashboardService);
    }

    @Test
    void keeperPage_guestIsRedirectedToLogin() throws Exception {
        mockMvc.perform(get("/keeper"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
        verifyNoInteractions(keeperDashboardService);
    }

    @Test
    @WithMockUser(username = "ben", roles = "KEEPER")
    void feed_savesChosenTimeOfTodayInRigaAndRedirectsWithSuccess() throws Exception {
        mockMvc.perform(post("/keeper/animals/10/feedings").with(csrf())
                        .param("food", "Hay").param("amountKg", "0.750")
                        .param("fedAt", "11:30").param("note", "Ate well"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/keeper"))
                .andExpect(flash().attribute("fedAnimalId", 10L));

        verify(feedingService).recordFeeding(10L, new FeedingRequest("Hay", new BigDecimal("0.750"), "Ate well"),
                "ben", Instant.parse("2026-11-10T09:30:00Z"));
    }

    @Test
    @WithMockUser(username = "ben", roles = "KEEPER")
    void feed_tooEarly_redirectsWithTooEarlyAnimal() throws Exception {
        when(feedingService.recordFeeding(any(), any(), any(), any()))
                .thenThrow(new FeedingTooEarlyException(Instant.parse("2026-11-10T15:00:00Z")));

        mockMvc.perform(post("/keeper/animals/10/feedings").with(csrf())
                        .param("food", "Hay").param("amountKg", "1").param("fedAt", "12:00"))
                .andExpect(redirectedUrl("/keeper"))
                .andExpect(flash().attribute("tooEarlyAnimalId", 10L));
    }

    @Test
    @WithMockUser(username = "ben", roles = "KEEPER")
    void feed_otherBusinessError_redirectsWithItsMessage() throws Exception {
        when(feedingService.recordFeeding(any(), any(), any(), any())).thenThrow(new FeedingTimeInFutureException());

        mockMvc.perform(post("/keeper/animals/10/feedings").with(csrf())
                        .param("food", "Hay").param("amountKg", "1").param("fedAt", "12:00"))
                .andExpect(redirectedUrl("/keeper"))
                .andExpect(flash().attribute("feedError", "Feeding time cannot be in the future"));
    }

    @Test
    @WithMockUser(username = "ben", roles = "KEEPER")
    void feed_invalidForm_redirectsWithErrorWithoutCallingService() throws Exception {
        mockMvc.perform(post("/keeper/animals/10/feedings").with(csrf())
                        .param("food", " ").param("amountKg", "-1").param("fedAt", "12:00"))
                .andExpect(redirectedUrl("/keeper"))
                .andExpect(flash().attribute("feedError", "Please enter the food and a positive amount."));
        verifyNoInteractions(feedingService);
    }

    @Test
    @WithMockUser(username = "ben", roles = "KEEPER")
    void feed_withoutCsrf_isForbidden() throws Exception {
        mockMvc.perform(post("/keeper/animals/10/feedings")
                        .param("food", "Hay").param("amountKg", "1").param("fedAt", "12:00"))
                .andExpect(status().isForbidden());
        verifyNoInteractions(feedingService);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void feed_adminIsForbidden() throws Exception {
        mockMvc.perform(post("/keeper/animals/10/feedings").with(csrf())
                        .param("food", "Hay").param("amountKg", "1").param("fedAt", "12:00"))
                .andExpect(status().isForbidden());
        verifyNoInteractions(feedingService);
    }

    private KeeperAnimalView animalView(Long id, String name, FeedingStatus status, Instant lastFedAt, Instant next) {
        return new KeeperAnimalView(id, name, "Lion", "Savanna 1", HabitatType.SAVANNA,
                LocalDate.of(2020, 5, 1), 6, 180.5f, "Calm lion", 6,
                lastFedAt, lastFedAt == null ? null : "Ann Lee", next, status);
    }
}