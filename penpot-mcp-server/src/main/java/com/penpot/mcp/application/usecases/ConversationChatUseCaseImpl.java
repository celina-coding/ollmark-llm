package com.penpot.mcp.application.usecases;

import com.penpot.mcp.core.ports.in.ConversationChatUseCase;
import com.penpot.mcp.core.ports.out.AiServicePort;
import com.penpot.mcp.shared.exception.ToolExecutionException;
import com.penpot.mcp.shared.util.ValidationUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import java.util.UUID;

/**
 * Implémentation du use case de chat conversationnel.
 * 
 * <h2>Responsabilité unique</h2>
 * Ce use case orchestre les conversations entre l'utilisateur et l'assistant IA
 * pour la génération de contenu marketing graphique sur Penpot.
 * 
 * <h2>Architecture</h2>
 * <ul>
 *     <li>Délègue toute l'intelligence artificielle à {@link AiServicePort}</li>
 *     <li>Ne contient aucune logique IA directement</li>
 *     <li>Gère uniquement la validation et la génération d'IDs de conversation</li>
 * </ul>
 * 
 * <h2>Gestion de la mémoire</h2>
 * La mémoire conversationnelle (ChatMemory) est entièrement gérée par
 * {@link com.penpot.mcp.adapters.out.ai.OllamaAiAdapter} :
 * <ul>
 *     <li>Chargement automatique de l'historique</li>
 *     <li>Sauvegarde automatique des messages</li>
 *     <li>Gestion de la fenêtre de messages</li>
 * </ul>
 * 
 * <h2>Workflow utilisateur</h2>
 * <pre>
 * 1. Utilisateur : "Crée un post Instagram pour ma boulangerie"
 *    ↓
 * 2. Use Case : Valide et transmet à AiServicePort
 *    ↓
 * 3. OllamaAiAdapter : 
 *    - Charge l'historique de la conversation
 *    - Invoque les tools RAG (searchTemplates)
 *    - Génère la réponse avec le template approprié
 *    - Sauvegarde dans l'historique
 *    ↓
 * 4. Retour utilisateur : "J'ai trouvé 3 templates..."
 * </pre>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ConversationChatUseCaseImpl implements ConversationChatUseCase {

    /**
     * Port vers le service d'IA.
     * Implémenté par {@link com.penpot.mcp.adapters.out.ai.OllamaAiAdapter}.
     */
    private final AiServicePort aiService;

    @Override
    public String chat(String conversationId, String message) {
        validateChatInput(conversationId, message);

        log.info("Processing chat request for conversation: {} (message length: {} chars)", 
            conversationId, message.length());

        try {
            String response = aiService.chat(conversationId, message);
            log.info("Chat completed successfully (response length: {} chars)", response.length());
            return response;
        } catch (Exception e) {
            log.error("Chat failed for conversation: {}", conversationId, e);
            if (e instanceof ToolExecutionException) throw e;
            throw new ToolExecutionException(
                "Failed to process chat for conversation " + conversationId + ": " + e.getMessage(), 
                e
            );
        }
    }

    @Override
    public String startNewConversation(String userId) {
        String conversationId = generateConversationId(userId);
        log.info("Started new conversation: {} (userId: {})", 
            conversationId, 
            userId != null ? userId : "anonymous");
        return conversationId;
    }

    @Override
    public void clearConversation(String conversationId) {
        ValidationUtils.requireNonBlank(conversationId, "Conversation ID");
        log.info("Clearing conversation history: {}", conversationId);

        try {
            // TODO: Implémenter via AiServicePort.clearConversation(conversationId)
            log.warn("Clear conversation not yet implemented - requires ChatMemory injection");
        } catch (Exception e) {
            log.error("Failed to clear conversation: {}", conversationId, e);
            throw new ToolExecutionException(
                "Failed to clear conversation " + conversationId + ": " + e.getMessage(), 
                e
            );
        }
    }

    /**
     * Génère un ID de conversation unique.
     * 
     * <h3>Format</h3>
     * <ul>
     *     <li>Avec userId : {@code user-{userId}-{uuid8}}</li>
     *     <li>Sans userId : {@code anonymous-{uuid8}}</li>
     * </ul>
     * 
     * <h3>Exemples</h3>
     * <ul>
     *     <li>{@code user-alice-a1b2c3d4}</li>
     *     <li>{@code user-bob-e5f6g7h8}</li>
     *     <li>{@code anonymous-i9j0k1l2}</li>
     * </ul>
     * 
     * @param userId ID de l'utilisateur (peut être null)
     * @return ID de conversation généré
     */
    private String generateConversationId(String userId) {
        String uuid8 = UUID.randomUUID().toString().substring(0, 8);
        if (userId != null && !userId.isBlank()) {
            return String.format("user-%s-%s", userId, uuid8);
        }
        return String.format("anonymous-%s", uuid8);
    }

    private void validateChatInput(String conversationId, String message) {
        ValidationUtils.requireNonBlank(conversationId, "Conversation ID");
        ValidationUtils.validateString(message, "Message", 10000);
    }
}