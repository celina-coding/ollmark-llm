package com.penpot.mcp.core.ports.out;

import com.penpot.mcp.core.domain.AiContext;

/**
 * Port de sortie pour les services d'intelligence artificielle.
 * 
 * Ce port abstrait les services IA et permet de découpler le domaine
 * de l'implémentation concrète (Ollama, OpenAI, Claude, etc.).
 * 
 * <h2>Responsabilités</h2>
 * <ul>
 *     <li>Chat conversationnel avec mémoire persistée</li>
 *     <li>Génération de code JavaScript pour Penpot</li>
 *     <li>Accès à la documentation API Penpot</li>
 *     <li>Intégration de tools (function calling) pour RAG</li>
 * </ul>
 * 
 * <h2>Implémentation</h2>
 * Implémenté par {@link com.penpot.mcp.adapters.out.ai.OllamaAiAdapter}
 * qui centralise toute la logique IA de l'application.
 * 
 * @see com.penpot.mcp.adapters.out.ai.OllamaAiAdapter Implémentation avec Ollama
 */
public interface AiServicePort {

    /**
     * Engage une conversation avec l'assistant IA avec gestion automatique
     * de la mémoire conversationnelle.
     * 
     * <h3>Gestion automatique de la mémoire</h3>
     * L'implémentation doit :
     * <ul>
     *     <li>Charger automatiquement l'historique via {@code conversationId}</li>
     *     <li>Injecter l'historique dans le contexte du prompt</li>
     *     <li>Sauvegarder automatiquement le message utilisateur</li>
     *     <li>Sauvegarder automatiquement la réponse générée</li>
     * </ul>
     * 
     * <h3>Function Calling / Tools</h3>
     * L'IA doit avoir accès à des tools (function calling) pour :
     * <ul>
     *     <li>Rechercher des templates marketing (RAG)</li>
     *     <li>Générer du code depuis des templates</li>
     *     <li>Lister les catégories de templates</li>
     *     <li>Filtrer par type ou tag</li>
     * </ul>
     * 
     * <h3>Exemples d'utilisation</h3>
     * <pre>
     * // Première interaction
     * String response1 = aiService.chat("user-alice-abc", "Crée un post Instagram");
     * // → L'IA invoque searchTemplates("instagram post")
     * // → Retourne : "J'ai trouvé 3 templates Instagram..."
     * 
     * // Deuxième interaction (avec contexte)
     * String response2 = aiService.chat("user-alice-abc", "Utilise le premier");
     * // → L'IA se souvient du contexte précédent
     * // → Invoque generateFromTemplate(templateId)
     * // → Retourne le code JavaScript généré
     * </pre>
     * 
     * @param conversationId identifiant unique de la conversation
     * @param userMessage    message envoyé par l'utilisateur
     * @return réponse textuelle générée par l'IA
     * @throws RuntimeException si l'appel IA échoue
     */
    String chat(String conversationId, String userMessage);

    /**
     * Génère du code JavaScript exécutable pour accomplir une tâche dans Penpot.
     * 
     * <h3>Contexte enrichi</h3>
     * Le contexte fourni contient :
     * <ul>
     *     <li>La tâche à accomplir (description textuelle)</li>
     *     <li>Documentation API pertinente (types, méthodes)</li>
     *     <li>Exemples de code similaires</li>
     *     <li>Best practices à suivre</li>
     *     <li>Contraintes techniques</li>
     * </ul>
     * 
     * <h3>Code généré</h3>
     * Le code retourné doit être :
     * <ul>
     *     <li>Exécutable directement (pas de markdown)</li>
     *     <li>Sans require() ou import</li>
     *     <li>Utilisant uniquement les objets globaux (penpot, penpotUtils, storage)</li>
     *     <li>Nettoyé des commentaires superflus</li>
     * </ul>
     * 
     * <h3>Tools RAG disponibles</h3>
     * L'IA peut invoquer des tools pour rechercher des templates
     * si la tâche correspond à un cas d'usage marketing.
     * 
     * @param context contexte enrichi avec documentation, exemples, etc.
     * @return code JavaScript prêt à être exécuté dans Penpot
     * @throws RuntimeException si la génération échoue
     */
    String generateCode(AiContext context);

    /**
     * Obtient la documentation d'un type ou membre API Penpot spécifique.
     * 
     * <h3>Utilisation</h3>
     * Permet à l'IA d'accéder à la documentation de l'API Penpot
     * pour générer du code plus précis.
     * 
     * @param typeName   nom du type API (ex: "Shape", "Board", "Text")
     * @param memberName nom du membre optionnel (propriété ou méthode, peut être null)
     * @return documentation formatée en texte/markdown
     */
    String getApiTypeInfo(String typeName, String memberName);

    /**
     * Obtient une vue d'ensemble de l'API Penpot.
     * 
     * <h3>Contenu</h3>
     * Vue générale incluant :
     * <ul>
     *     <li>Liste des types disponibles</li>
     *     <li>Descriptions courtes de chaque type</li>
     *     <li>Organisation de l'API</li>
     * </ul>
     * 
     * @return documentation générale de l'API
     */
    String getPenpotOverview();
}