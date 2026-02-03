package com.penpot.mcp.adapters.out.plugin;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.penpot.mcp.core.domain.*;
import com.penpot.mcp.core.ports.out.PluginCommunicationPort;
import com.penpot.mcp.infrastructure.session.SessionManager;
import com.penpot.mcp.application.service.TaskOrchestrator;
import com.penpot.mcp.model.*;
import com.penpot.mcp.shared.exception.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.*;
import java.util.*;
import java.util.concurrent.*;

/**
 * Adapter pour la communication avec le plugin Penpot via WebSocket.
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
        log.info("Sending task {} to plugin)", 
            task.getId());

        SessionCriteria criteria = buildCriteria(task);
        WebSocketSession session = sessionManager.findSession(criteria)
            .orElseThrow(() -> new PluginConnectionException(
                "No active plugin session found for criteria: " + criteria
            ));

        PluginTaskRequest request = buildRequest(task);
        CompletableFuture<PluginTaskResponse<?>> future = 
            responseOrchestrator.registerTask(task.getId());

        try {
            String jsonRequest = objectMapper.writeValueAsString(request);
            session.sendMessage(new TextMessage(jsonRequest));
            log.debug("Task {} sent successfully", task.getId());

            PluginTaskResponse<?> response = future.get(
                1000000,
                TimeUnit.SECONDS
            );

            log.info("Received response for task {}: success={}", 
                task.getId(), response.getSuccess());

            return (PluginTaskResponse<T>) response;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new TaskExecutionException("Task interrupted", e);
        } catch (ExecutionException e) {
            throw new TaskExecutionException(
                "Task execution failed: " + e.getCause().getMessage(),
                e.getCause()
            );
        } catch (Exception e) {
            throw new TaskExecutionException(
                "Failed to send task: " + e.getMessage(),
                e
            );
        } finally {
            responseOrchestrator.unregisterTask(task.getId());
        }
    }

    @Override
    public boolean hasActiveConnection() {
        return sessionManager.hasActiveSessions();
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