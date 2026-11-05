package com.zoo.care.controller;

import com.zoo.care.config.SecurityConfig;
import com.zoo.care.config.WebConfig;
import com.zoo.care.dto.AnimalResponse;
import com.zoo.care.exception.ResourceNotFoundException;
import com.zoo.care.service.AnimalService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.List;

import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = AnimalApiController.class,
        excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = WebConfig.class))
@Import(SecurityConfig.class)
class AnimalApiControllerTest {

    private static final AnimalResponse LEO = new AnimalResponse(1L, "Leo", 1L, "Lion", 2L, "Savannah",
            LocalDate.of(2020, 5, 1), 180.5f, 120, 6, "A lion", "abc.png", "HEALTHY");

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    AnimalService animalService;

    @Test
    @WithMockUser(roles = "ADMIN")
    void getAll_adminSeesJsonList() throws Exception {
        when(animalService.findAll()).thenReturn(List.of(LEO));

        mockMvc.perform(get("/api/animals"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$[0].name").value("Leo"))
                .andExpect(jsonPath("$[0].speciesName").value("Lion"))
                .andExpect(jsonPath("$[0].feedingInterval").value(6));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void getById_returnsAnimal() throws Exception {
        when(animalService.findById(1L)).thenReturn(LEO);

        mockMvc.perform(get("/api/animals/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.enclosureName").value("Savannah"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void getById_unknownId_returnsNotFound() throws Exception {
        when(animalService.findById(99L)).thenThrow(new ResourceNotFoundException("Animal", 99L));

        mockMvc.perform(get("/api/animals/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Animal with id 99 not found"));
    }

    @Test
    @WithMockUser(roles = "KEEPER")
    void getAll_keeperIsForbidden() throws Exception {
        mockMvc.perform(get("/api/animals"))
                .andExpect(status().isForbidden());
        verifyNoInteractions(animalService);
    }

    @Test
    void getAll_anonymousIsRedirectedToLogin() throws Exception {
        mockMvc.perform(get("/api/animals"))
                .andExpect(status().is3xxRedirection());
    }
}