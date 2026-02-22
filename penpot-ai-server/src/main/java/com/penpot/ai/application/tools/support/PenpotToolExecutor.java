package com.penpot.ai.application.tools.support;

import com.penpot.ai.core.domain.*;
import com.penpot.ai.core.ports.in.ExecuteCodeUseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.function.Function;

/**
 * Service d'exécution centralisé pour tous les Tools Penpot.
 *
 * <p><b>DRY :</b> élimine la duplication du pattern
 * {@code try → executeCode → check success → format result} présent dans chaque tool.</p>
 *
 * <p><b>SRP :</b> responsabilité unique — exécuter du code Penpot et mapper le résultat.</p>
 *
 * <p><b>OCP :</b> extensible via {@link ExecuteCodeCommand#execute(String, String, Function)} sans modification
 * de cette classe. Les méthodes de convenance couvrent les cas standard.</p>
 *
 * <p><b>DIP :</b> dépend de l'abstraction {@link ExecuteCodeUseCase}, jamais d'une implémentation.</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PenpotToolExecutor {

    private final ExecuteCodeUseCase executeCodeUseCase;

    // ==================== POINT D'ENTRÉE GÉNÉRIQUE (OCP) ====================

    /**
     * Exécute du code Penpot et applique un mapper personnalisé sur le résultat.
     * Point d'extension pour les cas non couverts par les méthodes de convenance.
     *
     * @param code          code JavaScript Penpot à exécuter
     * @param operationName nom lisible pour les logs
     * @param resultMapper  fonction de conversion {@link TaskResult} → JSON String
     * @return réponse JSON formatée
     */
    public String execute(String code, String operationName, Function<TaskResult, String> resultMapper) {
        try {
            TaskResult result = executeCodeUseCase.execute(ExecuteCodeCommand.of(code));
            if (!result.isSuccess()) {
                return ToolResponseBuilder.error(result.getError().orElse("Unknown error"));
            }
            return resultMapper.apply(result);
        } catch (Exception e) {
            log.error("Failed to execute Penpot operation: {}", operationName, e);
            return ToolResponseBuilder.error(e.getMessage());
        }
    }

    // ==================== MÉTHODES DE CONVENANCE ====================

    /**
     * Exécute la création d'une forme et retourne le format "SHAPE_ID: xxx".
     *
     * @param code      code JS retournant l'ID de la forme créée
     * @param shapeType type lisible (rectangle, ellipse, board...)
     */
    public String createShape(String code, String shapeType) {
        log.info("Creating shape: {}", shapeType);
        return execute(code, "create " + shapeType, result -> {
            String shapeId = result.getData().map(Object::toString).orElse("unknown");
            log.debug("Created {} → ID: {}", shapeType, shapeId);
            return ToolResponseBuilder.shapeCreated(shapeType, shapeId);
        });
    }

    /**
     * Exécute une transformation géométrique (rotate, scale, move, resize).
     * Tente d'extraire l'ID réel depuis la réponse ; utilise {@code expectedShapeId} en fallback.
     *
     * @param code            code JS retournant {@code { id: "..." }}
     * @param operation       nom de l'opération (rotated, scaled...)
     * @param expectedShapeId ID attendu (fallback si non retourné)
     */
    public String transformShape(String code, String operation, String expectedShapeId) {
        log.info("Transforming shape: {} ({})", expectedShapeId, operation);
        return execute(code, operation, result -> {
            String actualId = extractSingleId(result, "id", expectedShapeId);
            log.info("Successfully {} shape: {}", operation, actualId);
            return ToolResponseBuilder.shapeOperation(operation, actualId,
                String.format("Shape %s successfully", operation));
        });
    }

    /**
     * Applique un style ou un asset à une forme (fill, gradient, stroke, shadow, opacity...).
     *
     * @param code      code JS de modification de style
     * @param operation nom de l'opération (fillApplied, gradientApplied...)
     * @param shapeId   ID de la forme
     * @param details   description des paramètres appliqués
     */
    public String applyStyle(String code, String operation, String shapeId, String details) {
        log.info("Applying style {} to shape: {}", operation, shapeId);
        return execute(code, operation, result -> {
            log.info("Successfully applied {} to: {}", operation, shapeId);
            return ToolResponseBuilder.shapeOperation(operation, shapeId, details);
        });
    }

    /**
     * Exécute une opération sur plusieurs formes et retourne la liste des IDs affectés.
     * S'attend à ce que le code retourne {@code { ids: ["..."] }}.
     *
     * @param code           code JS retournant {@code { ids: string[] }}
     * @param operationLabel libellé de l'opération (aligned, distributed...)
     */
    public String executeMultiShape(String code, String operationLabel) {
        log.info("Executing multi-shape operation: {}", operationLabel);
        return execute(code, operationLabel, result -> {
            List<String> ids = extractIdList(result, "ids");
            log.info("Successfully {} {} shapes", operationLabel, ids.size());
            return ToolResponseBuilder.multiShapeOperation(operationLabel, ids);
        });
    }

    /**
     * Exécute la création d'un groupe.
     * S'attend à ce que le code retourne {@code { groupId: "..." }}.
     */
    public String createGroup(String code) {
        return execute(code, "group shapes", result -> {
            String groupId = extractSingleId(result, "groupId", "unknown");
            log.info("Created group: {}", groupId);
            return ToolResponseBuilder.groupCreated(groupId);
        });
    }

    /**
     * Exécute le clonage d'une forme.
     * S'attend à ce que le code retourne {@code { cloneId: "..." }}.
     */
    public String cloneShape(String code) {
        return execute(code, "clone shape", result -> {
            String cloneId = extractSingleId(result, "cloneId", "unknown");
            return ToolResponseBuilder.shapeCloned(cloneId);
        });
    }

    /**
     * Exécute une opération de suppression et retourne un message de confirmation.
     */
    public String executeDelete(String code, String operationLabel) {
        return execute(code, operationLabel, result ->
            ToolResponseBuilder.success(
                result.getData().map(Object::toString).orElse("Operation completed")
            )
        );
    }

    /**
     * Exécute la création de contenu (title, subtitle, paragraph, image).
     * S'attend à ce que le code retourne l'ID directement.
     */
    public String createContent(String code, String contentType) {
        return execute(code, "create " + contentType, result -> {
            String shapeId = result.getData().map(Object::toString).orElse("unknown");
            return ToolResponseBuilder.contentCreated(contentType, shapeId);
        });
    }

    // ==================== EXTRACTEURS PRIVÉS ====================

    /** Extrait un ID unique depuis un {@code Map} retourné dans le TaskResult. */
    private String extractSingleId(TaskResult result, String key, String fallback) {
        return result.getData()
            .filter(d -> d instanceof Map<?, ?>)
            .map(d -> {
                Object id = ((Map<?, ?>) d).get(key);
                return id != null ? id.toString() : fallback;
            })
            .orElse(fallback);
    }

    /** Extrait une liste d'IDs depuis un {@code Map} retourné dans le TaskResult. */
    @SuppressWarnings("unchecked")
    private List<String> extractIdList(TaskResult result, String key) {
        return result.getData()
            .filter(d -> d instanceof Map<?, ?>)
            .map(d -> {
                Object list = ((Map<?, ?>) d).get(key);
                return (list instanceof List<?>) ? (List<String>) list : List.<String>of();
            })
            .orElse(List.of());
    }
}