package com.geetanjali.backend.controller.api;

import com.geetanjali.backend.dto.assistant.ChatRequest;
import com.geetanjali.backend.dto.assistant.ChatResponse;
import com.geetanjali.backend.dto.assistant.PageContext;
import com.geetanjali.backend.entity.AppUser;
import com.geetanjali.backend.entity.Shop;
import com.geetanjali.backend.enums.Role;
import com.geetanjali.backend.security.AppUserDetails;
import com.geetanjali.backend.service.assistant.AssistantService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpSession;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AssistantApiControllerTest {

    @Mock
    private AssistantService assistantService;

    private AssistantApiController controller;
    private AppUser appUser;
    private AppUserDetails userDetails;

    @BeforeEach
    void setUp() {
        controller = new AssistantApiController(assistantService);

        Shop shop = new Shop("Geetanjali Studio");
        shop.setId(1L);
        appUser = new AppUser(shop, "Shubham", "9876543210", "hash", Role.OWNER);
        appUser.setId(10L);
        userDetails = new AppUserDetails(appUser);
    }

    @Test
    @DisplayName("POST /api/assistant/chat returns 200 with chat response for authenticated user")
    void testChat_authenticated_returns200() {
        MockHttpSession session = new MockHttpSession();
        ChatRequest request = new ChatRequest("Pending workload today?", new PageContext("/orders", "ORDERS_LIST", null, null));
        ChatResponse expected = new ChatResponse("You have 3 garments pending stitching.", "groq");

        when(assistantService.processUserMessage(eq(appUser), eq(request), eq(session))).thenReturn(expected);

        ResponseEntity<ChatResponse> response = controller.chat(request, userDetails, session);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getReply()).isEqualTo("You have 3 garments pending stitching.");
        assertThat(response.getBody().getProvider()).isEqualTo("groq");
    }

    @Test
    @DisplayName("POST /api/assistant/chat returns 401 when unauthenticated")
    void testChat_unauthenticated_returns401() {
        MockHttpSession session = new MockHttpSession();
        ChatRequest request = new ChatRequest("Test", new PageContext());

        ResponseEntity<ChatResponse> response = controller.chat(request, null, session);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    @DisplayName("POST /api/assistant/clear clears session conversation history")
    void testClearHistory_returnsOk() {
        MockHttpSession session = new MockHttpSession();

        ResponseEntity<Map<String, Object>> response = controller.clearHistory(session);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).containsEntry("cleared", true);
        verify(assistantService).clearHistory(session);
    }
}
