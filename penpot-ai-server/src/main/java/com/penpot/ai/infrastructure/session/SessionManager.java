package com.penpot.ai.infrastructure.session;

import com.penpot.ai.core.domain.SessionCriteria;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manager pour gérer les sessions WebSocket du plugin.
 * Centralise la gestion des sessions et déléguée la logique de sélection.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SessionManager {

    private final SessionSelectionStrategy selectionStrategy;

    /**
     * Sessions actives indexées par ID de session.
     */
    private final Map<String, WebSocketSession> sessions = new ConcurrentHashMap<>();

    /**
     * Mapping session ID -> user token pour le mode multi-utilisateur.
     */
    private final Map<String, String> sessionTokens = new ConcurrentHashMap<>();

    /**
     * Enregistre une nouvelle session.
     * 
     * @param session la session WebSocket à enregistrer
     * @param userToken token utilisateur optionnel
     */
    public void registerSession(WebSocketSession session, String userToken) {
        String sessionId = session.getId();
        sessions.put(sessionId, session);

        if (userToken != null && !userToken.isBlank()) {
            sessionTokens.put(sessionId, userToken);
            log.info("Registered session {} for user token: {}...", 
                sessionId, 
                userToken.substring(0, Math.min(8, userToken.length())));
        } else {
            log.info("Registered session {} (no user token)", sessionId);
        }

        log.debug("Total active sessions: {}", sessions.size());
    }

    /**
     * Désenregistre une session.
     * 
     * @param session la session à retirer
     */
    public void unregisterSession(WebSocketSession session) {
        String sessionId = session.getId();
        sessions.remove(sessionId);
        String userToken = sessionTokens.remove(sessionId);

        log.info("Unregistered session {} (user token: {})", 
            sessionId,
            userToken != null ? userToken.substring(0, Math.min(8, userToken.length())) + "..." : "none");

        log.debug("Total active sessions: {}", sessions.size());
    }

    /**
     * Trouve une session selon des critères.
     * Délègue la logique de sélection à la stratégie configurée.
     * 
     * @param criteria critères de recherche
     * @return la session trouvée ou Optional.empty()
     */
    public Optional<WebSocketSession> findSession(SessionCriteria criteria) {
        log.debug("Finding session with criteria: {}", criteria);

        Optional<WebSocketSession> session = selectionStrategy.selectSession(
            sessions, 
            sessionTokens,
            criteria
        );

        if (session.isPresent()) log.debug("Found session: {}", session.get().getId());
        else log.debug("No session found matching criteria");

        return session;
    }

    /**
     * Vérifie s'il existe des sessions actives.
     * 
     * @return true si au moins une session est active
     */
    public boolean hasActiveSessions() {
        return !sessions.isEmpty();
    }

    /**
     * Obtient le nombre de sessions actives.
     * 
     * @return le nombre de sessions
     */
    public int getActiveSessionCount() {
        return sessions.size();
    }

    /**
     * Obtient le token utilisateur pour une session.
     * 
     * @param sessionId l'ID de la session
     * @return le token ou null si non défini
     */
    public String getUserToken(String sessionId) {
        return sessionTokens.get(sessionId);
    }
}