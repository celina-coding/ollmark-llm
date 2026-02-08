package com.penpot.ai.application.service;

import com.penpot.ai.model.PluginTaskResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import java.util.Map;
import java.util.concurrent.*;

/**
 * Coordonnateur pour gérer les réponses asynchrones des tâches.
 * Implémente un pattern Observer léger pour notifier les tâches en attente.
 * 
 * Architecture:
 * 1. Les use cases enregistrent une tâche avant l'envoi
 * 2. Un Future est créé et stocké en mémoire
 * 3. Le WebSocketHandler notifie quand une réponse arrive
 * 4. Le Future est complété et le use case reçoit la réponse
 * 
 * Gestion des erreurs:
 * - Timeout: géré par le use case via Future.get(timeout)
 * - Erreur de transport: notifiée via notifyError()
 * - Connexion perdue: cancelAllPendingTasks() annule tout
 */
@Slf4j
@Component
public class TaskOrchestrator {

    /**
     * Map des tâches en attente de réponse.
     * Key: Task ID, Value: Future à compléter
     * 
     * Thread-safe car ConcurrentHashMap
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
        if (taskId == null || taskId.isBlank()) {
            throw new IllegalArgumentException("Task ID cannot be null or empty");
        }

        CompletableFuture<PluginTaskResponse<?>> future = new CompletableFuture<>();

        CompletableFuture<PluginTaskResponse<?>> existing = pendingTasks.putIfAbsent(taskId, future);
        if (existing != null) {
            log.warn("Task {} was already registered - this may indicate a duplicate task ID", taskId);
            return existing;
        }

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
        if (taskId == null || taskId.isBlank()) return false;
        CompletableFuture<?> removed = pendingTasks.remove(taskId);

        if (removed != null) {
            log.debug("Unregistered task {} (total pending: {})", 
                taskId, pendingTasks.size());

            // Si le future n'est pas encore complété, le canceller
            if (!removed.isDone()) {
                log.debug("Cancelling incomplete future for task {}", taskId);
                removed.cancel(false);
            }

            return true;
        }

        log.debug("Attempted to unregister unknown task: {}", taskId);
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
        if (response == null) {
            log.warn("Received null response");
            return false;
        }

        String taskId = response.getId();
        if (taskId == null || taskId.isBlank()) {
            log.warn("Received response with null or empty task ID");
            return false;
        }

        CompletableFuture<PluginTaskResponse<?>> future = pendingTasks.get(taskId);

        if (future == null) {
            log.warn("Received response for unknown or expired task: {}", taskId);
            return false;
        }

        log.debug("Completing future for task {} (success: {})", 
            taskId, response.getSuccess());

        try {
            boolean completed = future.complete(response);
            if (!completed) log.warn("Future for task {} was already completed", taskId);

            return completed;
        } catch (Exception e) {
            log.error("Error completing future for task {}", taskId, e);
            return false;
        }
    }

    /**
     * Notifie une erreur pour une tâche.
     * 
     * @param taskId l'ID de la tâche
     * @param error l'erreur survenue
     * @return true si une tâche correspondante était en attente
     */
    public boolean notifyError(String taskId, Throwable error) {
        if (taskId == null || taskId.isBlank()) {
            log.warn("Attempted to notify error with null/empty task ID");
            return false;
        }

        CompletableFuture<PluginTaskResponse<?>> future = pendingTasks.get(taskId);

        if (future == null) {
            log.warn("Received error for unknown task {}: {}", taskId, error.getMessage());
            return false;
        }

        log.debug("Completing future with error for task {}: {}", 
            taskId, error.getMessage());

        try {
            boolean completed = future.completeExceptionally(error);

            if (!completed) {
                log.warn("Future for task {} was already completed when trying to set error", taskId);
            }

            return completed;
        } catch (Exception e) {
            log.error("Error completing future exceptionally for task {}", taskId, e);
            return false;
        }
    }

    /**
     * Annule toutes les tâches en attente.
     * Utile lors de l'arrêt du serveur ou d'une déconnexion.
     * 
     * @param reason la raison de l'annulation
     */
    public void cancelAllPendingTasks(String reason) {
        int count = pendingTasks.size();

        if (count == 0) {
            log.debug("No pending tasks to cancel");
            return;
        }

        log.info("Cancelling {} pending tasks: {}", count, reason);

        pendingTasks.forEach((taskId, future) -> {
            if (!future.isDone()) {
                log.debug("Cancelling task: {}", taskId);
                future.completeExceptionally(
                    new RuntimeException("Task cancelled: " + reason)
                );
            } else {
                log.debug("Task {} was already completed", taskId);
            }
        });

        pendingTasks.clear();
        log.info("All pending tasks cancelled and cleared");
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
        if (taskId == null || taskId.isBlank()) return false;
        return pendingTasks.containsKey(taskId);
    }

    /**
     * Nettoie les tâches expirées ou complétées.
     * Peut être appelé périodiquement pour libérer la mémoire.
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

        if (cleaned > 0) {
            log.debug("Cleaned up {} completed tasks (remaining: {})", 
                cleaned, pendingTasks.size());
        }

        return cleaned;
    }

    /**
     * Obtient des statistiques sur les tâches en attente.
     * Utile pour monitoring et debugging.
     * 
     * @return map avec les statistiques
     */
    public Map<String, Object> getStatistics() {
        int total = pendingTasks.size();
        long completed = pendingTasks.values().stream()
            .filter(CompletableFuture::isDone)
            .count();
        long pending = total - completed;

        return Map.of(
            "total", total,
            "pending", pending,
            "completed", completed
        );
    }
}