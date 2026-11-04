package com.zoo.care.controller;
import com.zoo.care.config.WebConfig;
import com.zoo.care.dto.AnimalResponse;
import com.zoo.care.service.AnimalService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Duration;
import java.time.LocalDate;
import java.util.List;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = AnimalApiController.class,
        excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = WebConfig.class))
class AnimalApiControllerTest {

    @Autowired MockMvc mockMvc;
    @MockitoBean AnimalService animalService;

    @Test
    @WithMockUser
    void getAll_returnsJson() throws Exception {
        when(animalService.getAllAnimals()).thenReturn(List.of(
                new AnimalResponse(1L, "Leo", 1L, "Lion", 2L, "Savannah",
                        LocalDate.of(2020, 5, 1), 180.5f, 120, Duration.ofHours(6),
                        "A lion", "abc.png", "HEALTHY")));

        mockMvc.perform(get("/api/animals"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$[0].name").value("Leo"))
                .andExpect(jsonPath("$[0].speciesName").value("Lion"));
    }

    @Test
    void anonymous_isNotAllowed() throws Exception {
        mockMvc.perform(get("/api/animals"))
                .andExpect(status().isUnauthorized());
    }
}