package com.penpot.mcp.core.ports.in;

import com.penpot.mcp.core.domain.TaskResult;
import com.penpot.mcp.core.domain.ExecuteCodeCommand;

/**
 * Port d'entrée pour l'exécution de code JavaScript dans le plugin Penpot.
 * Interface du use case suivant le principe de ségrégation des interfaces (ISP).
 */
public interface ExecuteCodeUseCase {
    /**
     * Exécute du code JavaScript dans le contexte du plugin Penpot.
     * 
     * @param command la commande d'exécution contenant le code et les paramètres
     * @return le résultat de l'exécution
     * @throws PluginConnectionException si aucune connexion plugin n'est active
     * @throws TaskExecutionException si l'exécution échoue
     */
    TaskResult execute(ExecuteCodeCommand command);
}