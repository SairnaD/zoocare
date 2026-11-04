package com.zoo.care.controller;

import com.zoo.care.config.WebConfig;
import com.zoo.care.exception.BusinessRuleException;
import com.zoo.care.repository.EnclosureRepository;
import com.zoo.care.repository.SpeciesRepository;
import com.zoo.care.service.AnimalService;
import com.zoo.care.service.FileStorageService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.RequestBuilder;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@WebMvcTest(controllers = AnimalPageController.class,
        excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = WebConfig.class))
class AnimalPageControllerTest {

    @Autowired MockMvc mockMvc;

    @MockitoBean
    AnimalService animalService;
    @MockitoBean FileStorageService fileStorageService;
    @MockitoBean SpeciesRepository speciesRepository;
    @MockitoBean EnclosureRepository enclosureRepository;

    private static final MockMultipartFile PHOTO =
            new MockMultipartFile("photo", "leo.png", "image/png", new byte[]{1, 2, 3});

    private RequestBuilder validPost(String name) {
        return multipart("/addAnimal")
                .file(PHOTO)
                .with(csrf())
                .param("name", name)
                .param("speciesId", "1")
                .param("enclosureId", "2")
                .param("birthDate", "2020-05-01")
                .param("weight", "180.5")
                .param("size", "120")
                .param("feedingIntervalHours", "6")
                .param("description", "Lion");
    }

    @Test
    @WithMockUser
    void listAnimals_rendersListView() throws Exception {
        when(animalService.getAllAnimals()).thenReturn(List.of());

        mockMvc.perform(get("/animals"))
                .andExpect(status().isOk())
                .andExpect(view().name("animals/animals"))
                .andExpect(model().attributeExists("animals"));
    }

    @Test
    @WithMockUser
    void showForm_populatesFormAndDropdowns() throws Exception {
        when(speciesRepository.findAll()).thenReturn(List.of());
        when(enclosureRepository.findAll()).thenReturn(List.of());

        mockMvc.perform(get("/addAnimal"))
                .andExpect(status().isOk())
                .andExpect(view().name("animals/addAnimal"))
                .andExpect(model().attributeExists("createAnimalRequest", "speciesList", "enclosures"));
    }

    @Test
    @WithMockUser
    void validPost_redirectsToAnimals() throws Exception {
        mockMvc.perform(validPost("Leo"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/animals?created"));

        verify(animalService).createAnimal(any());
    }

    @Test
    @WithMockUser
    void invalidPost_returnsFormWithErrorsAndDropdowns() throws Exception {
        mockMvc.perform(multipart("/addAnimal")
                        .file(PHOTO)
                        .with(csrf())
                        .param("name", "ab"))        // too short, everything else missing
                .andExpect(status().isOk())
                .andExpect(view().name("animals/addAnimal"))
                .andExpect(model().attributeHasFieldErrors("createAnimalRequest",
                        "name", "speciesId", "enclosureId", "birthDate", "weight", "size", "feedingIntervalHours"))
                .andExpect(model().attributeExists("speciesList", "enclosures"));

        verifyNoInteractions(animalService);
    }

    @Test
    @WithMockUser
    void invalidPhoto_returnsFormWithPhotoError() throws Exception {
        when(fileStorageService.validate(any())).thenReturn("Please choose an image");

        mockMvc.perform(validPost("Leo"))
                .andExpect(status().isOk())
                .andExpect(view().name("animals/addAnimal"))
                .andExpect(model().attributeHasFieldErrors("createAnimalRequest", "photo"));

        verifyNoInteractions(animalService);
    }

    @Test
    @WithMockUser
    void duplicateName_returnsFormWithNameError() throws Exception {
        when(animalService.createAnimal(any()))
                .thenThrow(new BusinessRuleException("Animal with name already exists!"));

        mockMvc.perform(validPost("Leo"))
                .andExpect(status().isOk())
                .andExpect(view().name("animals/addAnimal"))
                .andExpect(model().attributeHasFieldErrors("createAnimalRequest", "name"))
                .andExpect(model().attributeExists("speciesList", "enclosures"));
    }

    @Test
    void anonymous_isRedirectedToLogin() throws Exception {
        mockMvc.perform(get("/animals")
                .accept(MediaType.TEXT_HTML))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
    }

    @Test
    @WithMockUser
    void post_withoutCsrf_isForbidden() throws Exception {
        mockMvc.perform(multipart("/addAnimal").file(PHOTO).param("name", "Leo"))
                .andExpect(status().isForbidden());
    }
}