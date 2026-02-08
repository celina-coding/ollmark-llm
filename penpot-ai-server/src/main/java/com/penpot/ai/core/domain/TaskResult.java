package com.penpot.ai.core.domain;

import lombok.*;
import java.util.*;

/**
 * Value Object représentant le résultat d'une exécution de tâche.
 * Immutable et type-safe.
 */
@Value
@Builder
public class TaskResult {
    boolean success;

    @Builder.Default
    Optional<Object> data = Optional.empty();

    @Builder.Default
    Optional<String> error = Optional.empty();

    @Builder.Default
    List<String> logs = Collections.emptyList();

    /**
     * Factory method pour un résultat réussi.
     */
    public static TaskResult success(Object data) {
        return TaskResult.builder()
            .success(true)
            .data(Optional.ofNullable(data))
            .build();
    }

    /**
     * Factory method pour un résultat réussi avec logs.
     */
    public static TaskResult success(Object data, List<String> logs) {
        return TaskResult.builder()
            .success(true)
            .data(Optional.ofNullable(data))
            .logs(logs != null ? Collections.unmodifiableList(logs) : Collections.emptyList())
            .build();
    }

    /**
     * Factory method pour un échec.
     */
    public static TaskResult failure(String error) {
        return TaskResult.builder()
            .success(false)
            .error(Optional.of(error))
            .build();
    }

    /**
     * Factory method pour un échec avec logs.
     */
    public static TaskResult failure(String error, List<String> logs) {
        return TaskResult.builder()
            .success(false)
            .error(Optional.of(error))
            .logs(logs != null ? Collections.unmodifiableList(logs) : Collections.emptyList())
            .build();
    }
}