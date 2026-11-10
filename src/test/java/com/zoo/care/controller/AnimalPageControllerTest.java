package com.zoo.care.controller;

import com.zoo.care.config.SecurityConfig;
import com.zoo.care.dto.AnimalResponse;
import com.zoo.care.exception.AnimalAlreadyExistsException;
import com.zoo.care.exception.EnclosureFullException;
import com.zoo.care.exception.HabitatMismatchException;
import com.zoo.care.service.AnimalService;
import com.zoo.care.service.EnclosureService;
import com.zoo.care.service.SpeciesService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.RequestBuilder;

import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@WebMvcTest(AnimalPageController.class)
@Import(SecurityConfig.class)
class AnimalPageControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    AnimalService animalService;
    @MockitoBean
    SpeciesService speciesService;
    @MockitoBean
    EnclosureService enclosureService;

    private RequestBuilder validPost() {
        return post("/admin/animals")
                .with(csrf())
                .param("name", "Leo")
                .param("speciesId", "1")
                .param("enclosureId", "2")
                .param("birthDate", "2020-05-01")
                .param("weight", "180.5")
                .param("feedingIntervalHours", "6")
                .param("description", "A lion");
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void list_showsAnimals() throws Exception {
        when(animalService.findAll()).thenReturn(List.of());

        mockMvc.perform(get("/admin/animals"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/animals/list"))
                .andExpect(model().attributeExists("animals"));
    }

    @Test
    @WithMockUser(roles = "KEEPER")
    void list_keeperIsForbidden() throws Exception {
        mockMvc.perform(get("/admin/animals"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void newForm_populatesFormAndDropdowns() throws Exception {
        when(speciesService.findAll()).thenReturn(List.of());
        when(enclosureService.findAll()).thenReturn(List.of());

        mockMvc.perform(get("/admin/animals/new"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/animals/form"))
                .andExpect(model().attributeExists("animalRequest", "speciesList", "enclosures"));
    }

    @Test
    @WithMockUser(roles = "KEEPER")
    void create_keeperIsForbidden() throws Exception {
        mockMvc.perform(validPost())
                .andExpect(status().isForbidden());
        verifyNoInteractions(animalService);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void create_validPost_redirectsToListWithMessage() throws Exception {
        mockMvc.perform(validPost())
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/animals"))
                .andExpect(flash().attribute("successMessage", "Animal registered"));

        verify(animalService).create(any());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void create_invalidPost_returnsFormWithFieldErrors() throws Exception {
        mockMvc.perform(post("/admin/animals")
                        .with(csrf())
                        .param("name", "a"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/animals/form"))
                .andExpect(model().attributeHasFieldErrors("animalRequest",
                        "name", "speciesId", "enclosureId", "birthDate", "weight", "feedingIntervalHours"))
                .andExpect(model().attributeExists("speciesList", "enclosures"));

        verifyNoInteractions(animalService);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void create_duplicateName_showsErrorUnderName() throws Exception {
        when(animalService.create(any())).thenThrow(new AnimalAlreadyExistsException("Leo"));

        mockMvc.perform(validPost())
                .andExpect(status().isOk())
                .andExpect(view().name("admin/animals/form"))
                .andExpect(model().attributeHasFieldErrors("animalRequest", "name"))
                .andExpect(model().attributeExists("speciesList", "enclosures"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void create_fullEnclosure_showsErrorUnderEnclosure() throws Exception {
        when(animalService.create(any())).thenThrow(new EnclosureFullException("Savannah"));

        mockMvc.perform(validPost())
                .andExpect(status().isOk())
                .andExpect(view().name("admin/animals/form"))
                .andExpect(model().attributeHasFieldErrorCode("animalRequest", "enclosureId", "placement"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void create_habitatMismatch_showsErrorUnderEnclosure() throws Exception {
        when(animalService.create(any())).thenThrow(new HabitatMismatchException("Lion", "Penguin Pool"));

        mockMvc.perform(validPost())
                .andExpect(status().isOk())
                .andExpect(view().name("admin/animals/form"))
                .andExpect(model().attributeHasFieldErrorCode("animalRequest", "enclosureId", "placement"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void create_withoutCsrf_isForbidden() throws Exception {
        mockMvc.perform(post("/admin/animals").param("name", "Leo"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void profile_showsAnimal() throws Exception {
        when(animalService.findById(1L)).thenReturn(new AnimalResponse(1L, "Leo", 1L, "Lion", 2L,
                "Savannah", LocalDate.of(2020, 5, 1), 180.5f, 6, "A lion"));

        mockMvc.perform(get("/admin/animals/1"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/animals/profile"))
                .andExpect(model().attributeExists("animal"));
    }

    @Test
    void anonymous_isRedirectedToLogin() throws Exception {
        mockMvc.perform(get("/admin/animals"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
    }
}