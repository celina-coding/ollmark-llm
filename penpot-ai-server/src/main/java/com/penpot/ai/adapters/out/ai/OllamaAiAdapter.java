package com.penpot.ai.adapters.out.ai;

import com.penpot.ai.core.domain.AiContext;
import com.penpot.ai.core.ports.out.AiServicePort;
import com.penpot.ai.application.service.PromptsConfigService;
import com.penpot.ai.application.tools.*;
import com.penpot.ai.shared.exception.ToolExecutionException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import static org.springframework.ai.chat.memory.ChatMemory.CONVERSATION_ID;

/**
 * Adaptateur sortant centralisé vers le service d'IA conversationnelle Ollama.
 * 
 * <h2>Responsabilités uniques</h2>
 * Cet adaptateur est le SEUL point d'accès à l'IA dans l'application :
 * <ul>
 *     <li>Chat conversationnel avec mémoire persistée (ChatMemory)</li>
 *     <li>Génération de code JavaScript pour Penpot</li>
 *     <li>Gestion de l'historique conversationnel</li>
 *     <li>Intégration des tools RAG (recherche de templates marketing)</li>
 * </ul>
 *
 * @see AiServicePort Port de sortie implémenté
 * @see TemplateSearchTools Tools RAG disponibles pour l'IA
 * @see PromptsConfigService Configuration des prompts système
 */
@Slf4j
@Component
@Primary
@RequiredArgsConstructor
public class OllamaAiAdapter implements AiServicePort {

    /** Client Spring AI configuré avec ChatMemory et advisors. */
    private final ChatClient chatClient;

    /** Mémoire de conversation persistée (ChatMemory). */
    private final ChatMemory chatMemory;

    /** Service centralisant les prompts système et la configuration IA. */
    private final PromptsConfigService promptsConfigService;

    /** Tools IA pour la recherche de templates marketing (RAG). */
    private final TemplateSearchTools templateSearchTools;

    /** Tools Penpot pour la création de formes. */
    private final PenpotShapeTools penpotShapeTools;

    /** Tools Penpot pour les transformations géométriques. */
    private final PenpotTransformTools penpotTransformTools;

    /** Tools Penpot pour l'alignement et la distribution. */
    private final PenpotLayoutTools penpotLayoutTools;

    /** Tools Penpot pour la gestion des assets et styles. */
    private final PenpotAssetTools penpotAssetTools;

    /** Tools Penpot pour la gestion du contenu. */
    private final PenpotContentTools penpotContentTools;

    @Override
    public String chat(String conversationId, String userMessage) {
        try {
            log.info(
                "Processing chat request (conversation: {}, message length: {} chars)", 
                conversationId, userMessage.length()
            );

            String systemPrompt = buildChatSystemPrompt();

            String response = chatClient.prompt()
                .system(systemPrompt)
                .user(userMessage)
                .advisors(advisor -> advisor.param(CONVERSATION_ID, conversationId))
                .tools(
                    templateSearchTools,
                    penpotShapeTools,
                    penpotTransformTools,
                    penpotLayoutTools,
                    penpotAssetTools,
                    penpotContentTools
                )
                .call()
                .content();

            log.info("Chat response generated (length: {} chars)", response.length());

            return response;
        } catch (Exception e) {
            log.error("Error during AI chat for conversation: {}", conversationId, e);
            throw new ToolExecutionException(
                "AI chat service error for conversation " + conversationId + ": " + e.getMessage(), 
                e
            );
        }
    }

    @Override
    public void clearConversation(String conversationId) {
        if (conversationId == null || conversationId.isBlank()) {
            throw new IllegalArgumentException("Conversation ID cannot be null or empty");
        }

        try {
            log.info("Clearing conversation history for: {}", conversationId);

            int messageCountBefore = chatMemory.get(conversationId).size();
            chatMemory.clear(conversationId);

            log.info("Successfully cleared conversation {} ({} messages removed)", 
                conversationId, messageCountBefore);
        } catch (Exception e) {
            log.error("Failed to clear conversation: {}", conversationId, e);
            throw new ToolExecutionException(
                "Failed to clear conversation " + conversationId + ": " + e.getMessage(),
                e
            );
        }
    }

    /**
     * Construit le prompt système pour le chat conversationnel.
     * Délègue entièrement la configuration à prompts.yml.
     * 
     * @return prompt système depuis prompts.yml
     */
    private String buildChatSystemPrompt() {
        return promptsConfigService.getInitialInstructions();
    }

    /**
     * Construit le prompt système pour la génération de code.
     *
     * @param context contexte (utilisé uniquement pour le user prompt)
     * @return prompt système depuis prompts.yml
     */
    private String buildCodeGenerationSystemPrompt(AiContext context) {
        return promptsConfigService.getInitialInstructions();
    }

    /**
     * Construit le message utilisateur pour la génération de code.
     *
     * @param context contexte contenant la tâche et le contexte utilisateur
     * @return message utilisateur formaté
     */
    private String buildCodeGenerationUserPrompt(AiContext context) {
        return context.buildUserPrompt();
    }
}