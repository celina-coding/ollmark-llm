package com.penpot.ai.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;

/**
 * Réponse d'une tâche reçue du plugin Penpot via WebSocket.
 * Contient le résultat de l'exécution d'une tâche, avec indication
 * de succès ou d'échec et les données retournées.
 *
 * @param <T> le type des données retournées
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PluginTaskResponse<T> {

    /** Identifiant de la tâche correspondante */
    private String id;

    /** Indique si la tâche s'est exécutée avec succès */
    private Boolean success;

    /** Message d'erreur en cas d'échec */
    private String error;

    /** Données retournées par la tâche */
    private T data;
}