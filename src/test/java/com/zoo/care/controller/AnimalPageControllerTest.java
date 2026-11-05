    package com.zoo.care.controller;

    import com.zoo.care.config.SecurityConfig;
    import com.zoo.care.config.WebConfig;
    import com.zoo.care.dto.AnimalResponse;
    import com.zoo.care.exception.BusinessRuleException;
    import com.zoo.care.service.AnimalService;
    import com.zoo.care.service.EnclosureService;
    import com.zoo.care.service.FileStorageService;
    import com.zoo.care.service.SpeciesService;
    import org.junit.jupiter.api.Test;
    import org.springframework.beans.factory.annotation.Autowired;
    import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
    import org.springframework.context.annotation.ComponentScan;
    import org.springframework.context.annotation.FilterType;
    import org.springframework.context.annotation.Import;
    import org.springframework.mock.web.MockMultipartFile;
    import org.springframework.security.test.context.support.WithMockUser;
    import org.springframework.test.context.bean.override.mockito.MockitoBean;
    import org.springframework.test.web.servlet.MockMvc;
    import org.springframework.test.web.servlet.RequestBuilder;

    import java.time.LocalDate;
    import java.util.List;

    import static org.junit.jupiter.api.Assertions.assertFalse;
    import static org.junit.jupiter.api.Assertions.assertNotNull;
    import static org.mockito.ArgumentMatchers.any;
    import static org.mockito.ArgumentMatchers.eq;
    import static org.mockito.Mockito.*;
    import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
    import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
    import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
    import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

    @WebMvcTest(controllers = AnimalPageController.class,
            excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = WebConfig.class))
    @Import(SecurityConfig.class)
    class AnimalPageControllerTest {

        private static final MockMultipartFile PHOTO =
                new MockMultipartFile("photo", "leo.png", "image/png", new byte[]{1, 2, 3});

        @Autowired
        MockMvc mockMvc;

        @MockitoBean
        AnimalService animalService;
        @MockitoBean
        SpeciesService speciesService;
        @MockitoBean
        EnclosureService enclosureService;
        @MockitoBean
        FileStorageService fileStorageService;

        private RequestBuilder validPost() {
            return multipart("/admin/animals")
                    .file(PHOTO)
                    .with(csrf())
                    .param("name", "Leo")
                    .param("speciesId", "1")
                    .param("enclosureId", "2")
                    .param("birthDate", "2020-05-01")
                    .param("weight", "180.5")
                    .param("size", "120")
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

            verify(animalService).create(any(),any());
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        void create_invalidPost_returnsFormWithFieldErrors() throws Exception {
            mockMvc.perform(multipart("/admin/animals")
                            .file(PHOTO)
                            .with(csrf())
                            .param("name", "a")) // too short, everything else is missing
                    .andExpect(status().isOk())
                    .andExpect(view().name("admin/animals/form"))
                    .andExpect(model().attributeHasFieldErrors("animalRequest",
                            "name", "speciesId", "enclosureId", "birthDate", "weight", "size",
                            "feedingIntervalHours"))
                    .andExpect(model().attributeExists("speciesList", "enclosures"));

            verifyNoInteractions(animalService);
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        void create_invalidPhoto_returnsFormWithPhotoError() throws Exception {
            when(fileStorageService.validate(any())).thenReturn("Please choose an image");

            mockMvc.perform(validPost())
                    .andExpect(status().isOk())
                    .andExpect(view().name("admin/animals/form"))
                    .andExpect(model().attributeHasFieldErrors("animalRequest", "photo"));

            verifyNoInteractions(animalService);
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        void create_businessRuleViolation_showsGlobalErrorNotNameError() throws Exception {
            when(animalService.create(any(),any()))
                    .thenThrow(new BusinessRuleException("Enclosure 'Savannah' is full"));

            mockMvc.perform(validPost())
                    .andExpect(status().isOk())
                    .andExpect(view().name("admin/animals/form"))
                    .andExpect(model().attributeHasErrors("animalRequest"))
                    .andExpect(result -> {
                        var bindingResult = (org.springframework.validation.BindingResult)
                                result.getModelAndView().getModel().get(
                                        "org.springframework.validation.BindingResult.animalRequest");

                        assertNotNull(bindingResult);
                        assertFalse(bindingResult.hasFieldErrors("name"));
                    })
                    .andExpect(model().attributeExists("speciesList", "enclosures"));
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        void create_withoutCsrf_isForbidden() throws Exception {
            mockMvc.perform(multipart("/admin/animals").file(PHOTO).param("name", "Leo"))
                    .andExpect(status().isForbidden());
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        void profile_showsAnimal() throws Exception {
            when(animalService.findById(1L)).thenReturn(new AnimalResponse(1L, "Leo", 1L, "Lion", 2L,
                    "Savannah", LocalDate.of(2020, 5, 1), 180.5f, 120, 6, "A lion", "abc.png", "HEALTHY"));

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

        @Test
        @WithMockUser(roles = "ADMIN")
        void newForm_clearsPhotoLeftFromPreviousAttempt() throws Exception {
            mockMvc.perform(get("/admin/animals/new").sessionAttr("pendingPhoto", "old.png"))
                    .andExpect(status().isOk())
                    .andExpect(request().sessionAttributeDoesNotExist("pendingPhoto"));
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        void create_validPost_storesPhotoAndClearsSession() throws Exception {
            when(fileStorageService.store(any())).thenReturn("abc.png");

            mockMvc.perform(validPost())
                    .andExpect(status().is3xxRedirection())
                    .andExpect(request().sessionAttributeDoesNotExist("pendingPhoto"));

            verify(animalService).create(any(), eq("abc.png"));
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        void create_withoutPhoto_passesNullToService() throws Exception {
            mockMvc.perform(multipart("/admin/animals")
                            .with(csrf())
                            .param("name", "Leo")
                            .param("speciesId", "1")
                            .param("enclosureId", "2")
                            .param("birthDate", "2020-05-01")
                            .param("weight", "180.5")
                            .param("size", "120")
                            .param("feedingIntervalHours", "6"))
                    .andExpect(status().is3xxRedirection());

            verify(animalService).create(any(), isNull());
            verify(fileStorageService, never()).store(any());
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        void create_withoutNewPhoto_usesPhotoFromPreviousAttempt() throws Exception {
            mockMvc.perform(multipart("/admin/animals")
                            .sessionAttr("pendingPhoto", "abc.png")
                            .with(csrf())
                            .param("name", "Leo")
                            .param("speciesId", "1")
                            .param("enclosureId", "2")
                            .param("birthDate", "2020-05-01")
                            .param("weight", "180.5")
                            .param("size", "120")
                            .param("feedingIntervalHours", "6"))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(request().sessionAttributeDoesNotExist("pendingPhoto"));

            verify(animalService).create(any(), eq("abc.png"));
            verify(fileStorageService, never()).store(any());
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        void create_invalidPost_keepsValidPhotoForNextAttempt() throws Exception {
            when(fileStorageService.store(any())).thenReturn("abc.png");

            mockMvc.perform(multipart("/admin/animals")
                            .file(PHOTO)
                            .with(csrf())
                            .param("name", "a")) // too short, everything else is missing
                    .andExpect(status().isOk())
                    .andExpect(view().name("admin/animals/form"))
                    .andExpect(model().attribute("pendingPhoto", "abc.png"))
                    .andExpect(request().sessionAttribute("pendingPhoto", "abc.png"));

            verifyNoInteractions(animalService);
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        void create_businessRuleViolation_keepsPhotoForNextAttempt() throws Exception {
            when(fileStorageService.store(any())).thenReturn("abc.png");
            when(animalService.create(any(), any()))
                    .thenThrow(new BusinessRuleException("Enclosure 'Savannah' is full"));

            mockMvc.perform(validPost())
                    .andExpect(status().isOk())
                    .andExpect(view().name("admin/animals/form"))
                    .andExpect(model().attribute("pendingPhoto", "abc.png"))
                    .andExpect(request().sessionAttribute("pendingPhoto", "abc.png"));
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        void create_invalidPhoto_isNotStored() throws Exception {
            when(fileStorageService.validate(any())).thenReturn("Only JPG, PNG, WebP or GIF images are allowed");

            mockMvc.perform(validPost())
                    .andExpect(status().isOk())
                    .andExpect(view().name("admin/animals/form"));

            verify(fileStorageService, never()).store(any());
            verifyNoInteractions(animalService);
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        void newForm_deletesPhotoLeftFromPreviousAttempt() throws Exception {
            mockMvc.perform(get("/admin/animals/new").sessionAttr("pendingPhoto", "old.png"))
                    .andExpect(status().isOk());

            verify(fileStorageService).delete("old.png");
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        void create_newPhoto_deletesPreviousPendingPhoto() throws Exception {
            when(fileStorageService.store(any())).thenReturn("new.png");

            mockMvc.perform(multipart("/admin/animals")
                            .file(PHOTO)
                            .sessionAttr("pendingPhoto", "old.png")
                            .with(csrf())
                            .param("name", "a")) // invalid, so the form comes back
                    .andExpect(status().isOk())
                    .andExpect(request().sessionAttribute("pendingPhoto", "new.png"));

            verify(fileStorageService).delete("old.png");
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        void create_validPost_doesNotDeletePhotoOfSavedAnimal() throws Exception {
            when(fileStorageService.store(any())).thenReturn("abc.png");

            mockMvc.perform(validPost())
                    .andExpect(status().is3xxRedirection());

            verify(animalService).create(any(), eq("abc.png"));
            verify(fileStorageService, never()).delete("abc.png");
        }
    }