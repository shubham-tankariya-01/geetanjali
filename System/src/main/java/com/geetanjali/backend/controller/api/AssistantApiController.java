package com.geetanjali.backend.controller.api;

import com.geetanjali.backend.dto.assistant.ChatRequest;
import com.geetanjali.backend.dto.assistant.ChatResponse;
import com.geetanjali.backend.security.AppUserDetails;
import com.geetanjali.backend.service.assistant.AssistantService;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * AssistantApiController: REST controller serving AJAX chat queries from the floating assistant widget.
 * Protected by Spring Security and CSRF tokens.
 */
@RestController
@RequestMapping("/api/assistant")
public class AssistantApiController {

    private final AssistantService assistantService;

    public AssistantApiController(AssistantService assistantService) {
        this.assistantService = assistantService;
    }

    /**
     * Handles POST /api/assistant/chat:
     * Receives message and active page context from browser widget, delegates to AssistantService.
     */
    @PostMapping("/chat")
    public ResponseEntity<ChatResponse> chat(
            @RequestBody ChatRequest request,
            @AuthenticationPrincipal AppUserDetails userDetails,
            HttpSession session) {

        if (userDetails == null || userDetails.getUser() == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ChatResponse("Session expired. Please log in again.", "system"));
        }

        ChatResponse response = assistantService.processUserMessage(userDetails.getUser(), request, session);
        return ResponseEntity.ok(response);
    }

    /**
     * Handles POST /api/assistant/clear:
     * Clears the rolling conversation window in the user's session.
     */
    @PostMapping("/clear")
    public ResponseEntity<Map<String, Object>> clearHistory(HttpSession session) {
        assistantService.clearHistory(session);
        return ResponseEntity.ok(Map.of("cleared", true));
    }
}
