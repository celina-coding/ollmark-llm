package com.penpot.ai.infrastructure.session;

import com.penpot.ai.core.domain.SessionCriteria;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;
import java.util.*;

/**
 * Stratégie de sélection pour le mode mono-utilisateur.
 * Retourne simplement la première session disponible.
 */
@Slf4j
@Component
@ConditionalOnProperty(
    name = "penpot.multi-user",
    havingValue = "false",
    matchIfMissing = true
)
public class SingleUserSessionStrategy implements SessionSelectionStrategy {

    @Override
    public Optional<WebSocketSession> selectSession(
        Map<String, WebSocketSession> sessions,
        Map<String, String> sessionTokens,
        SessionCriteria criteria
    ) {
        log.debug("Using single-user session selection strategy");
        return sessions.values().stream()
            .filter(session -> {
                if (!criteria.isRequireActive()) return true;
                return session.isOpen();
            })
            .findFirst();
    }
}