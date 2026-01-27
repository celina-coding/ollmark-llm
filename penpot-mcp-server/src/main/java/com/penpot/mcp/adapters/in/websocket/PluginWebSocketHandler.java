package com.penpot.mcp.adapters.in.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.penpot.mcp.application.service.TaskOrchestrator;
import com.penpot.mcp.infrastructure.session.SessionManager;
import com.penpot.mcp.model.PluginTaskResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.*;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import java.io.IOException;
import java.util.Map;

/**
 * Handler WebSocket pour la communication avec le plugin Penpot.
 * Refactorisé pour utiliser SessionManager et TaskOrchestrator.
 * Suit le Single Responsibility Principle en déléguant les responsabilités.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PluginWebSocketHandler extends TextWebSocketHandler {

    private final ObjectMapper objectMapper;
    private final SessionManager sessionManager;
    private final TaskOrchestrator responseOrchestrator;

    /**
     * Appelé lorsqu'une nouvelle connexion WebSocket est établie.
     * Extrait le userToken si présent et enregistre la session.
     */
    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        String userToken = extractUserToken(session);
        sessionManager.registerSession(session, userToken);

        log.info("WebSocket connection established: {} (active connections: {})", 
            session.getId(), 
            sessionManager.getActiveSessionCount());
    }

    /**
     * Traite les messages texte reçus du plugin.
     * Reconnaît deux formats :
     * - Format enveloppe: {type: "task-response", response: {...}}
     * - Format direct: {id: "...", success: true, ...}
     */
    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        try {
            String payload = message.getPayload();
            log.debug("Received WebSocket message from session {}: {}", 
                session.getId(), 
                truncateForLog(payload));

            PluginTaskResponse<?> response = parseTaskResponse(payload);

            if (response != null) {
                boolean handled = responseOrchestrator.notifyResponse(response);

                if (!handled) {
                    log.warn("Response for task {} was not handled (no pending task)", 
                        response.getId());
                }
            } else {
                log.debug("Received non-task-response message, ignoring");
            }
        } catch (Exception e) {
            log.error("Failed to process WebSocket message from session {}", 
                session.getId(), e);
        }
    }

    /**
     * Appelé lorsqu'une connexion est fermée.
     * Nettoie les ressources associées.
     */
    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        sessionManager.unregisterSession(session);

        log.info("WebSocket connection closed: {} - {} (active connections: {})", 
            session.getId(),
            status, 
            sessionManager.getActiveSessionCount());
    }

    /**
     * Gère les erreurs de transport WebSocket.
     * Ferme la session en erreur et nettoie les ressources.
     */
    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) {
        log.error("WebSocket transport error for session {}", 
            session.getId(), exception);

        try {
            if (session.isOpen()) {
                session.close(CloseStatus.SERVER_ERROR);
            }
        } catch (IOException e) {
            log.error("Failed to close session after transport error", e);
        }

        sessionManager.unregisterSession(session);
    }

    /**
     * Parse un message JSON en PluginTaskResponse.
     * Supporte les deux formats de réponse.
     * 
     * @param payload le message JSON
     * @return la réponse parsée ou null si ce n'est pas une task response
     */
    private PluginTaskResponse<?> parseTaskResponse(String payload) {
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> messageMap = objectMapper.readValue(payload, Map.class);

            if ("task-response".equals(messageMap.get("type"))) {
                @SuppressWarnings("unchecked")
                Map<String, Object> responseMap = 
                    (Map<String, Object>) messageMap.get("response");
                return objectMapper.convertValue(responseMap, PluginTaskResponse.class);
            }

            if (messageMap.containsKey("id") && messageMap.containsKey("success")) {
                return objectMapper.convertValue(messageMap, PluginTaskResponse.class);
            }

            return null;
        } catch (Exception e) {
            log.error("Failed to parse task response", e);
            return null;
        }
    }

    /**
     * Extrait le userToken de l'URI de la session WebSocket.
     * Recherche le paramètre userToken=... dans la query string.
     * 
     * @param session la session WebSocket
     * @return le token utilisateur ou null si absent
     */
    private String extractUserToken(WebSocketSession session) {
        String query = session.getUri().getQuery();
        if (query != null && query.contains("userToken=")) {
            String[] params = query.split("&");
            for (String param : params) {
                if (param.startsWith("userToken=")) {
                    return param.substring("userToken=".length());
                }
            }
        }
        return null;
    }

    /**
     * Tronque un message pour les logs.
     * 
     * @param message le message à tronquer
     * @return le message tronqué si nécessaire
     */
    private String truncateForLog(String message) {
        if (message == null) return "null";
        if (message.length() <= 200) return message;
        return message.substring(0, 200) + "... (truncated)";
    }
}