package com.penpot.mcp.infrastructure.session;

import com.penpot.mcp.core.domain.SessionCriteria;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;
import java.util.*;

/**
 * Stratégie de sélection par round-robin pour répartir la charge.
 */
@Slf4j
public class RoundRobinSessionStrategy implements SessionSelectionStrategy {

    private int currentIndex = 0;

    @Override
    public Optional<WebSocketSession> selectSession(
        Map<String, WebSocketSession> sessions,
        Map<String, String> sessionTokens,
        SessionCriteria criteria
    ) {
        log.debug("Using round-robin session selection strategy");

        if (sessions.isEmpty()) return Optional.empty();

        var sessionList = sessions.values().stream()
            .filter(session -> !criteria.isRequireActive() || session.isOpen())
            .toList();

        if (sessionList.isEmpty()) return Optional.empty();

        WebSocketSession selected = sessionList.get(currentIndex % sessionList.size());
        currentIndex = (currentIndex + 1) % sessionList.size();

        log.debug("Selected session (round-robin): {}", selected.getId());
        return Optional.of(selected);
    }
}