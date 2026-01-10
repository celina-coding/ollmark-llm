package com.penpot.mcp.model;

import lombok.*;

/**
 * Conteneur générique pour le résultat d'une tâche plugin.
 * <p>
 * Structure simple encapsulant les données retournées par une tâche.
 *
 * @param <T> le type des données retournées
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PluginTaskResult<T> {
    /** Les données du résultat */
    private T data;
}