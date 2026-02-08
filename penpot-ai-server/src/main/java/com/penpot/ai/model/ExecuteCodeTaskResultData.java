package com.penpot.ai.model;

import lombok.*;

/**
 * Données de résultat pour une tâche d'exécution de code.
 * Contient le résultat de l'exécution du code ainsi que les logs
 * éventuels générés pendant l'exécution.
 *
 * @param <T> le type du résultat retourné par l'exécution
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExecuteCodeTaskResultData<T> {

    /** Le résultat de l'exécution du code */
    private T result;

    /** Les logs de console générés pendant l'exécution */
    private String log;
}