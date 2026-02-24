package com.penpot.ai.infrastructure.factory;

import com.penpot.ai.core.domain.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import java.util.*;

/**
 * Factory pour créer des objets Task du domaine.
 * Encapsule la logique de création et applique des validations.
 */
@Slf4j
@Component
public class TaskFactory {

    /**
     * Crée une tâche d'exécution de code.
     * 
     * @param code le code JavaScript à exécuter
     * @param userToken token utilisateur optionnel
     * @return la tâche créée avec un ID unique
     */
    public Task createExecuteCodeTask(String code, String userToken) {
        validateCode(code);

        String taskId = generateTaskId();
        log.debug("Creating execute code task with ID: {}", taskId);

        return Task.builder()
            .id(taskId)
            .type(TaskType.EXECUTE_CODE)
            .parameters(Map.of("code", code))
            .userToken(java.util.Optional.ofNullable(userToken))
            .build();
    }

    /**
     * Génère un ID unique pour une tâche.
     * 
     * @return l'ID généré
     */
    private String generateTaskId() {
        return UUID.randomUUID().toString();
    }

    /**
     * Valide le code JavaScript.
     * 
     * @param code le code à valider
     * @throws IllegalArgumentException si le code est invalide
     */
    private void validateCode(String code) {
        if (code == null || code.isBlank()) {
            throw new IllegalArgumentException("Code cannot be null or empty");
        }

        if (code.length() > 100_000) {
            throw new IllegalArgumentException("Code is too long (max 100,000 characters)");
        }
    }
}