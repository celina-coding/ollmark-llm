package com.penpot.ai.application.tools.support;

import com.penpot.ai.shared.util.JsonUtils;
import lombok.experimental.UtilityClass;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Constructeur centralisé des réponses JSON pour tous les Tools Penpot.
 */
@UtilityClass
public class ToolResponseBuilder {

    /** Réponse d'erreur générique. */
    public static String error(String message) {
        return String.format("{\"success\": false, \"error\": %s}", JsonUtils.escapeJson(message));
    }

    /** Réponse succès avec message texte libre. */
    public static String success(String message) {
        return String.format("{\"success\": true, \"message\": %s}", JsonUtils.escapeJson(message));
    }

    /**
     * Réponse pour la création d'une forme.
     * Format optimisé pour que l'IA mémorise l'UUID retourné.
     */
    public static String shapeCreated(String shapeType, String shapeId) {
        return String.format(
            "%s created successfully!\n\nSHAPE_ID: %s\n\n"
            + "SAVE THIS ID! Use it in subsequent operations like:\n"
            + "- alignShapes(shapeIds=\"%s,...\", alignment=\"top\")\n"
            + "- rotateShape(shapeId=\"%s\", angle=45)\n"
            + "- moveShape(shapeId=\"%s\", newX=200, newY=300)",
            shapeType, shapeId, shapeId, shapeId, shapeId
        );
    }

    /**
     * Réponse pour la création de contenu textuel ou image.
     */
    public static String contentCreated(String contentType, String shapeId) {
        return String.format(
            "{\"success\": true, \"type\": %s, \"id\": %s}",
            JsonUtils.escapeJson(contentType),
            JsonUtils.escapeJson(shapeId)
        );
    }

    /**
     * Réponse pour une transformation ou application de style (rotate, fill, shadow...).
     */
    public static String shapeOperation(String operation, String shapeId, String details) {
        return String.format(Locale.US,
            "{\"success\": true, \"operation\": %s, \"shapeId\": %s, \"details\": %s}",
            JsonUtils.escapeJson(operation),
            JsonUtils.escapeJson(shapeId),
            JsonUtils.escapeJson(details)
        );
    }

    /**
     * Réponse pour une opération sur plusieurs formes (align, distribute, z-order...).
     */
    public static String multiShapeOperation(String operation, List<String> ids) {
        String idList = ids.stream()
            .map(id -> "  - " + id)
            .collect(Collectors.joining("\n"));

        return String.format(
            "Successfully %s %d shapes.\n\nShape IDs:\n%s\n\n"
            + "TIP: These shapes have been %s. You can continue working with them.",
            operation, ids.size(), idList, operation
        );
    }

    /** Réponse pour la création d'un groupe. */
    public static String groupCreated(String groupId) {
        return String.format(
            "Successfully created group.\n\nGroup ID: %s\n\n"
            + "TIP: You can now work with the entire group as a single shape.",
            groupId
        );
    }

    /** Réponse pour le clonage d'une forme. */
    public static String shapeCloned(String cloneId) {
        return String.format(
            "Successfully cloned shape.\n\nClone ID: %s\n\n"
            + "TIP: Save this ID to manipulate the clone.",
            cloneId
        );
    }
}