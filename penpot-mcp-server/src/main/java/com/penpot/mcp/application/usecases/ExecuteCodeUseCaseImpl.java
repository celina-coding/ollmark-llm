package com.penpot.mcp.application.usecases;

import com.penpot.mcp.core.domain.*;
import com.penpot.mcp.core.ports.in.ExecuteCodeUseCase;
import com.penpot.mcp.core.ports.out.PluginCommunicationPort;
import com.penpot.mcp.infrastructure.factory.*;
import com.penpot.mcp.model.*;
import com.penpot.mcp.shared.exception.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import java.util.*;

/**
 * Implémentation du use case d'exécution de code.
 * 
 * <h2>Responsabilités</h2>
 * <ul>
 *     <li>Validation de la commande d'exécution</li>
 *     <li>Vérification de la connexion plugin</li>
 *     <li>Création et envoi de la tâche au plugin</li>
 *     <li>Conversion de la réponse en résultat métier</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ExecuteCodeUseCaseImpl implements ExecuteCodeUseCase {

    private final PluginCommunicationPort pluginPort;
    private final ResultFormatterFactory formatterFactory;
    private final TaskFactory taskFactory;

    @Override
    public TaskResult execute(ExecuteCodeCommand command) {
        log.info("Executing code use case (code length: {} chars)", 
            command.getCode().length());

        ensurePluginConnected();

        Task task = taskFactory.createExecuteCodeTask(
            command.getCode(),
            command.getUserToken().orElse(null)
        );

        log.debug("Created task with ID: {}", task.getId());

        try {
            PluginTaskResponse<?> response = pluginPort.sendTask(task);
            return convertResponse(response);
        } catch (TaskExecutionException e) {
            log.error("Task {} execution failed", task.getId(), e);
            throw e;
        } catch (Exception e) {
            log.error("Unexpected error during task {} execution", task.getId(), e);
            throw new TaskExecutionException(
                "Unexpected error during code execution: " + e.getMessage(), 
                e
            );
        }
    }

    /**
     * Vérifie qu'une connexion plugin est disponible.
     * 
     * @throws PluginConnectionException si aucune connexion n'est active
     */
    private void ensurePluginConnected() {
        if (!pluginPort.hasActiveConnection()) {
            throw new PluginConnectionException(
                "No active Penpot plugin connection. " +
                "Please ensure the plugin is loaded and connected."
            );
        }
    }

    /**
     * Convertit une PluginTaskResponse en TaskResult du domaine.
     * Applique le formatage approprié selon le type de données.
     * 
     * @param response la réponse du plugin
     * @return le résultat converti
     */
    private TaskResult convertResponse(PluginTaskResponse<?> response) {
        if (!response.getSuccess()) {
            String errorMsg = response.getError() != null 
                ? response.getError() 
                : "Task failed without error message";

            log.warn("Task failed: {}", errorMsg);
            return TaskResult.failure(errorMsg);
        }

        Object data = response.getData();
        if (data == null) {
            log.debug("Task succeeded with no data");
            return TaskResult.success(null);
        }

        try {
            var formatter = formatterFactory.getFormatterForObject(data);
            String formattedData = formatter.format(data);

            log.debug("Formatted result using: {}", formatter.getClass().getSimpleName());

            var logs = extractLogs(data);
            return logs.isEmpty()
                ? TaskResult.success(formattedData)
                : TaskResult.success(formattedData, logs);
        } catch (Exception e) {
            log.error("Error formatting task result", e);
            throw new FormattingException(
                "Failed to format task result: " + e.getMessage(), 
                e
            );
        }
    }

    /**
     * Extrait les logs des données de réponse si présents.
     * 
     * @param data les données de réponse
     * @return la liste des logs ou une liste vide
     */
    private List<String> extractLogs(Object data) {
        // Cas 1 : ExecuteCodeTaskResultData
        if (data instanceof ExecuteCodeTaskResultData<?> resultData) {
            String log = resultData.getLog();
            return log != null && !log.isBlank()
                ? List.of(log)
                : List.of();
        }

        // Cas 2 : Map contenant "log"
        if (data instanceof Map<?, ?> map) {
            Object logValue = map.get("log");
            if (logValue instanceof String log && !log.isBlank()) {
                return List.of(log);
            }
        }

        return List.of();
    }
}