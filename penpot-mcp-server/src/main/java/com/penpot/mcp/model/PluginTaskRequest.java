package com.penpot.mcp.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;
import java.util.UUID;

/**
 * Requête de tâche envoyée au plugin Penpot via WebSocket.
 * <p>
 * Représente une instruction à exécuter dans le plugin, identifiée
 * par un ID unique pour le suivi de la réponse asynchrone.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PluginTaskRequest {
    /** Identifiant unique de la tâche */
    private String id;

    /** Type de tâche à exécuter (ex: "executeCode") */
    private String task;

    /** Paramètres spécifiques à la tâche */
    private Object params;

    /**
     * Crée une nouvelle requête de tâche avec un ID généré automatiquement.
     *
     * @param task le type de tâche à exécuter
     * @param params les paramètres de la tâche
     * @return une nouvelle requête de tâche
     */
    public static PluginTaskRequest create(String task, Object params) {
        return PluginTaskRequest.builder()
                .id(UUID.randomUUID().toString())
                .task(task)
                .params(params)
                .build();
    }
}