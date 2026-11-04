package com.geetanjali.backend.service.assistant;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.geetanjali.backend.config.AssistantProperties;
import com.geetanjali.backend.dto.assistant.ChatRequest;
import com.geetanjali.backend.dto.assistant.ChatResponse;
import com.geetanjali.backend.dto.assistant.PageContext;
import com.geetanjali.backend.entity.AppUser;
import com.geetanjali.backend.entity.Shop;
import com.geetanjali.backend.enums.Role;
import com.geetanjali.backend.service.assistant.model.AssistantMessage;
import com.geetanjali.backend.service.assistant.model.ToolCall;
import com.geetanjali.backend.service.assistant.model.ToolResult;
import com.geetanjali.backend.service.assistant.provider.FallbackLlmClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpSession;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AssistantServiceTest {

    @Mock
    private FallbackLlmClient llmClient;

    @Mock
    private ToolRegistryService toolRegistryService;

    @Mock
    private com.geetanjali.backend.service.ShopService shopService;

    private AssistantProperties properties;
    private ObjectMapper objectMapper;
    private Clock fixedClock;

    private AssistantService assistantService;
    private AppUser owner;

    @BeforeEach
    void setUp() {
        properties = new AssistantProperties();
        properties.setRollingWindowSize(4);
        properties.getGroq().setMaxTokens(900);

        objectMapper = new ObjectMapper();
        fixedClock = Clock.fixed(Instant.parse("2026-11-04T10:00:00Z"), ZoneId.of("Asia/Kolkata"));

        Shop shop = new Shop("Geetanjali Studio");
        shop.setId(1L);

        lenient().when(shopService.getPrimaryShop()).thenReturn(shop);
        lenient().when(shopService.getShopName()).thenReturn("Geetanjali Studio");

        assistantService = new AssistantService(
                llmClient,
                toolRegistryService,
                shopService,
                properties,
                objectMapper,
                fixedClock
        );

        owner = new AppUser(shop, "Shubham Owner", "9876543210", "hash", Role.OWNER);
        owner.setId(10L);
    }

    @Test
    @DisplayName("Falls back to deterministic rule engine when no LLM key is configured")
    void testProcessMessage_offlineRuleEngine() {
        when(llmClient.isConfigured()).thenReturn(false);

        when(toolRegistryService.executeTool(eq(1L), eq(Role.OWNER), any(ToolCall.class)))
                .thenReturn(ToolResult.ok(Map.of(
                        "found", true,
                        "formattedOrderNo", "TS-2026-1001",
                        "customerName", "Ramesh Patel",
                        "nearestDeliveryDate", "2026-11-10",
                        "totalAmount", "1500.00",
                        "balanceAmount", "500.00",
                        "isPaidInFull", false,
                        "isAllDelivered", false,
                        "orderId", 101L
                )));

        MockHttpSession session = new MockHttpSession();
        ChatRequest request = new ChatRequest("What is the delivery date of order 1001?", new PageContext());

        ChatResponse response = assistantService.processUserMessage(owner, request, session);

        assertThat(response.getProvider()).isEqualTo("assistant");
        assertThat(response.getReply()).contains("TS-2026-1001");
        assertThat(response.getReply()).contains("2026-11-10");
        assertThat(response.getActions()).isNotEmpty();
    }

    @Test
    @DisplayName("Executes full LLM tool-calling loop when LLM provider is active")
    void testProcessMessage_llmWithToolCall() throws Exception {
        when(llmClient.isConfigured()).thenReturn(true);

        // Turn 1: LLM decides to call getOrderStatus
        AssistantMessage toolCallMsg = new AssistantMessage("assistant", "");
        toolCallMsg.setToolCalls(List.of(new ToolCall("call_abc", "getOrderStatus", Map.of("orderIdentifier", "1001"))));

        // Turn 2: LLM generates final answer based on tool observation
        AssistantMessage finalMsg = new AssistantMessage("assistant", "Order TS-2026-1001 will be ready on 10th November 2026.");

        when(llmClient.chat(anyList(), anyList(), eq(900)))
                .thenReturn(toolCallMsg)
                .thenReturn(finalMsg);

        when(toolRegistryService.executeTool(eq(1L), eq(Role.OWNER), any(ToolCall.class)))
                .thenReturn(ToolResult.ok(Map.of(
                        "found", true,
                        "formattedOrderNo", "TS-2026-1001",
                        "nearestDeliveryDate", "2026-11-10"
                )));

        MockHttpSession session = new MockHttpSession();
        ChatRequest request = new ChatRequest("When is order 1001 ready?", new PageContext("/orders", "ORDERS_LIST", null, null));

        ChatResponse response = assistantService.processUserMessage(owner, request, session);

        assertThat(response.getProvider()).isEqualTo("assistant");
        assertThat(response.getReply()).contains("10th November 2026");
        verify(toolRegistryService).executeTool(eq(1L), eq(Role.OWNER), any(ToolCall.class));
    }

    @Test
    @DisplayName("Studio Guardrail intercepts code generation requests without calling LLM")
    void testProcessMessage_guardrailBlocksCodeGeneration() {
        MockHttpSession session = new MockHttpSession();
        ChatRequest request = new ChatRequest("give me python code to reverse a string", new PageContext());

        ChatResponse response = assistantService.processUserMessage(owner, request, session);

        assertThat(response.getReply()).contains("Geetanjali Tailoring Studio operations");
        assertThat(response.getReply()).contains("cannot generate programming code");
        verifyNoInteractions(llmClient);
        verifyNoInteractions(toolRegistryService);
    }
}
