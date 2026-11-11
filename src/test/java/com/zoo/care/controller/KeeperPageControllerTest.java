package com.zoo.care.controller;

import com.zoo.care.config.SecurityConfig;
import com.zoo.care.dto.KeeperOverviewResponse;
import com.zoo.care.service.KeeperService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@WebMvcTest(KeeperPageController.class)
@Import(SecurityConfig.class)
class KeeperPageControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private KeeperService keeperService;

    @Test
    @WithMockUser(roles = "ADMIN")
    void list_returnsListViewWithKeepers() throws Exception {
        List<KeeperOverviewResponse> keepers = List.of(new KeeperOverviewResponse(
                7L, "Anna Berzina", "anna", "anna.berzina@zoo.lv", List.of(), 0));
        when(keeperService.findAllWithOverview()).thenReturn(keepers);

        mockMvc.perform(get("/admin/keepers"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/keepers/list"))
                .andExpect(model().attribute("keepers", keepers));
    }

    @Test
    @WithMockUser(roles = "KEEPER")
    void list_keeperIsForbidden() throws Exception {
        mockMvc.perform(get("/admin/keepers"))
                .andExpect(status().isForbidden());
        verifyNoInteractions(keeperService);
    }
}