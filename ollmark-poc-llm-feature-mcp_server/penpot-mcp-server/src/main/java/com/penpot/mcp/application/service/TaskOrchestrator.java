package com.penpot.mcp.application.service;

import com.penpot.mcp.model.PluginTaskResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import java.util.Map;
import java.util.concurrent.*;

/**
 * Coordonnateur pour gérer les réponses asynchrones des tâches.
 * Implémente un pattern Observer léger pour notifier les tâches en attente.
 */
@Slf4j
@Component
public class TaskOrchestrator {

    /**
     * Map des tâches en attente de réponse.
     * Key: Task ID, Value: Future à compléter
     */
    private final Map<String, CompletableFuture<PluginTaskResponse<?>>> pendingTasks = 
        new ConcurrentHashMap<>();

    /**
     * Enregistre une tâche en attente de réponse.
     * 
     * @param taskId l'ID de la tâche
     * @return le future qui sera complété à réception de la réponse
     */
    public CompletableFuture<PluginTaskResponse<?>> registerTask(String taskId) {
        CompletableFuture<PluginTaskResponse<?>> future = new CompletableFuture<>();
        pendingTasks.put(taskId, future);

        log.debug("Registered task {} for response tracking (total pending: {})", 
            taskId, pendingTasks.size());

        return future;
    }

    /**
     * Désenregistre une tâche (après réception ou timeout).
     * 
     * @param taskId l'ID de la tâche
     * @return true si la tâche était enregistrée, false sinon
     */
    public boolean unregisterTask(String taskId) {
        CompletableFuture<?> removed = pendingTasks.remove(taskId);

        if (removed != null) {
            log.debug("Unregistered task {} (total pending: {})", 
                taskId, pendingTasks.size());
            return true;
        }

        log.warn("Attempted to unregister unknown task: {}", taskId);
        return false;
    }

    /**
     * Notifie la réception d'une réponse.
     * Appelé par le WebSocketHandler quand une réponse arrive.
     * 
     * @param response la réponse reçue du plugin
     * @return true si une tâche correspondante était en attente
     */
    public boolean notifyResponse(PluginTaskResponse<?> response) {
        String taskId = response.getId();
        CompletableFuture<PluginTaskResponse<?>> future = pendingTasks.get(taskId);

        if (future != null) {
            log.debug("Completing future for task {} (success: {})", 
                taskId, response.getSuccess());

            future.complete(response);
            return true;
        }

        log.warn("Received response for unknown or expired task: {}", taskId);
        return false;
    }

    /**
     * Notifie une erreur pour une tâche.
     * 
     * @param taskId l'ID de la tâche
     * @param error l'erreur survenue
     * @return true si une tâche correspondante était en attente
     */
    public boolean notifyError(String taskId, Throwable error) {
        CompletableFuture<PluginTaskResponse<?>> future = pendingTasks.get(taskId);

        if (future != null) {
            log.debug("Completing future with error for task {}: {}", 
                taskId, error.getMessage());

            future.completeExceptionally(error);
            return true;
        }

        log.warn("Received error for unknown task {}: {}", taskId, error.getMessage());
        return false;
    }

    /**
     * Annule toutes les tâches en attente.
     * Utile lors de l'arrêt du serveur ou d'une déconnexion.
     * 
     * @param reason la raison de l'annulation
     */
    public void cancelAllPendingTasks(String reason) {
        log.info("Cancelling {} pending tasks: {}", pendingTasks.size(), reason);

        pendingTasks.forEach((taskId, future) -> {
            if (!future.isDone()) {
                log.debug("Cancelling task: {}", taskId);
                future.completeExceptionally(
                    new RuntimeException("Task cancelled: " + reason)
                );
            }
        });

        pendingTasks.clear();
        log.info("All pending tasks cancelled");
    }

    /**
     * Retourne le nombre de tâches en attente.
     * 
     * @return le nombre de tâches
     */
    public int getPendingTaskCount() {
        return pendingTasks.size();
    }

    /**
     * Vérifie si une tâche est en attente.
     * 
     * @param taskId l'ID de la tâche
     * @return true si la tâche est en attente
     */
    public boolean isTaskPending(String taskId) {
        return pendingTasks.containsKey(taskId);
    }

    /**
     * Nettoie les tâches expirées ou complétées.
     * 
     * @return le nombre de tâches nettoyées
     */
    public int cleanupCompletedTasks() {
        int cleaned = 0;

        for (Map.Entry<String, CompletableFuture<PluginTaskResponse<?>>> entry : 
             pendingTasks.entrySet()) {

            if (entry.getValue().isDone()) {
                pendingTasks.remove(entry.getKey());
                cleaned++;
            }
        }

        if (cleaned > 0) log.debug("Cleaned up {} completed tasks", cleaned);
        return cleaned;
    }
}