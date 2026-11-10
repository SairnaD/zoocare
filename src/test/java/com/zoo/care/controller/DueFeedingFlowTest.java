package com.zoo.care.controller;

import com.zoo.care.support.FeedingTestData;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:due-feeding-flow;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@AutoConfigureMockMvc
@Transactional(isolation = Isolation.READ_COMMITTED)
class DueFeedingFlowTest {
    private static final Instant NOW = Instant.parse("2026-11-10T12:00:00Z");
    @Autowired private MockMvc mvc;
    @Autowired private EntityManager em;

    @TestConfiguration(proxyBeanMethods = false)
    static class FixedTime {
        @Bean @Primary Clock dashboardTestClock() { return Clock.fixed(NOW, ZoneOffset.UTC); }
    }

    @Test
    void recordingFeedingRemovesAnimalFromDashboardOnNextRequest() throws Exception {
        var own = FeedingTestData.persist(em, NOW);
        var unrelated = FeedingTestData.persist(em, NOW);
        em.flush();
        em.clear();
        var keeper = user(own.keeper().getUsername()).roles("KEEPER");

        mvc.perform(get("/keeper").with(keeper))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString(own.animal().getName())))
                .andExpect(content().string(not(containsString(unrelated.animal().getName()))))
                .andExpect(content().string(containsString("Never fed")));

        mvc.perform(post("/api/animals/{id}/feedings", own.animal().getId()).with(keeper)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"food\":\"Hay\",\"amountKg\":0.750,\"note\":\"Ate normally\"}"))
                .andExpect(status().isCreated());
        em.flush();
        em.clear();

        mvc.perform(get("/keeper").with(keeper))
                .andExpect(status().isOk())
                .andExpect(content().string(not(containsString(own.animal().getName()))))
                .andExpect(content().string(containsString("No animals are due for feeding right now.")));
    }
}
