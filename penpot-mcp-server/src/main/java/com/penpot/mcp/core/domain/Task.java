package com.penpot.mcp.core.domain;

import lombok.*;
import java.util.*;

/**
 * Value Object représentant une tâche à exécuter dans le plugin.
 * Immutable pour garantir la cohérence des données (Domain-Driven Design).
 */
@Value
@Builder
public class Task {
    String id;
    TaskType type;
    Map<String, Object> parameters;
    @Builder.Default
    Optional<String> userToken = Optional.empty();

    /**
     * Factory method pour créer une tâche avec un ID auto-généré.
     */
    public static Task create(TaskType type, Map<String, Object> params) {
        return Task.builder()
            .id(UUID.randomUUID().toString())
            .type(type)
            .parameters(Collections.unmodifiableMap(params))
            .build();
    }

    /**
     * Factory method pour créer une tâche avec token utilisateur.
     */
    public static Task create(TaskType type, Map<String, Object> params, String userToken) {
        return Task.builder()
            .id(UUID.randomUUID().toString())
            .type(type)
            .parameters(Collections.unmodifiableMap(params))
            .userToken(Optional.ofNullable(userToken))
            .build();
    }
}