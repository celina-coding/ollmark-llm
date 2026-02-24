package com.penpot.ai.core.ports.out;

/**
 * Port de sortie pour les services d'intelligence artificielle.
 * 
 * <h2>Responsabilités</h2>
 * <ul>
 *     <li>Chat conversationnel avec mémoire persistée</li>
 *     <li>Génération de code JavaScript pour Penpot</li>
 * </ul>
 * 
 * <h2>Implémentation</h2>
 * Implémenté par {@link com.penpot.ai.adapters.out.ai.OllamaAiAdapter}
 * qui centralise toute la logique IA de l'application.
 * 
 * @see com.penpot.ai.adapters.out.ai.OllamaAiAdapter Implémentation avec Ollama
 */
public interface AiServicePort {

    /**
     * Engage une conversation avec l'assistant IA avec gestion automatique
     * de la mémoire conversationnelle.
     * 
     * @param conversationId identifiant unique de la conversation
     * @param userMessage    message envoyé par l'utilisateur
     * @return réponse textuelle générée par l'IA
     * @throws RuntimeException si l'appel IA échoue
     */
    String chat(String conversationId, String userMessage, String userToken);

    /**
     * Efface complètement l'historique d'une conversation.
     * 
     * <h3>Comportement</h3>
     * <ul>
     *     <li>Supprime tous les messages de la conversation dans ChatMemory</li>
     *     <li>La conversation peut continuer après avec un contexte vierge</li>
     *     <li>L'ID de conversation reste valide</li>
     * </ul>
     * 
     * <h3>Cas d'usage</h3>
     * <ul>
     *     <li>L'utilisateur veut recommencer à zéro</li>
     *     <li>Changement de contexte ou de sujet</li>
     *     <li>Nettoyage des données personnelles</li>
     * </ul>
     * 
     * @param conversationId identifiant de la conversation à effacer
     * @throws IllegalArgumentException si l'ID est null ou vide
     * @throws RuntimeException si la suppression échoue
     */
    void clearConversation(String conversationId);
}