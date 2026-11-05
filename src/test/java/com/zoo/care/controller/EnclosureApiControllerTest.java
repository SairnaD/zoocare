package com.zoo.care.controller;

import com.zoo.care.config.SecurityConfig;
import com.zoo.care.dto.EnclosureRequest;
import com.zoo.care.dto.EnclosureResponse;
import com.zoo.care.dto.KeeperAssignmentResponse;
import com.zoo.care.entity.HabitatType;
import com.zoo.care.exception.BusinessRuleException;
import com.zoo.care.exception.ResourceNotFoundException;
import com.zoo.care.service.EnclosureService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
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

    @Test
    @WithMockUser(roles = "KEEPER")
    void getAll_keeperIsForbidden() throws Exception {
        mockMvc.perform(get("/api/enclosures"))
                .andExpect(status().isForbidden());
        verifyNoInteractions(enclosureService);
    }

    @Test
    @WithAnonymousUser
    void getAll_anonymousIsRedirectedToLogin() throws Exception {
        mockMvc.perform(get("/api/enclosures"))
                .andExpect(status().is3xxRedirection());
    }

    @Test
    @WithMockUser(roles = "KEEPER")
    void create_keeperIsForbidden() throws Exception {
        mockMvc.perform(post("/api/enclosures")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(SAVANNA_JSON))
                .andExpect(status().isForbidden());
        verifyNoInteractions(enclosureService);
    }

    @Test
    @WithMockUser(roles = "KEEPER")
    void delete_keeperIsForbidden() throws Exception {
        mockMvc.perform(delete("/api/enclosures/1"))
                .andExpect(status().isForbidden());
        verifyNoInteractions(enclosureService);
    }

    @Test
    void create_unknownHabitat_returnsBadRequest() throws Exception {
        mockMvc.perform(post("/api/enclosures")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "Savanna 1", "habitatType": "OCEAN", "capacity": 3}
                                """))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(enclosureService);
    }

    @Test
    void update_unknownId_returnsNotFound() throws Exception {
        when(enclosureService.update(eq(99L), any(EnclosureRequest.class)))
                .thenThrow(new ResourceNotFoundException("Enclosure", 99L));

        mockMvc.perform(put("/api/enclosures/99")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(SAVANNA_JSON))
                .andExpect(status().isNotFound());
    }

    @Test
    void delete_enclosureWithAnimals_returnsConflict() throws Exception {
        doThrow(new BusinessRuleException("Enclosure still has animals and cannot be deleted"))
                .when(enclosureService).delete(1L);

        mockMvc.perform(delete("/api/enclosures/1"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Enclosure still has animals and cannot be deleted"));
    }

    private static final KeeperAssignmentResponse ASSIGNMENT = new KeeperAssignmentResponse(
            10L, 1L, 7L, "Ben Ten",
            Instant.parse("2026-11-04T12:00:00Z"), null, Instant.parse("2026-11-04T12:00:00Z"));

    @Test
    void getKeepers_returnsActiveAssignments() throws Exception {
        when(enclosureService.findActiveKeepers(1L)).thenReturn(List.of(ASSIGNMENT));

        mockMvc.perform(get("/api/enclosures/1/keepers"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].keeperName").value("Ben Ten"));
    }

    @Test
    void assignKeeper_validRequest_returnsCreated() throws Exception {
        when(enclosureService.assignKeeper(1L, 7L)).thenReturn(ASSIGNMENT);

        mockMvc.perform(post("/api/enclosures/1/keepers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"keeperId": 7}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.keeperId").value(7))
                .andExpect(jsonPath("$.endsAt").isEmpty());
    }

    @Test
    void assignKeeper_nonPositiveKeeperId_returnsBadRequest() throws Exception {
        mockMvc.perform(post("/api/enclosures/1/keepers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"keeperId": 0}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.keeperId").exists());
        verifyNoInteractions(enclosureService);
    }

    @Test
    void assignKeeper_alreadyAssigned_returnsConflict() throws Exception {
        when(enclosureService.assignKeeper(1L, 7L))
                .thenThrow(new BusinessRuleException("Keeper is already assigned to this enclosure"));

        mockMvc.perform(post("/api/enclosures/1/keepers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"keeperId": 7}
                                """))
                .andExpect(status().isConflict());
    }

    @Test
    void assignKeeper_unknownUser_returnsNotFound() throws Exception {
        when(enclosureService.assignKeeper(1L, 7L)).thenThrow(new ResourceNotFoundException("User", 7L));

        mockMvc.perform(post("/api/enclosures/1/keepers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"keeperId": 7}
                                """))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(roles = "KEEPER")
    void assignKeeper_keeperIsForbidden() throws Exception {
        mockMvc.perform(post("/api/enclosures/1/keepers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"keeperId": 7}
                                """))
                .andExpect(status().isForbidden());
        verifyNoInteractions(enclosureService);
    }

    @Test
    void removeKeeper_returnsNoContent() throws Exception {
        mockMvc.perform(delete("/api/enclosures/1/keepers/7"))
                .andExpect(status().isNoContent());
        verify(enclosureService).unassignKeeper(1L, 7L);
    }

    @Test
    void removeKeeper_notAssigned_returnsNotFound() throws Exception {
        doThrow(new ResourceNotFoundException("Keeper 7 is not assigned to enclosure 1"))
                .when(enclosureService).unassignKeeper(1L, 7L);

        mockMvc.perform(delete("/api/enclosures/1/keepers/7"))
                .andExpect(status().isNotFound());
    }
}