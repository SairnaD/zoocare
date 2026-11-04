package com.zoo.care.controller;

import com.zoo.care.config.SecurityConfig;
import com.zoo.care.dto.EnclosureRequest;
import com.zoo.care.dto.EnclosureResponse;
import com.zoo.care.entity.HabitatType;
import com.zoo.care.exception.BusinessRuleException;
import com.zoo.care.service.EnclosureService;
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

@WebMvcTest(EnclosurePageController.class)
@Import(SecurityConfig.class)
@WithMockUser(roles = "ADMIN")
class EnclosurePageControllerTest {

    private static final EnclosureResponse SAVANNA =
            new EnclosureResponse(1L, "Savanna 1", HabitatType.SAVANNA, 3);

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private EnclosureService enclosureService;

    @Test
    void list_returnsListViewWithEnclosures() throws Exception {
        when(enclosureService.findAll()).thenReturn(List.of(SAVANNA));

        mockMvc.perform(get("/admin/enclosures"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/enclosure/list"))
                .andExpect(model().attribute("enclosures", List.of(SAVANNA)));
    }

    @Test
    void newForm_returnsEmptyForm() throws Exception {
        mockMvc.perform(get("/admin/enclosures/new"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/enclosure/form"))
                .andExpect(model().attribute("enclosureRequest", new EnclosureRequest(null, null, null)));
    }

    @Test
    void create_validForm_redirectsToListWithMessage() throws Exception {
        mockMvc.perform(post("/admin/enclosures")
                        .with(csrf())
                        .param("name", "Savanna 1")
                        .param("habitatType", "SAVANNA")
                        .param("capacity", "3"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/enclosures"))
                .andExpect(flash().attribute("successMessage", "Enclosure created"));
        verify(enclosureService).create(any(EnclosureRequest.class));
    }

    @Test
    void create_invalidForm_showsFormWithErrorsWithoutCallingService() throws Exception {
        mockMvc.perform(post("/admin/enclosures")
                        .with(csrf())
                        .param("name", "")
                        .param("habitatType", "SAVANNA")
                        .param("capacity", "0"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/enclosure/form"))
                .andExpect(model().attributeHasFieldErrors("enclosureRequest", "name", "capacity"));
        verifyNoInteractions(enclosureService);
    }

    @Test
    void create_duplicateName_showsErrorUnderName() throws Exception {
        when(enclosureService.create(any(EnclosureRequest.class)))
                .thenThrow(new BusinessRuleException("Enclosure with name 'Savanna 1' already exists"));

        mockMvc.perform(post("/admin/enclosures")
                        .with(csrf())
                        .param("name", "Savanna 1")
                        .param("habitatType", "SAVANNA")
                        .param("capacity", "3"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/enclosure/form"))
                .andExpect(model().attributeHasFieldErrors("enclosureRequest", "name"));
    }

    @Test
    void editForm_prefillsFormWithEnclosureData() throws Exception {
        when(enclosureService.findById(1L)).thenReturn(SAVANNA);

        mockMvc.perform(get("/admin/enclosures/1/edit"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/enclosure/form"))
                .andExpect(model().attribute("enclosureRequest",
                        new EnclosureRequest("Savanna 1", HabitatType.SAVANNA, 3)))
                .andExpect(model().attribute("enclosureId", 1L));
    }

    @Test
    void update_validForm_redirectsToListWithMessage() throws Exception {
        mockMvc.perform(post("/admin/enclosures/1")
                        .with(csrf())
                        .param("name", "Savanna 1")
                        .param("habitatType", "SAVANNA")
                        .param("capacity", "5"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/enclosures"))
                .andExpect(flash().attribute("successMessage", "Enclosure updated"));
        verify(enclosureService).update(eq(1L), any(EnclosureRequest.class));
    }

    @Test
    void delete_redirectsToListWithMessage() throws Exception {
        mockMvc.perform(post("/admin/enclosures/1/delete").with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/enclosures"))
                .andExpect(flash().attribute("successMessage", "Enclosure deleted"));
        verify(enclosureService).delete(1L);
    }
}