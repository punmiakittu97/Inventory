package com.example.incidentintake.mcp;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@Slf4j
public class McpContextController {

    private final ToolCallbackProvider toolCallbackProvider;
    private final ObjectMapper objectMapper;

    @GetMapping("/mcp/tools")
    public List<Map<String, Object>> tools() {
        return Arrays.stream(toolCallbackProvider.getToolCallbacks())
                .map(tc -> {
                    var def = tc.getToolDefinition();
                    JsonNode schema;
                    try {
                        schema = objectMapper.readTree(def.inputSchema());
                    } catch (Exception e) {
                        log.warn("event=SCHEMA_PARSE_ERROR tool={}", def.name());
                        schema = objectMapper.createObjectNode();
                    }
                    return Map.<String, Object>of(
                            "name", def.name(),
                            "description", def.description(),
                            "inputSchema", schema
                    );
                })
                .toList();
    }
}
