package com.penpot.ai.core.ports.in;

/**
 * Port d'entrée pour le chat avec gestion de conversation.
 */
public interface ConversationChatUseCase {

    /**
     * Engage une conversation avec l'assistant IA.
     * 
     * <h3>Délégation complète</h3>
     * Ce use case délègue entièrement la logique conversationnelle à
     * {@link AiServicePort#chat(String, String)}, qui :
     * <ul>
     *     <li>Récupère automatiquement l'historique via ChatMemory</li>
     *     <li>Détecte les intentions marketing et invoque les tools RAG</li>
     *     <li>Génère des réponses contextuelles</li>
     *     <li>Sauvegarde automatiquement le message et la réponse</li>
     * </ul>
     * 
     * @param conversationId ID unique de la conversation (ex: "user-alice-abc123")
     * @param message        message de l'utilisateur
     * @param userToken
     * @return réponse générée par l'IA
     * @throws IllegalArgumentException si les paramètres sont invalides
     * @throws RuntimeException         si l'appel IA échoue
     */
    String chat(String conversationId, String message, String userToken);

    /**
     * Démarre une nouvelle conversation.
     * 
     * <h3>Génération d'ID</h3>
     * Génère un ID unique au format :
     * <ul>
     *     <li>Avec userId : {@code user-{userId}-{uuid8}}</li>
     *     <li>Sans userId : {@code anonymous-{uuid8}}</li>
     * </ul>
     * 
     * <h3>Pas d'initialisation de mémoire</h3>
     * Aucune initialisation explicite de ChatMemory n'est nécessaire.
     * La première fois que cet ID sera utilisé dans {@link #chat(String, String)},
     * ChatMemory créera automatiquement une nouvelle entrée.
     * 
     * @param userId ID de l'utilisateur (peut être null pour anonyme)
     * @return ID de conversation généré
     */
    String startNewConversation(String userId);

    /**
     * Efface l'historique d'une conversation.
     * 
     * <h3>Suppression définitive</h3>
     * Supprime tous les messages de l'historique de cette conversation
     * dans ChatMemory (base de données H2).
     * 
     * <h3>Impact</h3>
     * Après cet appel :
     * <ul>
     *     <li>L'IA n'aura plus accès aux messages précédents</li>
     *     <li>La conversation peut continuer avec un contexte vierge</li>
     *     <li>L'ID de conversation reste valide</li>
     * </ul>
     * 
     * @param conversationId ID de la conversation à effacer
     * @throws IllegalArgumentException si l'ID est invalide
     */
    void clearConversation(String conversationId);
}