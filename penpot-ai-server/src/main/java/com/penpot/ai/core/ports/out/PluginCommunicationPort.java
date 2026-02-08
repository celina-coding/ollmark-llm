package com.penpot.ai.core.ports.out;

import com.penpot.ai.core.domain.*;
import com.penpot.ai.model.PluginTaskResponse;
import org.springframework.web.socket.WebSocketSession;
import java.util.Optional;

/**
 * Port de sortie pour la communication avec le plugin Penpot.
 * Abstraction suivant le principe d'inversion de dépendances (DIP).
 */
public interface PluginCommunicationPort {

    /**
     * Envoie une tâche au plugin et attend la réponse.
     * 
     * @param task la tâche à exécuter
     * @param <T> type des données de réponse attendues
     * @return la réponse de la tâche
     * @throws TaskExecutionException si l'exécution échoue
     * @throws TimeoutException si le délai est dépassé
     */
    <T> PluginTaskResponse<T> sendTask(Task task);

    /**
     * Vérifie si au moins une connexion plugin est active.
     * 
     * @return true si une connexion est disponible
     */
    boolean hasActiveConnection();

    /**
     * Trouve une session WebSocket selon des critères.
     * 
     * @param criteria critères de recherche (token utilisateur, etc.)
     * @return la session trouvée, ou Optional.empty()
     */
    Optional<WebSocketSession> findSession(SessionCriteria criteria);
}