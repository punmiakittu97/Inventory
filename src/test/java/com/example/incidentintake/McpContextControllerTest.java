package com.example.incidentintake;

import com.example.incidentintake.mcp.McpContextController;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.definition.ToolDefinition;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class McpContextControllerTest {

    @Mock ToolCallbackProvider toolCallbackProvider;

    McpContextController controller;

    @BeforeEach
    void setUp() {
        controller = new McpContextController(toolCallbackProvider, new ObjectMapper());
    }

    @Test
    void tools_returnsNameDescriptionAndParsedSchema() {
        ToolCallback callback = toolCallback("create_incident", "Create a new incident",
                "{\"type\":\"object\",\"properties\":{\"title\":{\"type\":\"string\"}}}");
        when(toolCallbackProvider.getToolCallbacks()).thenReturn(new ToolCallback[]{callback});

        List<Map<String, Object>> result = controller.tools();

        assertThat(result).hasSize(1);
        Map<String, Object> tool = result.get(0);
        assertThat(tool.get("name")).isEqualTo("create_incident");
        assertThat(tool.get("description")).isEqualTo("Create a new incident");
        assertThat(tool.get("inputSchema")).isNotNull();
    }

    @Test
    void tools_returnsEmptyListWhenNoToolsRegistered() {
        when(toolCallbackProvider.getToolCallbacks()).thenReturn(new ToolCallback[0]);

        assertThat(controller.tools()).isEmpty();
    }

    @Test
    void tools_returnsOneEntryPerRegisteredTool() {
        ToolCallback t1 = toolCallback("create_incident", "Create", "{}");
        ToolCallback t2 = toolCallback("get_incident", "Get", "{}");
        ToolCallback t3 = toolCallback("list_incidents", "List", "{}");
        when(toolCallbackProvider.getToolCallbacks()).thenReturn(new ToolCallback[]{t1, t2, t3});

        List<Map<String, Object>> result = controller.tools();

        assertThat(result).extracting(m -> m.get("name"))
                .containsExactlyInAnyOrder("create_incident", "get_incident", "list_incidents");
    }

    @Test
    void tools_malformedSchema_fallsBackToEmptyObjectInsteadOfThrowing() {
        ToolCallback callback = toolCallback("bad_tool", "desc", "not-valid-json");
        when(toolCallbackProvider.getToolCallbacks()).thenReturn(new ToolCallback[]{callback});

        List<Map<String, Object>> result = controller.tools();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).get("name")).isEqualTo("bad_tool");
        assertThat(result.get(0).get("inputSchema")).isNotNull();
    }

    private ToolCallback toolCallback(String name, String description, String inputSchema) {
        ToolDefinition definition = ToolDefinition.builder()
                .name(name)
                .description(description)
                .inputSchema(inputSchema)
                .build();
        ToolCallback callback = org.mockito.Mockito.mock(ToolCallback.class);
        when(callback.getToolDefinition()).thenReturn(definition);
        return callback;
    }
}
