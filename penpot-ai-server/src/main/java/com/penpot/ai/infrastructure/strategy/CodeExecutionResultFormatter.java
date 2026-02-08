package com.penpot.ai.infrastructure.strategy;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.penpot.ai.shared.util.JsonUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import java.util.*;

/**
 * Formatter spécialisé pour les données d'exécution de code.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CodeExecutionResultFormatter implements ResultFormatter {

    private final ObjectMapper objectMapper;

    @Override
    public String format(Object result) {
        if (!(result instanceof Map)) return result.toString();

        @SuppressWarnings("unchecked")
        Map<String, Object> data = (Map<String, Object>) result;

        StringBuilder formatted = new StringBuilder();
        formatted.append("{\n");

        if (data.containsKey("result")) {
            formatted.append("  \"result\": ");
            formatted.append(formatValue(data.get("result")));
            formatted.append(",\n");
        }

        if (data.containsKey("log") && data.get("log") != null) {
            formatted.append("  \"log\": ");
            formatted.append(JsonUtils.escapeJson(data.get("log").toString()));
            formatted.append("\n");
        } else {
            int lastComma = formatted.lastIndexOf(",");
            if (lastComma > 0) {
                formatted.delete(lastComma, lastComma + 1);
                formatted.append("\n");
            }
        }

        formatted.append("}");
        return formatted.toString();
    }

    @Override
    public boolean supports(Class<?> resultType) {
        return Map.class.isAssignableFrom(resultType);
    }

    @Override
    public int priority() {
        return 15;
    }

    private String formatValue(Object value) {
        if (value == null) return "null";
        if (value instanceof String) return JsonUtils.escapeJson((String) value);
        if (value instanceof Number || value instanceof Boolean) {
            return value.toString();
        }

        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            log.warn("Failed to serialize value", e);
            return JsonUtils.escapeJson(value.toString());
        }
    }
}