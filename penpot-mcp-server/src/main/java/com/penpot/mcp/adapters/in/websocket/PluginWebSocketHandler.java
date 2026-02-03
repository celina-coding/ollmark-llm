package com.penpot.mcp.adapters.in.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.penpot.mcp.infrastructure.session.SessionManager;
import com.penpot.mcp.model.PluginTaskResponse;
import com.penpot.mcp.shared.util.JsonUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.*;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import java.io.IOException;
import java.util.Map;

/**
 * Handler WebSocket pour la communication avec le plugin Penpot.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PluginWebSocketHandler extends TextWebSocketHandler {

    private final ObjectMapper objectMapper;
    private final SessionManager sessionManager;

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
            if (session.isOpen()) session.close(CloseStatus.SERVER_ERROR);
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
            if (!JsonUtils.isValidJson(payload)) {
                log.warn("Received invalid JSON payload");
                return null;
            }

            @SuppressWarnings("unchecked")
            Map<String, Object> messageMap = objectMapper.readValue(payload, Map.class);

            // Format enveloppe: {type: "task-response", response: {...}}
            if ("task-response".equals(messageMap.get("type"))) {
                @SuppressWarnings("unchecked")
                Map<String, Object> responseMap = 
                    (Map<String, Object>) messageMap.get("response");
                return objectMapper.convertValue(responseMap, PluginTaskResponse.class);
            }

            // Format direct: {id: "...", success: true, ...}
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
}