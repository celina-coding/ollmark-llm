package com.penpot.mcp.infrastructure.factory;

import com.penpot.mcp.core.domain.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import java.util.*;

/**
 * Factory pour créer des objets Task du domaine.
 * Encapsule la logique de création et applique des validations.
 * Suit le Factory Pattern pour centraliser la création d'objets complexes.
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
     * Crée une tâche de récupération de structure.
     * 
     * @param shapeId l'ID de la forme à analyser
     * @param userToken token utilisateur optionnel
     * @return la tâche créée
     */
    public Task createFetchStructureTask(String shapeId, String userToken) {
        validateShapeId(shapeId);

        String taskId = generateTaskId();
        log.debug("Creating fetch structure task with ID: {}", taskId);

        return Task.builder()
            .id(taskId)
            .type(TaskType.FETCH_STRUCTURE)
            .parameters(Map.of("shapeId", shapeId))
            .userToken(java.util.Optional.ofNullable(userToken))
            .build();
    }

    /**
     * Crée une tâche de modification de forme.
     * 
     * @param shapeId l'ID de la forme à modifier
     * @param modifications les modifications à appliquer
     * @param userToken token utilisateur optionnel
     * @return la tâche créée
     */
    public Task createModifyShapeTask(
        String shapeId, 
        Map<String, Object> modifications,
        String userToken
    ) {
        validateShapeId(shapeId);
        validateModifications(modifications);

        String taskId = generateTaskId();
        log.debug("Creating modify shape task with ID: {}", taskId);

        Map<String, Object> params = Map.of(
            "shapeId", shapeId,
            "modifications", modifications
        );

        return Task.builder()
            .id(taskId)
            .type(TaskType.MODIFY_SHAPE)
            .parameters(params)
            .userToken(java.util.Optional.ofNullable(userToken))
            .build();
    }

    /**
     * Crée une tâche de création d'élément.
     * 
     * @param elementType le type d'élément à créer
     * @param properties les propriétés de l'élément
     * @param userToken token utilisateur optionnel
     * @return la tâche créée
     */
    public Task createElementTask(
        String elementType,
        Map<String, Object> properties,
        String userToken
    ) {
        validateElementType(elementType);

        String taskId = generateTaskId();
        log.debug("Creating create element task with ID: {} (type: {})", 
            taskId, elementType);

        Map<String, Object> params = Map.of(
            "elementType", elementType,
            "properties", properties != null ? properties : Map.of()
        );

        return Task.builder()
            .id(taskId)
            .type(TaskType.CREATE_ELEMENT)
            .parameters(params)
            .userToken(Optional.ofNullable(userToken))
            .build();
    }

    /**
     * Crée une tâche générique à partir d'un type et de paramètres.
     * 
     * @param taskType le type de tâche
     * @param parameters les paramètres de la tâche
     * @param userToken token utilisateur optionnel
     * @return la tâche créée
     */
    public Task createTask(
        TaskType taskType,
        Map<String, Object> parameters,
        String userToken
    ) {
        String taskId = generateTaskId();
        log.debug("Creating generic task with ID: {} (type: {})", 
            taskId, taskType);

        return Task.builder()
            .id(taskId)
            .type(taskType)
            .parameters(parameters != null ? parameters : Map.of())
            .userToken(Optional.ofNullable(userToken))
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
            throw new IllegalArgumentException(
                "Code cannot be null or empty"
            );
        }

        if (code.length() > 100_000) {
            throw new IllegalArgumentException(
                "Code is too long (max 100,000 characters)"
            );
        }
    }

    /**
     * Valide un ID de forme.
     * 
     * @param shapeId l'ID à valider
     * @throws IllegalArgumentException si l'ID est invalide
     */
    private void validateShapeId(String shapeId) {
        if (shapeId == null || shapeId.isBlank()) {
            throw new IllegalArgumentException(
                "Shape ID cannot be null or empty"
            );
        }
    }

    /**
     * Valide les modifications.
     * 
     * @param modifications les modifications à valider
     * @throws IllegalArgumentException si les modifications sont invalides
     */
    private void validateModifications(Map<String, Object> modifications) {
        if (modifications == null || modifications.isEmpty()) {
            throw new IllegalArgumentException(
                "Modifications cannot be null or empty"
            );
        }
    }

    /**
     * Valide le type d'élément.
     * 
     * @param elementType le type à valider
     * @throws IllegalArgumentException si le type est invalide
     */
    private void validateElementType(String elementType) {
        if (elementType == null || elementType.isBlank()) {
            throw new IllegalArgumentException(
                "Element type cannot be null or empty"
            );
        }

        Set<String> validTypes = java.util.Set.of(
            "rectangle", "ellipse", "text", "board", "group", "path"
        );

        if (!validTypes.contains(elementType.toLowerCase())) {
            throw new IllegalArgumentException(
                "Unsupported element type: " + elementType + 
                ". Valid types: " + validTypes
            );
        }
    }
}