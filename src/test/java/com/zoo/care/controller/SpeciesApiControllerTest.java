package com.zoo.care.controller;

import com.zoo.care.config.SecurityConfig;
import com.zoo.care.dto.SpeciesRequest;
import com.zoo.care.dto.SpeciesResponse;
import com.zoo.care.entity.DietType;
import com.zoo.care.entity.HabitatType;
import com.zoo.care.exception.BusinessRuleException;
import com.zoo.care.exception.ResourceNotFoundException;
import com.zoo.care.service.SpeciesService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(SpeciesApiController.class)
@Import(SecurityConfig.class)
class SpeciesApiControllerTest {

    private static final String LION_JSON = """
            {"name": "Lion", "habitatType": "SAVANNA", "dietType": "CARNIVORE"}
            """;

    private static final SpeciesResponse LION =
            new SpeciesResponse(1L, "Lion", HabitatType.SAVANNA, DietType.CARNIVORE);

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private SpeciesService speciesService;

    @Test
    void getAll_returnsSpeciesList() throws Exception {
        when(speciesService.findAll()).thenReturn(List.of(LION));

        mockMvc.perform(get("/api/species"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Lion"))
                .andExpect(jsonPath("$[0].habitatType").value("SAVANNA"));
    }

    @Test
    void getById_returnsSpecies() throws Exception {
        when(speciesService.findById(1L)).thenReturn(LION);

        mockMvc.perform(get("/api/species/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.dietType").value("CARNIVORE"));
    }

    @Test
    void getById_unknownId_returnsNotFound() throws Exception {
        when(speciesService.findById(99L)).thenThrow(new ResourceNotFoundException("Species", 99L));

        mockMvc.perform(get("/api/species/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Species with id 99 not found"));
    }

    @Test
    void create_validRequest_returnsCreated() throws Exception {
        when(speciesService.create(any(SpeciesRequest.class))).thenReturn(LION);

        mockMvc.perform(post("/api/species")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(LION_JSON))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    void create_blankName_returnsBadRequestWithoutCallingService() throws Exception {
        mockMvc.perform(post("/api/species")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "", "habitatType": "SAVANNA", "dietType": "CARNIVORE"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.name").exists());
        verifyNoInteractions(speciesService);
    }

    @Test
    void create_unknownHabitat_returnsBadRequest() throws Exception {
        mockMvc.perform(post("/api/species")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "Lion", "habitatType": "OCEAN", "dietType": "CARNIVORE"}
                                """))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(speciesService);
    }

    @Test
    void create_duplicateName_returnsConflict() throws Exception {
        when(speciesService.create(any(SpeciesRequest.class)))
                .thenThrow(new BusinessRuleException("Species with name 'Lion' already exists"));

        mockMvc.perform(post("/api/species")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(LION_JSON))
                .andExpect(status().isConflict());
    }

    @Test
    void update_validRequest_returnsUpdatedSpecies() throws Exception {
        when(speciesService.update(eq(1L), any(SpeciesRequest.class))).thenReturn(LION);

        mockMvc.perform(put("/api/species/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(LION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Lion"));
    }

    @Test
    void delete_existingSpecies_returnsNoContent() throws Exception {
        mockMvc.perform(delete("/api/species/1"))
                .andExpect(status().isNoContent());
        verify(speciesService).delete(1L);
    }

    @Test
    void delete_unknownId_returnsNotFound() throws Exception {
        doThrow(new ResourceNotFoundException("Species", 99L)).when(speciesService).delete(99L);

        mockMvc.perform(delete("/api/species/99"))
                .andExpect(status().isNotFound());
    }
}