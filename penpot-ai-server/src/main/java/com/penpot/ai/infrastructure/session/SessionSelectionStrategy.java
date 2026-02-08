package com.penpot.ai.infrastructure.session;

import com.penpot.ai.core.domain.SessionCriteria;
import org.springframework.web.socket.WebSocketSession;
import java.util.*;

/**
 * Strategy Pattern pour la sélection de sessions WebSocket.
 * Permet de changer la logique de sélection selon le mode (mono/multi-utilisateur).
 */
public interface SessionSelectionStrategy {

    /**
     * Sélectionne une session selon les critères.
     * 
     * @param sessions les sessions disponibles
     * @param sessionTokens mapping session ID -> user token
     * @param criteria critères de sélection
     * @return la session sélectionnée ou Optional.empty()
     */
    Optional<WebSocketSession> selectSession(
        Map<String, WebSocketSession> sessions,
        Map<String, String> sessionTokens,
        SessionCriteria criteria
    );
}