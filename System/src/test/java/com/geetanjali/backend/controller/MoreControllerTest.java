package com.geetanjali.backend.controller;

import com.geetanjali.backend.entity.Garment;
import com.geetanjali.backend.service.GarmentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ui.ConcurrentModel;
import org.springframework.ui.Model;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

// MoreControllerTest: Unit tests verifying MoreController routes.
@ExtendWith(MockitoExtension.class)
class MoreControllerTest {

    @Mock
    private GarmentService garmentService;

    private MoreController moreController;

    @BeforeEach
    void setUp() {
        moreController = new MoreController(garmentService);
    }

    @Test
    @DisplayName("showMorePage adds active garments and returns view 'more'")
    void testShowMorePage() {
        Garment shirt = new Garment("Shirt", 1, true);
        when(garmentService.listActiveGarments()).thenReturn(List.of(shirt));

        Model model = new ConcurrentModel();
        String view = moreController.showMorePage(model);

        assertEquals("more", view);
        assertTrue(model.containsAttribute("garments"));
        List<?> garments = (List<?>) model.getAttribute("garments");
        assertEquals(1, garments.size());
    }

    @Test
    @DisplayName("showGarments redirects /garments to /settings/garments")
    void testShowGarments() {
        String view = moreController.showGarments();
        assertEquals("redirect:/settings/garments", view);
    }

    @Test
    @DisplayName("redirectToSettings redirects /settings to /settings/garments")
    void testRedirectToSettings() {
        String view = moreController.redirectToSettings();
        assertEquals("redirect:/settings/garments", view);
    }
}
