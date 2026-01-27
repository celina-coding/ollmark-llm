package com.penpot.mcp.core.ports.in;

import org.springframework.ai.chat.messages.Message;
import java.util.List;

/**
 * Port d'entrée pour le chat avec l'assistant AI.
 * Use case suivant le principe de ségrégation des interfaces (ISP).
 */
public interface ChatUseCase {

    /**
     * Engage une conversation avec l'assistant AI.
     * 
     * @param message le message de l'utilisateur
     * @param conversationHistory l'historique des messages précédents
     * @return la réponse de l'assistant
     */
    String chat(String message, List<Message> conversationHistory);
}