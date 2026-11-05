package com.zoo.care.controller;

import com.zoo.care.config.SecurityConfig;
import com.zoo.care.dto.SpeciesResponse;
import com.zoo.care.entity.DietType;
import com.zoo.care.entity.HabitatType;
import com.zoo.care.exception.BusinessRuleException;
import com.zoo.care.service.SpeciesService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
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

@WebMvcTest(SpeciesPageController.class)
@Import(SecurityConfig.class)
class SpeciesPageControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private SpeciesService speciesService;

    private SpeciesResponse lion() {
        return new SpeciesResponse(1L, "Lion", HabitatType.SAVANNA, DietType.CARNIVORE);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void list_showsSpecies() throws Exception {
        when(speciesService.findAll()).thenReturn(List.of(lion()));

        mockMvc.perform(get("/admin/species"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/species/list"))
                .andExpect(model().attributeExists("speciesList"));
    }

    @Test
    @WithMockUser(roles = "KEEPER")
    void list_keeperIsForbidden() throws Exception {
        mockMvc.perform(get("/admin/species"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void newForm_showsEmptyForm() throws Exception {
        mockMvc.perform(get("/admin/species/new"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/species/form"))
                .andExpect(model().attributeExists("speciesRequest", "habitatTypes", "dietTypes"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void create_validDataRedirectsToListWithMessage() throws Exception {
        mockMvc.perform(post("/admin/species")
                        .with(csrf())
                        .param("name", "Lion")
                        .param("habitatType", "SAVANNA")
                        .param("dietType", "CARNIVORE"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/species"))
                .andExpect(flash().attribute("successMessage", "Species created"));

        verify(speciesService).create(any());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void create_invalidDataShowsFormWithErrors() throws Exception {
        mockMvc.perform(post("/admin/species")
                        .with(csrf())
                        .param("name", "")
                        .param("habitatType", "")
                        .param("dietType", "CARNIVORE"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/species/form"))
                .andExpect(model().attributeHasFieldErrors("speciesRequest", "name", "habitatType"));

        verifyNoInteractions(speciesService);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void create_duplicateNameShowsFieldError() throws Exception {
        when(speciesService.create(any()))
                .thenThrow(new BusinessRuleException("Species with name 'Lion' already exists"));

        mockMvc.perform(post("/admin/species")
                        .with(csrf())
                        .param("name", "Lion")
                        .param("habitatType", "SAVANNA")
                        .param("dietType", "CARNIVORE"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/species/form"))
                .andExpect(model().attributeHasFieldErrors("speciesRequest", "name"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void editForm_prefillsExistingSpecies() throws Exception {
        when(speciesService.findById(1L)).thenReturn(lion());

        mockMvc.perform(get("/admin/species/1/edit"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/species/form"))
                .andExpect(model().attribute("speciesId", 1L))
                .andExpect(model().attributeExists("speciesRequest"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void update_validDataRedirectsToListWithMessage() throws Exception {
        mockMvc.perform(post("/admin/species/1")
                        .with(csrf())
                        .param("name", "Lion")
                        .param("habitatType", "SAVANNA")
                        .param("dietType", "CARNIVORE"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/species"))
                .andExpect(flash().attribute("successMessage", "Species updated"));

        verify(speciesService).update(eq(1L), any());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void delete_existingRedirectsWithSuccessMessage() throws Exception {
        mockMvc.perform(post("/admin/species/1/delete").with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/species"))
                .andExpect(flash().attribute("successMessage", "Species deleted"));

        verify(speciesService).delete(1L);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void delete_speciesInUseRedirectsWithErrorMessage() throws Exception {
        doThrow(new BusinessRuleException("Species is in use and cannot be deleted"))
                .when(speciesService).delete(1L);

        mockMvc.perform(post("/admin/species/1/delete").with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/species"))
                .andExpect(flash().attribute("errorMessage", "Species is in use and cannot be deleted"));
    }
}