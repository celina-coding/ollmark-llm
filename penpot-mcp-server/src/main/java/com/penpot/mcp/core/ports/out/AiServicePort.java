package com.penpot.mcp.core.ports.out;

import com.penpot.mcp.core.domain.AiContext;
import org.springframework.ai.chat.messages.Message;
import java.util.List;

/**
 * Port de sortie pour les services d'intelligence artificielle.
 * Abstraction suivant le principe d'inversion de dépendances (DIP).
 * Permet de découpler le domaine de l'implémentation AI concrète (Ollama, OpenAI, etc.).
 */
public interface AiServicePort {

    /**
     * Engage une conversation avec l'assistant AI.
     * Maintient le contexte conversationnel en incluant l'historique.
     * 
     * @param userMessage le message de l'utilisateur
     * @param conversationHistory l'historique des messages précédents (peut être null)
     * @return la réponse de l'assistant AI
     */
    String chat(String userMessage, List<Message> conversationHistory);

    /**
     * Génère du code JavaScript exécutable via AI pour accomplir une tâche.
     * Le code généré est prêt à être exécuté directement dans le contexte
     * du plugin Penpot sans modification.
     * 
     * @param context le contexte enrichi contenant la tâche, documentation, exemples, etc.
     * @return le code JavaScript exécutable généré
     */
    String generateCode(AiContext context);

    /**
     * Obtient la documentation d'un type ou membre API spécifique.
     * 
     * @param typeName le nom du type API à consulter
     * @param memberName le nom du membre optionnel (peut être null)
     * @return la documentation formatée en texte/markdown
     */
    String getApiTypeInfo(String typeName, String memberName);

    /**
     * Obtient une vue d'ensemble de l'API Penpot.
     * 
     * @return la documentation d'ensemble de l'API
     */
    String getPenpotOverview();
}