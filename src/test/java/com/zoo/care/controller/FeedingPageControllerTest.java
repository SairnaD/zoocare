package com.zoo.care.controller;

import com.zoo.care.config.SecurityConfig;
import com.zoo.care.dto.AnimalFeedingGroup;
import com.zoo.care.dto.FeedingHistoryResponse;
import com.zoo.care.service.FeedingService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(FeedingPageController.class)
@Import(SecurityConfig.class)
class FeedingPageControllerTest {

    private static final FeedingHistoryResponse LATEST = new FeedingHistoryResponse(10L, "Zuri", "Giraffe",
            "Savanna 2", Instant.parse("2026-11-10T09:00:00Z"), "Hay", new BigDecimal("12.000"),
            "Ate all the hay", "Anna Berzina");
    private static final FeedingHistoryResponse EARLIER = new FeedingHistoryResponse(9L, "Zuri", "Giraffe",
            "Savanna 2", Instant.parse("2026-11-09T09:00:00Z"), "Hay", new BigDecimal("11.500"),
            null, "Anna Berzina");
    private static final List<AnimalFeedingGroup> GROUPS =
            List.of(new AnimalFeedingGroup(LATEST, List.of(EARLIER)));

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private FeedingService feedingService;

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void adminPage_showsGroupedFeedingsInRigaTime() throws Exception {
        when(feedingService.getFeedingOverview("admin")).thenReturn(GROUPS);

        mockMvc.perform(get("/admin/feedings"))
                .andExpect(status().isOk())
                .andExpect(view().name("feedings/history"))
                .andExpect(model().attribute("groups", GROUPS))
                .andExpect(model().attribute("displayZone", ZoneId.of("Europe/Riga")))
                .andExpect(content().string(containsString("Zuri")))
                .andExpect(content().string(containsString("Savanna 2")))
                .andExpect(content().string(containsString("10.11.2026 11:00")))
                .andExpect(content().string(containsString("09.11.2026 11:00")))
                .andExpect(content().string(containsString("12 kg")))
                .andExpect(content().string(containsString("11.5 kg")))
                .andExpect(content().string(containsString("Ate all the hay")))
                .andExpect(content().string(containsString("feedings-10")))
                .andExpect(content().string(not(containsString("No feedings in the last 7 days."))));
    }

    @Test
    @WithMockUser(username = "ben", roles = "KEEPER")
    void keeperPage_showsOverviewOfLoggedInKeeper() throws Exception {
        when(feedingService.getFeedingOverview("ben")).thenReturn(GROUPS);

        mockMvc.perform(get("/keeper/feedings"))
                .andExpect(status().isOk())
                .andExpect(view().name("feedings/history"))
                .andExpect(content().string(containsString("Zuri")));

        verify(feedingService).getFeedingOverview("ben");
    }

    @Test
    @WithMockUser(username = "ben", roles = "KEEPER")
    void emptyOverview_showsEmptyState() throws Exception {
        when(feedingService.getFeedingOverview("ben")).thenReturn(List.of());

        mockMvc.perform(get("/keeper/feedings"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("No feedings in the last 7 days.")))
                .andExpect(content().string(not(containsString("<table"))));
    }

    @Test
    @WithMockUser(roles = "KEEPER")
    void adminPage_keeperIsForbidden() throws Exception {
        mockMvc.perform(get("/admin/feedings"))
                .andExpect(status().isForbidden());
        verifyNoInteractions(feedingService);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void keeperPage_adminIsForbidden() throws Exception {
        mockMvc.perform(get("/keeper/feedings"))
                .andExpect(status().isForbidden());
        verifyNoInteractions(feedingService);
    }

    @Test
    void adminPage_guestIsRedirectedToLogin() throws Exception {
        mockMvc.perform(get("/admin/feedings"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
        verifyNoInteractions(feedingService);
    }
}