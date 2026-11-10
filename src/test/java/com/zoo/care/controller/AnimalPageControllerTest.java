package com.zoo.care.controller;

import com.zoo.care.config.SecurityConfig;
import com.zoo.care.dto.AnimalResponse;
import com.zoo.care.exception.*;
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
import static org.mockito.Mockito.*;
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

    private static final AnimalResponse LEO = new AnimalResponse(5L, "Leo", 1L, "Lion", 2L,
            "Savannah", LocalDate.of(2020, 5, 1), 180.5f, 6, "A lion");

    private RequestBuilder validUpdate() {
        return post("/admin/animals/5")
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
    void editForm_prefillsTheAnimal() throws Exception {
        when(animalService.findById(5L)).thenReturn(LEO);

        mockMvc.perform(get("/admin/animals/5/edit"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/animals/form"))
                .andExpect(model().attribute("animalId", 5L))
                .andExpect(model().attributeExists("animalRequest", "speciesList", "enclosures"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void update_validPost_redirectsToProfileWithMessage() throws Exception {
        mockMvc.perform(validUpdate())
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/animals/5"))
                .andExpect(flash().attribute("successMessage", "Animal updated"));

        verify(animalService).update(eq(5L), any());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void update_invalidPost_returnsFormKeepingTheAnimalId() throws Exception {
        mockMvc.perform(post("/admin/animals/5").with(csrf()).param("name", "a"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/animals/form"))
                .andExpect(model().attribute("animalId", 5L));

        verifyNoInteractions(animalService);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void update_duplicateName_showsErrorUnderName() throws Exception {
        when(animalService.update(eq(5L), any())).thenThrow(new AnimalAlreadyExistsException("Leo"));

        mockMvc.perform(validUpdate())
                .andExpect(status().isOk())
                .andExpect(view().name("admin/animals/form"))
                .andExpect(model().attributeHasFieldErrors("animalRequest", "name"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void update_habitatMismatch_showsErrorUnderEnclosure() throws Exception {
        when(animalService.update(eq(5L), any()))
                .thenThrow(new HabitatMismatchException("Lion", "Arctic House"));

        mockMvc.perform(validUpdate())
                .andExpect(status().isOk())
                .andExpect(model().attributeHasFieldErrorCode("animalRequest", "enclosureId", "placement"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void update_deletedAnimal_redirectsToListWithError() throws Exception {
        when(animalService.update(eq(5L), any())).thenThrow(new AnimalDeletedException());

        mockMvc.perform(validUpdate())
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/animals"))
                .andExpect(flash().attributeExists("errorMessage"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void transfer_valid_redirectsToProfileWithMessage() throws Exception {
        when(animalService.transferAnimal(5L, 3L)).thenReturn(new AnimalResponse(5L, "Leo", 1L, "Lion",
                3L, "Savannah 2", LocalDate.of(2020, 5, 1), 180.5f, 6, "A lion"));

        mockMvc.perform(post("/admin/animals/5/transfer").with(csrf()).param("enclosureId", "3"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/animals/5"))
                .andExpect(flash().attribute("successMessage", "Animal moved to Savannah 2"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void transfer_withoutEnclosure_showsErrorAndDoesNotCallService() throws Exception {
        mockMvc.perform(post("/admin/animals/5/transfer").with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/animals/5"))
                .andExpect(flash().attributeExists("errorMessage"));

        verifyNoInteractions(animalService);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void transfer_fullEnclosure_showsErrorMessage() throws Exception {
        when(animalService.transferAnimal(5L, 3L)).thenThrow(new EnclosureFullException("Savannah 2"));

        mockMvc.perform(post("/admin/animals/5/transfer").with(csrf()).param("enclosureId", "3"))
                .andExpect(redirectedUrl("/admin/animals/5"))
                .andExpect(flash().attribute("errorMessage", "Enclosure 'Savannah 2' is full"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void transfer_sameEnclosure_showsErrorMessage() throws Exception {
        when(animalService.transferAnimal(5L, 2L)).thenThrow(new SameEnclosureException());

        mockMvc.perform(post("/admin/animals/5/transfer").with(csrf()).param("enclosureId", "2"))
                .andExpect(flash().attribute("errorMessage", "Animal is already in this enclosure"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void delete_valid_redirectsToListWithMessage() throws Exception {
        mockMvc.perform(post("/admin/animals/5/delete").with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/animals"))
                .andExpect(flash().attribute("successMessage", "Animal deleted"));

        verify(animalService).delete(5L);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void delete_animalInUse_redirectsToProfileWithError() throws Exception {
        doThrow(new AnimalInUseException()).when(animalService).delete(5L);

        mockMvc.perform(post("/admin/animals/5/delete").with(csrf()))
                .andExpect(redirectedUrl("/admin/animals/5"))
                .andExpect(flash().attributeExists("errorMessage"));
    }

    @Test
    @WithMockUser(roles = "KEEPER")
    void update_transfer_delete_keeperIsForbidden() throws Exception {
        mockMvc.perform(validUpdate()).andExpect(status().isForbidden());
        mockMvc.perform(post("/admin/animals/5/transfer").with(csrf()).param("enclosureId", "3"))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/admin/animals/5/delete").with(csrf())).andExpect(status().isForbidden());

        verifyNoInteractions(animalService);
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