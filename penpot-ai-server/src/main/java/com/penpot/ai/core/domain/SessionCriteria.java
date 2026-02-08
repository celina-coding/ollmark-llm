package com.penpot.ai.core.domain;

import lombok.*;
import java.util.Optional;

/**
 * Critères pour la sélection d'une session WebSocket.
 * Value Object pour encapsuler les critères de recherche.
 */
@Value
@Builder
public class SessionCriteria {
    @Builder.Default
    Optional<String> userToken = Optional.empty();

    @Builder.Default
    boolean requireActive = true;

    public static SessionCriteria forUser(String userToken) {
        return SessionCriteria.builder()
            .userToken(Optional.ofNullable(userToken))
            .build();
    }

    public static SessionCriteria any() {
        return SessionCriteria.builder().build();
    }
}