package com.penpot.ai.core.ports.in;

import com.penpot.ai.core.domain.*;

/**
 * Port d'entrée pour l'exécution de code JavaScript dans le plugin Penpot.
 */
public interface ExecuteCodeUseCase {

    /**
     * Exécute une commande de code JavaScript dans le plugin Penpot.
     * 
     * <h3>Workflow</h3>
     * <ol>
     *     <li>Validation de la commande</li>
     *     <li>Vérification de la connexion plugin</li>
     *     <li>Création de la tâche</li>
     *     <li>Envoi au plugin via WebSocket</li>
     *     <li>Attente de la réponse (avec timeout)</li>
     *     <li>Conversion et formatage du résultat</li>
     * </ol>
     * 
     * @param command commande contenant le code et les paramètres
     * @return résultat de l'exécution
     * @throws PluginConnectionException si aucune connexion plugin n'est active
     * @throws TaskExecutionException    si l'exécution échoue
     */
    TaskResult execute(ExecuteCodeCommand command);
}