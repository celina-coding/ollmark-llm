package com.penpot.mcp.infrastructure.strategy;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import java.util.*;

/**
 * Formatter JSON pour les objets complexes (Map, List, POJO).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JsonResultFormatter implements ResultFormatter {
    private final ObjectMapper objectMapper;

    @Override
    public String format(Object result) {
        if (result == null) return "null";

        try {
            return objectMapper.writerWithDefaultPrettyPrinter()
                .writeValueAsString(result);
        } catch (JsonProcessingException e) {
            log.error("Failed to format result as JSON", e);
            return result.toString();
        }
    }

    @Override
    public boolean supports(Class<?> resultType) {
        return Map.class.isAssignableFrom(resultType) 
            || List.class.isAssignableFrom(resultType)
            || resultType.getName().startsWith("com.penpot");
    }

    @Override
    public int priority() {
        return 10; // Haute priorité pour les types complexes
    }
}