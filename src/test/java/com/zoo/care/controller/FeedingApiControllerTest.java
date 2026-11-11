package com.zoo.care.controller;

import com.zoo.care.config.SecurityConfig;
import com.zoo.care.dto.FeedingRequest;
import com.zoo.care.dto.FeedingResponse;
import com.zoo.care.exception.FeedingTooEarlyException;
import com.zoo.care.exception.AnimalDeletedException;
import com.zoo.care.exception.ResourceNotFoundException;
import com.zoo.care.service.FeedingService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.stream.Stream;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(FeedingApiController.class)
@Import(SecurityConfig.class)
class FeedingApiControllerTest {
    private static final String URL = "/api/animals/5/feedings";
    private static final String VALID_JSON = """
            {"food":"Hay","amountKg":0.750,"note":"Ate normally"}
            """;
    private static final Instant NOW = Instant.parse("2026-11-10T12:00:00Z");
    @Autowired private MockMvc mvc;
    @MockitoBean private FeedingService service;

    @Test
    @WithMockUser(username = "ben", roles = "KEEPER")
    void validRequest_returns201AndPassesAuthenticatedUsername() throws Exception {
        when(service.recordFeeding(eq(5L), any(), eq("ben"), isNull()))
                .thenReturn(new FeedingResponse(20L, 5L, 7L, "Hay",
                        new BigDecimal("0.750"), NOW, "Ate normally"));
        // /api/** currently ignores CSRF; use the real SecurityConfig without adding a token.
        mvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content(VALID_JSON))
                .andExpect(status().isCreated())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(20))
                .andExpect(jsonPath("$.animalId").value(5))
                .andExpect(jsonPath("$.recordedById").value(7))
                .andExpect(jsonPath("$.food").value("Hay"))
                .andExpect(jsonPath("$.amountKg").value(0.750))
                .andExpect(jsonPath("$.fedAt").value(NOW.toString()))
                .andExpect(jsonPath("$.note").value("Ate normally"));
        verify(service).recordFeeding(5L,
                new FeedingRequest("Hay", new BigDecimal("0.750"), "Ate normally"), "ben", null);
    }

    @ParameterizedTest(name = "invalid {0}: {1}")
    @MethodSource("invalidFields")
    @WithMockUser(username = "ben", roles = "KEEPER")
    void invalidField_returns400WithoutCallingService(String field, String value) throws Exception {
        String food = field.equals("food") ? value : "\"Hay\"";
        String amount = field.equals("amountKg") ? value : "0.750";
        String note = field.equals("note") ? value : "\"Ate normally\"";
        String body = "{\"food\":" + food + ",\"amountKg\":" + amount + ",\"note\":" + note + "}";
        mvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.path").value(URL))
                .andExpect(jsonPath("$.fieldErrors." + field).isNotEmpty())
                .andExpect(jsonPath("$.fieldErrors.length()").value(1));
        verifyNoInteractions(service);
    }

    static Stream<Arguments> invalidFields() {
        return Stream.of(
                Arguments.of("food", "null"), Arguments.of("food", "\"\""),
                Arguments.of("food", "\"   \""),
                Arguments.of("food", "\"" + "x".repeat(101) + "\""),
                Arguments.of("amountKg", "null"), Arguments.of("amountKg", "0"),
                Arguments.of("amountKg", "-0.001"), Arguments.of("amountKg", "0.0001"),
                Arguments.of("amountKg", "10000000"),
                Arguments.of("note", "\"" + "x".repeat(501) + "\""));
    }

    @ParameterizedTest
    @MethodSource("missingFields")
    @WithMockUser(roles = "KEEPER")
    void missingRequiredField_returns400(String field, String body) throws Exception {
        mvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors." + field).isNotEmpty());
        verifyNoInteractions(service);
    }

    static Stream<Arguments> missingFields() {
        return Stream.of(Arguments.of("food", "{\"amountKg\":0.750}"),
                Arguments.of("amountKg", "{\"food\":\"Hay\"}"));
    }

    @Test
    @WithMockUser(username = "ben", roles = "KEEPER")
    void maximumLengthsAndAmount_areAccepted() throws Exception {
        String food = "x".repeat(100);
        String note = "x".repeat(500);
        when(service.recordFeeding(eq(5L), any(), eq("ben"), isNull()))
                .thenReturn(new FeedingResponse(20L, 5L, 7L, food,
                        new BigDecimal("9999999.999"), NOW, note));
        mvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"food\":\"" + food + "\",\"amountKg\":9999999.999,\"note\":\"" + note + "\"}"))
                .andExpect(status().isCreated());
        verify(service).recordFeeding(5L,
                new FeedingRequest(food, new BigDecimal("9999999.999"), note), "ben", null);
    }

    @Test
    @WithMockUser(username = "ben", roles = "KEEPER")
    void omittedNoteAndSmallestAmount_areAccepted() throws Exception {
        when(service.recordFeeding(eq(5L), any(), eq("ben"), isNull()))
                .thenReturn(new FeedingResponse(20L, 5L, 7L, "Hay", new BigDecimal("0.001"), NOW, null));
        mvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"food\":\"Hay\",\"amountKg\":0.001}"))
                .andExpect(status().isCreated());
        verify(service).recordFeeding(5L, new FeedingRequest("Hay", new BigDecimal("0.001"), null), "ben", null);
    }

    @ParameterizedTest
    @MethodSource("unreadableBodies")
    @WithMockUser(roles = "KEEPER")
    void unreadableBody_returns400(String body) throws Exception {
        mvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Malformed request body"));
        verifyNoInteractions(service);
    }

    static Stream<String> unreadableBodies() {
        return Stream.of("{", "", "{\"food\":\"Hay\",\"amountKg\":\"abc\"}");
    }

    @Test
    @WithMockUser(username = "ben", roles = "KEEPER")
    void missingAnimal_returns404() throws Exception {
        when(service.recordFeeding(eq(5L), any(), eq("ben"), isNull()))
                .thenThrow(new ResourceNotFoundException("Animal", 5L));
        mvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content(VALID_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Animal with id 5 not found"));
    }

    @Test
    @WithMockUser(username = "ben", roles = "KEEPER")
    void intervalViolation_returns409WithNextAllowedTime() throws Exception {
        String message = "Animal can be fed again at " + NOW.plusSeconds(3600);
        when(service.recordFeeding(eq(5L), any(), eq("ben"), isNull()))
                .thenThrow(new FeedingTooEarlyException(NOW.plusSeconds(3600)));
        mvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content(VALID_JSON))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.message").value(message));
    }

    @Test
    @WithMockUser(username = "ben", roles = "KEEPER")
    void deletedAnimal_returns409() throws Exception {
        when(service.recordFeeding(eq(5L), any(), eq("ben"), isNull()))
                .thenThrow(new AnimalDeletedException());
        mvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content(VALID_JSON))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Cannot record a feeding for a deleted animal"));
    }

    @Test
    @WithMockUser(username = "ben", roles = "KEEPER")
    void unassignedKeeper_returns403() throws Exception {
        when(service.recordFeeding(eq(5L), any(), eq("ben"), isNull()))
                .thenThrow(new AccessDeniedException("Not assigned"));
        mvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content(VALID_JSON))
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.message").value("Access denied"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void admin_isForbiddenWithoutCallingService() throws Exception {
        mvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content(VALID_JSON))
                .andExpect(status().isForbidden());
        verifyNoInteractions(service);
    }

    @Test
    void anonymous_isRedirectedToLoginWithoutCallingService() throws Exception {
        mvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content(VALID_JSON))
                .andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/login"));
        verifyNoInteractions(service);
    }
}
