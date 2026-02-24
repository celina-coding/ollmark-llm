package com.penpot.ai.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;

/**
 * Requête de tâche envoyée au plugin Penpot via WebSocket.
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
}