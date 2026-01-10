package com.penpot.mcp.model;

import lombok.*;

/**
 * Paramètres pour une tâche d'exécution de code dans le plugin.
 * <p>
 * Encapsule le code JavaScript qui sera envoyé au plugin Penpot
 * pour exécution via WebSocket.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExecuteCodeTaskParams {
    /** Le code JavaScript à exécuter dans le plugin */
    private String code;
}