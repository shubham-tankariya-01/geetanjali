package com.geetanjali.backend.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

// SettingsSecurityIntegrationTest: Verifies role-based access control on /settings/**.
// Ensures OWNER role is granted access while TAILOR role receives HTTP 403 Forbidden.
@SpringBootTest
class SettingsSecurityIntegrationTest {

    @Autowired
    private WebApplicationContext context;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(springSecurity())
                .build();
    }

    @Test
    @DisplayName("Unauthenticated user accessing /settings/garments is redirected to /login")
    void testUnauthenticated_redirectsToLogin() throws Exception {
        mockMvc.perform(get("/settings/garments"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
    }

    @Test
    @DisplayName("TAILOR role accessing /settings/garments receives HTTP 403 Forbidden")
    @WithMockUser(username = "9876543211", roles = {"TAILOR"})
    void testTailorRole_returns403Forbidden() throws Exception {
        mockMvc.perform(get("/settings/garments"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("OWNER role accessing /settings/garments receives HTTP 200 OK")
    @WithMockUser(username = "9876543210", roles = {"OWNER"})
    void testOwnerRole_returns200Ok() throws Exception {
        mockMvc.perform(get("/settings/garments"))
                .andExpect(status().isOk())
                .andExpect(view().name("settings/garments"));
    }

    @Test
    @DisplayName("OWNER role accessing /settings/garments/{id} receives HTTP 200 OK")
    @WithMockUser(username = "9876543210", roles = {"OWNER"})
    void testOwnerRole_canAccessGarmentTemplate() throws Exception {
        mockMvc.perform(get("/settings/garments/1"))
                .andExpect(status().isOk())
                .andExpect(view().name("settings/garment-template"))
                .andExpect(model().attributeExists("garment", "fields", "fieldForm"));
    }

    @Test
    @DisplayName("TAILOR role accessing /settings/garments/{id} receives HTTP 403 Forbidden")
    @WithMockUser(username = "9876543211", roles = {"TAILOR"})
    void testTailorRole_cannotAccessGarmentTemplate() throws Exception {
        mockMvc.perform(get("/settings/garments/1"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Dashboard page renders with Sarvam design system CSS link and Inter font")
    @WithMockUser(username = "9876543210", roles = {"OWNER"})
    void testDashboard_rendersSarvamDesignSystem() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(view().name("home/index"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("/css/app.css")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Inter:wght@400;500;600;700")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("btn-primary")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("btn-secondary")));
    }

    @Test
    @DisplayName("Order details page renders successfully")
    @WithMockUser(username = "9876543210", roles = {"OWNER"})
    void testOrderDetail_rendersSuccessfully() throws Exception {
        mockMvc.perform(get("/orders/1"))
                .andExpect(status().isOk())
                .andExpect(view().name("orders/detail"));
    }
}
