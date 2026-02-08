package com.penpot.ai.adapters.out.plugin;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.penpot.ai.core.domain.*;
import com.penpot.ai.core.ports.out.PluginCommunicationPort;
import com.penpot.ai.infrastructure.session.SessionManager;
import com.penpot.ai.application.service.TaskOrchestrator;
import com.penpot.ai.model.*;
import com.penpot.ai.shared.exception.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.*;
import java.util.*;
import java.util.concurrent.*;

/**
 * Adapter pour la communication avec le plugin Penpot via WebSocket.
 * 
 * Architecture:
 * 1. Vérifie qu'une session WebSocket est disponible
 * 2. Enregistre la tâche auprès du TaskOrchestrator
 * 3. Envoie la requête via WebSocket
 * 4. Attend la réponse via CompletableFuture
 * 5. Gère les timeouts et erreurs
 * 
 * Thread Safety:
 * - Les opérations WebSocket sont thread-safe
 * - TaskOrchestrator utilise ConcurrentHashMap
 * - Les futures sont thread-safe
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PluginBridgeAdapter implements PluginCommunicationPort {

    private final SessionManager sessionManager;
    private final ObjectMapper objectMapper;
    private final TaskOrchestrator responseOrchestrator;

    @Override
    public <T> PluginTaskResponse<T> sendTask(Task task) {
        log.info("Sending task {} to plugin (type: {})", 
            task.getId(), task.getType());

        SessionCriteria criteria = buildCriteria(task);
        WebSocketSession session = sessionManager.findSession(criteria)
            .orElseThrow(() -> new PluginConnectionException(
                "No active plugin session found for criteria: " + criteria
            ));

        PluginTaskRequest request = buildRequest(task);
        CompletableFuture<PluginTaskResponse<?>> future = 
            responseOrchestrator.registerTask(task.getId());

        try {
            sendWebSocketMessage(session, request);
            log.debug("Task {} sent successfully, waiting for response...", task.getId());

            PluginTaskResponse<?> response = future.get(
                10000,
                TimeUnit.SECONDS
            );

            log.info("Received response for task {}: success={}, hasData={}", 
                task.getId(), 
                response.getSuccess(),
                response.getData() != null);

            return (PluginTaskResponse<T>) response;
        } catch (TimeoutException e) {
            future.cancel(true);

            throw new TaskExecutionException(
                String.format("Task %s timed out after %d seconds", 
                    task.getId(), 100000),
                e
            );
        } catch (InterruptedException e) {
            log.error("Task {} was interrupted", task.getId());
            Thread.currentThread().interrupt();

            throw new TaskExecutionException(
                "Task " + task.getId() + " was interrupted", 
                e
            );
        } catch (ExecutionException e) {
            Throwable cause = e.getCause();
            log.error("Task {} execution failed: {}", 
                task.getId(), 
                cause != null ? cause.getMessage() : "Unknown error");

            throw new TaskExecutionException(
                "Task " + task.getId() + " execution failed: " + 
                    (cause != null ? cause.getMessage() : "Unknown error"),
                cause != null ? cause : e
            );
        } catch (Exception e) {
            log.error("Unexpected error sending task {}", task.getId(), e);

            throw new TaskExecutionException(
                "Failed to send task " + task.getId() + ": " + e.getMessage(),
                e
            );
        } finally {
            responseOrchestrator.unregisterTask(task.getId());
        }
    }

    /**
     * Envoie un message WebSocket de manière sécurisée.
     * 
     * @param session la session WebSocket
     * @param request la requête à envoyer
     * @throws TaskExecutionException si l'envoi échoue
     */
    private void sendWebSocketMessage(WebSocketSession session, PluginTaskRequest request) {
        try {
            if (!session.isOpen()) {
                throw new PluginConnectionException(
                    "WebSocket session " + session.getId() + " is not open"
                );
            }

            String jsonRequest = objectMapper.writeValueAsString(request);
            log.debug("Sending WebSocket message: {}", jsonRequest);

            session.sendMessage(new TextMessage(jsonRequest));
        } catch (Exception e) {
            log.error("Failed to send WebSocket message for task {}", request.getId(), e);
            throw new TaskExecutionException(
                "Failed to send WebSocket message: " + e.getMessage(),
                e
            );
        }
    }

    @Override
    public boolean hasActiveConnection() {
        boolean hasActive = sessionManager.hasActiveSessions();
        log.debug("Has active connection: {}", hasActive);
        return hasActive;
    }

    @Override
    public Optional<WebSocketSession> findSession(SessionCriteria criteria) {
        return sessionManager.findSession(criteria);
    }

    /**
     * Construit les critères de recherche de session depuis la tâche.
     */
    private SessionCriteria buildCriteria(Task task) {
        return task.getUserToken()
            .map(SessionCriteria::forUser)
            .orElseGet(SessionCriteria::any);
    }

    /**
     * Construit la requête plugin depuis la tâche du domaine.
     */
    private PluginTaskRequest buildRequest(Task task) {
        return PluginTaskRequest.builder()
            .id(task.getId())
            .task(task.getType().getTaskName())
            .params(task.getParameters())
            .build();
    }
}