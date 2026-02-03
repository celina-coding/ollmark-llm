package com.penpot.mcp.adapters.out.ai;

import com.penpot.mcp.core.domain.AiContext;
import com.penpot.mcp.core.ports.out.AiServicePort;
import com.penpot.mcp.application.service.PromptsConfigService;
import com.penpot.mcp.application.tools.TemplateSearchTools;
import com.penpot.mcp.shared.exception.ToolExecutionException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.*;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;
import java.util.*;

import static org.springframework.ai.chat.memory.ChatMemory.CONVERSATION_ID;

/**
 * Adaptateur sortant centralisé vers le service d'IA conversationnelle Ollama.
 * 
 * <h2>Responsabilités uniques</h2>
 * Cet adaptateur est le SEUL point d'accès à l'IA dans l'application :
 * <ul>
 *     <li>Chat conversationnel avec mémoire persistée (ChatMemory)</li>
 *     <li>Génération de code JavaScript pour Penpot</li>
 *     <li>Intégration des tools RAG (recherche de templates marketing)</li>
 * </ul>
 *
 * <h2>Gestion de la mémoire conversationnelle</h2>
 * Utilise {@link org.springframework.ai.chat.memory.ChatMemory} via les advisors Spring AI :
 * <ul>
 *     <li>Chargement automatique de l'historique via {@code conversationId}</li>
 *     <li>Injection dans le contexte du prompt</li>
 *     <li>Sauvegarde automatique des messages utilisateur et réponses IA</li>
 * </ul>
 *
 * <h2>Function Calling avec Templates</h2>
 * Expose les {@link TemplateSearchTools} à l'IA pour permettre la recherche
 * et la génération de templates marketing directement depuis la conversation.
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

    /** 
     * Client Spring AI configuré avec ChatMemory et advisors.
     * Injecté depuis {@link com.penpot.mcp.infrastructure.config.OllamaConfig}
     */
    private final ChatClient chatClient;

    /** Service centralisant les prompts système et la configuration IA. */
    private final PromptsConfigService promptsConfigService;

    /** Tools IA pour la recherche de templates marketing (RAG). */
    private final TemplateSearchTools templateSearchTools;

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
                .tools(templateSearchTools)
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
    public String generateCode(AiContext context) {
        try {
            log.info("Generating code for task: {}", context.getTask());

            String systemPrompt = buildCodeGenerationSystemPrompt(context);
            String userPrompt = buildCodeGenerationUserPrompt(context);

            List<Message> messages = List.of(
                new SystemMessage(systemPrompt),
                new UserMessage(userPrompt)
            );

            Prompt prompt = new Prompt(messages);
            ChatResponse response = chatClient.prompt(prompt)
                    .tools(templateSearchTools)
                    .call()
                    .chatResponse();

            String code = response.getResult().getOutput().getText();
            code = cleanGeneratedCode(code);

            log.info("Code generated successfully (length: {} chars)", code.length());
            log.debug("Generated code preview: {}", 
                code.length() > 100 ? code.substring(0, 100) + "..." : code);

            return code;
        } catch (Exception e) {
            log.error("Error during code generation for task: {}", context.getTask(), e);
            throw new ToolExecutionException(
                "Code generation failed: " + e.getMessage(), 
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

    /**
     * Nettoie le code généré par l'IA.
     * Supprime les artefacts markdown, commentaires inutiles et espaces superflus.
     *
     * @param code code brut généré par l'IA
     * @return code nettoyé prêt à l'exécution
     */
    private String cleanGeneratedCode(String code) {
        if (code == null || code.isBlank()) return "";

        code = code.replaceAll("^```(?:javascript|js)?\\s*", "");
        code = code.replaceAll("```\\s*$", "");
        code = code.replaceAll("//[^\n]*", "");
        code = code.replaceAll("/\\*.*?\\*/", "");
        code = code.replaceAll("\\n\\s*\\n", "\n");
        code = code.trim();

        return code;
    }
}