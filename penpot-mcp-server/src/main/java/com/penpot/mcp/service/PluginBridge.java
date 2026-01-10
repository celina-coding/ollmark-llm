package com.penpot.mcp.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.penpot.mcp.model.*;
import com.penpot.mcp.websocket.PluginWebSocketHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.*;
import java.util.*;
import java.util.concurrent.*;

/**
 * Service de pont pour la communication avec le plugin Penpot.
 * <p>
 * Ce service gère l'exécution asynchrone de tâches dans le plugin via WebSocket:
 * </p>
 * <ul>
 *   <li>Envoie des requêtes de tâches au plugin</li>
 *   <li>Attend les réponses avec timeout configurable</li>
 *   <li>Gère le mapping entre tâches et réponses via IDs uniques</li>
 *   <li>Support multi-utilisateur via tokens</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PluginBridge {

    /** Handler WebSocket pour la communication avec le plugin */
    private final PluginWebSocketHandler webSocketHandler;

    /** Mapper JSON pour la sérialisation/désérialisation */
    private final ObjectMapper objectMapper;

    /** Map des tâches en attente de réponse, indexées par ID de tâche */
    private final Map<String, CompletableFuture<PluginTaskResponse<?>>> pendingTasks = 
        new ConcurrentHashMap<>();

    /**
     * Exécute une tâche dans le plugin Penpot et attend la réponse.
     * <p>
     * Le processus est le suivant:
     * </p>
     * <ol>
     *   <li>Génère un ID unique pour la tâche</li>
     *   <li>Crée un CompletableFuture pour attendre la réponse</li>
     *   <li>Envoie la requête au plugin via WebSocket</li>
     *   <li>Attend la réponse avec un timeout configurable</li>
     *   <li>Vérifie le succès et retourne le résultat ou lance une exception</li>
     * </ol>
     *
     * @param <T> le type des données attendues en réponse
     * @param taskType le type de tâche à exécuter (ex: "executeCode")
     * @param params les paramètres de la tâche
     * @param userToken token optionnel pour identifier l'utilisateur (mode multi-utilisateur)
     * @param timeoutSeconds délai maximum d'attente en secondes
     * @return la réponse de la tâche avec ses données
     * @throws IllegalStateException si aucune connexion plugin n'est active
     * @throws RuntimeException si la tâche échoue
     * @throws TimeoutException si le délai d'attente est dépassé
     * @throws Exception pour toute autre erreur d'exécution
     */
    public <T> PluginTaskResponse<T> executeTask(
        String taskType, 
        Object params, 
        String userToken,
        int timeoutSeconds
    ) throws Exception {
        WebSocketSession session = findSessionForUser(userToken);
        if (session == null || !session.isOpen()) {
            throw new IllegalStateException("No active plugin connection found");
        }

        String taskId = UUID.randomUUID().toString();
        PluginTaskRequest request = PluginTaskRequest.builder()
            .id(taskId)
            .task(taskType)
            .params(params)
            .build();

        CompletableFuture<PluginTaskResponse<?>> future = new CompletableFuture<>();
        pendingTasks.put(taskId, future);

        try {
            String jsonRequest = objectMapper.writeValueAsString(request);
            log.info("Sending task to plugin via WebSocket session {}: {}", 
                session.getId(), jsonRequest);
            session.sendMessage(new TextMessage(jsonRequest));
            log.info("Task sent successfully, waiting for response...");

            PluginTaskResponse<?> response = future.get(timeoutSeconds, TimeUnit.SECONDS);
            log.info("Received response from plugin for task {}", taskId);
            if (!response.getSuccess()) {
                throw new RuntimeException("Task failed: " + response.getError());
            }

            return (PluginTaskResponse<T>) response;
        } catch (Exception e) {
            log.error("Task execution failed", e);
            throw e;
        } finally {
            pendingTasks.remove(taskId);
        }
    }

    /**
     * Traite une réponse de tâche reçue du plugin via WebSocket.
     * <p>
     * Cette méthode est appelée par {@link PluginWebSocketHandler} lorsqu'un
     * message de réponse est reçu. Elle résout le CompletableFuture associé
     * à la tâche, permettant à {@link #executeTask} de retourner.
     * </p>
     * <p>
     * La méthode effectue également une transformation spéciale pour les
     * réponses executeCode afin de structurer correctement les données.
     *
     * @param response la réponse reçue du plugin
     */
    public void handleTaskResponse(PluginTaskResponse<?> response) {
        String taskId = response.getId();
        log.info("Received task response for task {}: success={}", taskId, response.getSuccess());

        CompletableFuture<PluginTaskResponse<?>> future = pendingTasks.get(taskId);
        if (future != null) {
            if (response.getData() instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> dataMap = (Map<String, Object>) response.getData();

                Object result = dataMap.get("result");
                String log = (String) dataMap.get("log");

                ExecuteCodeTaskResultData<Object> properData = new ExecuteCodeTaskResultData<>();
                properData.setResult(result);
                properData.setLog(log);

                PluginTaskResponse<ExecuteCodeTaskResultData<Object>> typedResponse = 
                    new PluginTaskResponse<>();
                typedResponse.setId(response.getId());
                typedResponse.setSuccess(response.getSuccess());
                typedResponse.setError(response.getError());
                typedResponse.setData(properData);
                
                future.complete(typedResponse);
            } else {
                future.complete(response);
            }

            log.info("Task {} completed and future resolved", taskId);
        } else {
            log.warn("Received response for unknown task: {}", taskId);
        }
    }

    /**
     * Trouve la session WebSocket pour un utilisateur donné.
     * <p>
     * En mode mono-utilisateur (userToken null), retourne la première
     * session disponible.
     * </p>
     * <p>
     * En mode multi-utilisateur, recherche la session correspondant au token.
     *
     * @param userToken le token utilisateur ou null pour mono-utilisateur
     * @return la session WebSocket ou null si aucune session disponible
     */
    private WebSocketSession findSessionForUser(String userToken) {
        Map<String, WebSocketSession> sessions = webSocketHandler.getSessions();
        if (sessions.isEmpty()) return null;

        if (userToken != null) {
            for (Map.Entry<String, WebSocketSession> entry : sessions.entrySet()) {
                String sessionToken = webSocketHandler.getUserToken(entry.getKey());
                if (userToken.equals(sessionToken)) {
                    return entry.getValue();
                }
            }
        }

        return sessions.values().iterator().next();
    }

    /**
     * Vérifie s'il existe au moins une connexion plugin active.
     *
     * @return true si au moins une connexion est active, false sinon
     */
    public boolean hasActiveConnection() {
        return !webSocketHandler.getSessions().isEmpty();
    }
}