package com.zoo.care.controller;

import com.zoo.care.config.SecurityConfig;
import com.zoo.care.dto.EnclosureRequest;
import com.zoo.care.dto.EnclosureResponse;
import com.zoo.care.entity.HabitatType;
import com.zoo.care.exception.BusinessRuleException;
import com.zoo.care.exception.ResourceNotFoundException;
import com.zoo.care.service.EnclosureService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(EnclosureApiController.class)
@Import(SecurityConfig.class)
@WithMockUser(roles = "ADMIN")
class EnclosureApiControllerTest {

    private static final String SAVANNA_JSON = """
            {"name": "Savanna 1", "habitatType": "SAVANNA", "capacity": 3}
            """;

    private static final EnclosureResponse SAVANNA =
            new EnclosureResponse(1L, "Savanna 1", HabitatType.SAVANNA, 3);

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private EnclosureService enclosureService;

    @Test
    void getAll_returnsEnclosureList() throws Exception {
        when(enclosureService.findAll()).thenReturn(List.of(SAVANNA));

        mockMvc.perform(get("/api/enclosures"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Savanna 1"))
                .andExpect(jsonPath("$[0].capacity").value(3));
    }

    @Test
    void getById_returnsEnclosure() throws Exception {
        when(enclosureService.findById(1L)).thenReturn(SAVANNA);

        mockMvc.perform(get("/api/enclosures/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.habitatType").value("SAVANNA"));
    }

    @Test
    void getById_unknownId_returnsNotFound() throws Exception {
        when(enclosureService.findById(99L)).thenThrow(new ResourceNotFoundException("Enclosure", 99L));

        mockMvc.perform(get("/api/enclosures/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Enclosure with id 99 not found"));
    }

    @Test
    void create_validRequest_returnsCreated() throws Exception {
        when(enclosureService.create(any(EnclosureRequest.class))).thenReturn(SAVANNA);

        mockMvc.perform(post("/api/enclosures")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(SAVANNA_JSON))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    void create_zeroCapacity_returnsBadRequestWithoutCallingService() throws Exception {
        mockMvc.perform(post("/api/enclosures")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "Savanna 1", "habitatType": "SAVANNA", "capacity": 0}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.capacity").exists());
        verifyNoInteractions(enclosureService);
    }

    @Test
    void create_blankName_returnsBadRequestWithoutCallingService() throws Exception {
        mockMvc.perform(post("/api/enclosures")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "", "habitatType": "SAVANNA", "capacity": 3}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.name").exists());
        verifyNoInteractions(enclosureService);
    }

    @Test
    void create_duplicateName_returnsConflict() throws Exception {
        when(enclosureService.create(any(EnclosureRequest.class)))
                .thenThrow(new BusinessRuleException("Enclosure with name 'Savanna 1' already exists"));

        mockMvc.perform(post("/api/enclosures")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(SAVANNA_JSON))
                .andExpect(status().isConflict());
    }

    @Test
    void update_validRequest_returnsUpdatedEnclosure() throws Exception {
        when(enclosureService.update(eq(1L), any(EnclosureRequest.class))).thenReturn(SAVANNA);

        mockMvc.perform(put("/api/enclosures/1")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(SAVANNA_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Savanna 1"));
    }

    @Test
    void delete_existingEnclosure_returnsNoContent() throws Exception {
        mockMvc.perform(delete("/api/enclosures/1").with(csrf()))
                .andExpect(status().isNoContent());
        verify(enclosureService).delete(1L);
    }

    @Test
    void delete_unknownId_returnsNotFound() throws Exception {
        doThrow(new ResourceNotFoundException("Enclosure", 99L)).when(enclosureService).delete(99L);

        mockMvc.perform(delete("/api/enclosures/99").with(csrf()))
                .andExpect(status().isNotFound());
    }
}