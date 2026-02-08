package com.penpot.ai.infrastructure.session;

import com.penpot.ai.core.domain.SessionCriteria;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;
import java.util.*;

/**
 * Stratégie de sélection pour le mode multi-utilisateur.
 * Sélectionne la session correspondant au token utilisateur.
 */
@Slf4j
@Component
@ConditionalOnProperty(
    name = "penpot.multi-user",
    havingValue = "true"
)
public class MultiUserSessionStrategy implements SessionSelectionStrategy {

    @Override
    public Optional<WebSocketSession> selectSession(
        Map<String, WebSocketSession> sessions,
        Map<String, String> sessionTokens,
        SessionCriteria criteria
    ) {
        log.debug("Using multi-user session selection strategy");
        if (criteria.getUserToken().isEmpty()) {
            log.warn("No user token provided in multi-user mode");
            return Optional.empty();
        }

        String requestedToken = criteria.getUserToken().get();

        return sessions.entrySet().stream()
            .filter(entry -> {
                String sessionId = entry.getKey();
                WebSocketSession session = entry.getValue();
                String sessionToken = sessionTokens.get(sessionId);

                if (!requestedToken.equals(sessionToken)) return false;
                if (criteria.isRequireActive()) return session.isOpen();

                return true;
            })
            .map(Map.Entry::getValue)
            .findFirst();
    }
}