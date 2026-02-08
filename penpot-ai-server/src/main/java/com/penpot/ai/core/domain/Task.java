package com.penpot.ai.core.domain;

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
}