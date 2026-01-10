package com.penpot.mcp.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.penpot.mcp.model.PluginTaskResponse;
import com.penpot.mcp.service.PluginBridge;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.*;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Handler WebSocket pour la communication avec le plugin Penpot.
 * <p>
 * Ce composant gère:
 * </p>
 * <ul>
 *   <li>Les connexions/déconnexions WebSocket du plugin</li>
 *   <li>La réception et le routage des messages</li>
 *   <li>Le support multi-utilisateur via tokens</li>
 *   <li>La gestion des erreurs de transport</li>
 * </ul>
 * <p>
 * Le handler communique avec {@link PluginBridge} pour résoudre les
 * tâches en attente lorsque des réponses sont reçues.
 */
@Slf4j
@Component
public class PluginWebSocketHandler extends TextWebSocketHandler {

    /** Mapper JSON pour la sérialisation/désérialisation */
    private final ObjectMapper objectMapper;

    /** Service pont pour l'exécution des tâches (injection lazy) */
    private PluginBridge pluginBridge;

    /** Sessions WebSocket actives, indexées par ID de session */
    private final Map<String, WebSocketSession> sessions = new ConcurrentHashMap<>();

    /** Mapping session ID -> user token pour le mode multi-utilisateur */
    private final Map<String, String> sessionTokens = new ConcurrentHashMap<>();

    /**
     * Construit le handler avec le mapper JSON.
     *
     * @param objectMapper le mapper pour la sérialisation JSON
     */
    public PluginWebSocketHandler(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * Injection lazy du PluginBridge pour éviter la dépendance circulaire.
     * <p>
     * Cette méthode est appelée par {@link WebSocketConfig#init()} après
     * la construction des deux beans.
     *
     * @param pluginBridge le service pont à utiliser
     */
    @Lazy
    public void setPluginBridge(PluginBridge pluginBridge) {
        this.pluginBridge = pluginBridge;
    }

    /**
     * Appelé lorsqu'une nouvelle connexion WebSocket est établie.
     * <p>
     * Extrait le userToken si présent dans l'URI et enregistre la session.
     *
     * @param session la session WebSocket nouvellement connectée
     */
    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        String userToken = extractUserToken(session);

        if (userToken != null) {
            log.info("New WebSocket connection established for user: {}", 
                userToken.substring(0, Math.min(8, userToken.length())) + "...");
            sessionTokens.put(session.getId(), userToken);
        } else {
            log.info("New WebSocket connection established (no user token)");
        }

        sessions.put(session.getId(), session);
        log.info("Total active connections: {}", sessions.size());
    }

    /**
     * Traite les messages texte reçus du plugin.
     * <p>
     * Reconnaît deux formats de message:
     * </p>
     * <ul>
     *   <li>Format enveloppe: {@code {type: "task-response", response: {...}}}</li>
     *   <li>Format direct: {@code {id: "...", success: true, ...}}</li>
     * </ul>
     * <p>
     * Les réponses de tâches sont transmises à {@link PluginBridge} pour
     * résolution des CompletableFutures en attente.
     *
     * @param session la session qui a envoyé le message
     * @param message le message texte reçu
     */
    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        try {
            String payload = message.getPayload();
            log.debug("Received WebSocket message: {}", payload);

            @SuppressWarnings("unchecked")
            Map<String, Object> messageMap = objectMapper.readValue(payload, Map.class);

            PluginTaskResponse<?> response;
            if ("task-response".equals(messageMap.get("type"))) {
                @SuppressWarnings("unchecked")
                Map<String, Object> responseMap = (Map<String, Object>) messageMap.get("response");
                response = objectMapper.convertValue(responseMap, PluginTaskResponse.class);
            } else if (messageMap.containsKey("id") && messageMap.containsKey("success")) {
                response = objectMapper.convertValue(messageMap, PluginTaskResponse.class);
            } else {
                log.debug("Received non-task-response message: {}", messageMap.get("type"));
                return;
            }

            if (pluginBridge != null) {
                pluginBridge.handleTaskResponse(response);
            } else {
                log.warn("PluginBridge not initialized, cannot handle task response");
            }
        } catch (Exception e) {
            log.error("Failed to process WebSocket message", e);
        }
    }

    /**
     * Appelé lorsqu'une connexion est fermée.
     * <p>
     * Nettoie les ressources associées à la session.
     *
     * @param session la session fermée
     * @param status le statut de fermeture
     */
    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        String userToken = sessionTokens.get(session.getId());
        log.info("WebSocket connection closed: {} (user: {})", 
            status, 
            userToken != null ? userToken.substring(0, Math.min(8, userToken.length())) + "..." : "none");

        sessions.remove(session.getId());
        sessionTokens.remove(session.getId());
        log.info("Total active connections: {}", sessions.size());
    }

    /**
     * Gère les erreurs de transport WebSocket.
     * <p>
     * Ferme la session en erreur et nettoie les ressources.
     * </p>
     *
     * @param session la session en erreur
     * @param exception l'exception survenue
     */
    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) {
        log.error("WebSocket transport error for session {}", session.getId(), exception);
        try {
            session.close();
        } catch (IOException e) {
            log.error("Failed to close session after transport error", e);
        }
        sessions.remove(session.getId());
        sessionTokens.remove(session.getId());
    }

    /**
     * Envoie un message à une session spécifique.
     *
     * @param sessionId l'ID de la session destinataire
     * @param message le message à envoyer
     * @throws IOException si l'envoi échoue
     * @throws IllegalStateException si la session n'existe pas ou est fermée
     */
    public void sendMessage(String sessionId, String message) throws IOException {
        WebSocketSession session = sessions.get(sessionId);
        if (session != null && session.isOpen()) {
            session.sendMessage(new TextMessage(message));
        } else {
            throw new IllegalStateException("Session not found or closed: " + sessionId);
        }
    }

    /**
     * Retourne la map des sessions actives.
     *
     * @return les sessions actives indexées par ID
     */
    public Map<String, WebSocketSession> getSessions() {
        return sessions;
    }

    /**
     * Obtient le token utilisateur associé à une session.
     *
     * @param sessionId l'ID de la session
     * @return le token utilisateur ou null si non défini
     */
    public String getUserToken(String sessionId) {
        return sessionTokens.get(sessionId);
    }

    /**
     * Extrait le userToken de l'URI de la session WebSocket.
     * <p>
     * Recherche le paramètre {@code userToken=...} dans la query string.
     * </p>
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