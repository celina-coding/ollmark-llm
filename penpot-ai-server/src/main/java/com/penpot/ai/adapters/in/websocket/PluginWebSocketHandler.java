package com.penpot.ai.adapters.in.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.penpot.ai.application.service.TaskOrchestrator;
import com.penpot.ai.infrastructure.session.SessionManager;
import com.penpot.ai.model.PluginTaskResponse;
import com.penpot.ai.shared.util.JsonUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.*;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import java.io.IOException;
import java.util.Map;

/**
 * Handler WebSocket pour la communication avec le plugin Penpot.
 * 
 * Responsabilités:
 * - Gestion du cycle de vie des connexions WebSocket
 * - Réception et parsing des réponses de tâches
 * - Notification du TaskOrchestrator pour compléter les futures en attente
 * - Gestion des erreurs de transport et de parsing
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PluginWebSocketHandler extends TextWebSocketHandler {

    private final ObjectMapper objectMapper;
    private final SessionManager sessionManager;
    private final TaskOrchestrator taskOrchestrator;

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
     * Gère les messages texte entrants du plugin.
     * C'est ici que les réponses de tâches sont reçues et traitées.
     */
    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        String payload = message.getPayload();

        log.debug("Received WebSocket message from session {}: {}", 
            session.getId(), 
            JsonUtils.truncateForLog(payload, 200));

        try {
            PluginTaskResponse<?> response = parseTaskResponse(payload);

            if (response == null) {
                log.warn("Received non-task-response message from session {}: {}", 
                    session.getId(), 
                    JsonUtils.truncateForLog(payload, 100));
                return;
            }

            log.info("Parsed task response: id={}, success={}", 
                response.getId(), 
                response.getSuccess());

            boolean notified = taskOrchestrator.notifyResponse(response);

            if (!notified) {
                log.warn("Received response for unknown or expired task: {}", 
                    response.getId());
            }
        } catch (Exception e) {
            log.error("Failed to handle WebSocket message from session {}", 
                session.getId(), e);

            String taskId = tryExtractTaskId(payload);
            if (taskId != null) taskOrchestrator.notifyError(taskId, e);
        }
    }

    /**
     * Appelé lorsqu'une connexion est fermée.
     * Nettoie les ressources associées et annule les tâches en attente.
     */
    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        sessionManager.unregisterSession(session);

        log.info("WebSocket connection closed: {} - {} (active connections: {})", 
            session.getId(),
            status, 
            sessionManager.getActiveSessionCount());

        // Si c'était la dernière session, annuler toutes les tâches en attente
        if (!sessionManager.hasActiveSessions()) {
            log.warn("No active sessions remaining - cancelling all pending tasks");
            taskOrchestrator.cancelAllPendingTasks(
                "WebSocket connection closed: " + status.getReason()
            );
        }
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
     * Supporte les deux formats de réponse:
     * 1. Format enveloppe: {type: "task-response", response: {...}}
     * 2. Format direct: {id: "...", success: true, ...}
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

            if ("task-response".equals(messageMap.get("type"))) {
                @SuppressWarnings("unchecked")
                Map<String, Object> responseMap = 
                    (Map<String, Object>) messageMap.get("response");

                if (responseMap == null) {
                    log.warn("task-response envelope missing 'response' field");
                    return null;
                }

                return objectMapper.convertValue(responseMap, PluginTaskResponse.class);
            }

            // Format direct: {id: "...", success: true, ...}
            if (messageMap.containsKey("id") && messageMap.containsKey("success")) {
                return objectMapper.convertValue(messageMap, PluginTaskResponse.class);
            }

            return null;
        } catch (Exception e) {
            log.error("Failed to parse task response", e);
            throw new RuntimeException("Failed to parse task response: " + e.getMessage(), e);
        }
    }

    /**
     * Tente d'extraire un task ID d'un payload malformé pour la gestion d'erreur.
     * 
     * @param payload le payload JSON (potentiellement malformé)
     * @return le task ID si trouvé, null sinon
     */
    private String tryExtractTaskId(String payload) {
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> map = objectMapper.readValue(payload, Map.class);

            Object id = map.get("id");
            if (id != null) {
                return id.toString();
            }

            @SuppressWarnings("unchecked")
            Map<String, Object> response = (Map<String, Object>) map.get("response");
            if (response != null) {
                id = response.get("id");
                if (id != null) return id.toString();
            }

            return null;
        } catch (Exception e) {
            log.debug("Could not extract task ID from malformed payload", e);
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