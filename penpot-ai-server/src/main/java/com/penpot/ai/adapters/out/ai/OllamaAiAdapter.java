package com.penpot.ai.adapters.out.ai;

import com.penpot.ai.application.router.ToolCategoryResolver;
import com.penpot.ai.application.service.PromptsConfigService;
import com.penpot.ai.core.domain.*;
import com.penpot.ai.core.ports.out.*;
import com.penpot.ai.infrastructure.config.OllamaConfig.ChatClientFactory;
import com.penpot.ai.shared.exception.ToolExecutionException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import com.penpot.ai.application.advisor.ReReadingAdvisor;
import com.penpot.ai.application.advisor.InspectionFirstAdvisor;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.rag.advisor.RetrievalAugmentationAdvisor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.util.Set;

import static org.springframework.ai.chat.memory.ChatMemory.CONVERSATION_ID;

/**
 * Adaptateur sortant centralisé vers le service d'IA conversationnelle Ollama.
 *
 * <h2>Pipeline complet avec router</h2>
 * <pre>
 * userMessage
 *     │
 *     ├─ (1) RequestComplexityAnalyzer  →  TaskComplexity
 *     │                                    (SIMPLE / CREATIVE / COMPLEX)
 *     │
 *     ├─ (2) ToolRouterPort.route()     →  Set&lt;ToolCategory&gt;
 *     │       phi3:mini                     ex: {COLOR_AND_STYLE, INSPECTION}
 *     │
 *     ├─ (3) ToolCategoryResolver       →  Object[] tools  (sous-ensemble filtré)
 *     │       PenpotToolRegistry            ex: [assetTools, inspectorTools]
 *     │
 *     └─ (4) ChatClientFactory          →  ChatClient  (options selon complexité)
 *             qwen3:8b                       + RAG advisor + Memory advisor
 *                                            + tools filtrés
 *                                            → String response
 * </pre>
 * 
 * @see ToolRouterPort       Port de routing phi3:mini
 * @see ToolCategoryResolver Registry de résolution catégorie → tools
 * @see ChatClientFactory    Factory de complexité qwen3:8b
 * @see RequestComplexityAnalyzer Analyseur de complexité
 */
@Slf4j
@Component
@Primary
public class OllamaAiAdapter implements AiServicePort {

    /** Client exécuteur. */
    private final ChatClient executorChatClient;

    /** Factory pour adapter les options selon la complexité détectée. */
    private final ChatClientFactory chatClientFactory;

    /** Analyseur de complexité des requêtes. */
    private final RequestComplexityAnalyzer complexityAnalyzer;

    /** Mémoire de conversation persistée. */
    private final ChatMemory chatMemory;

    /** Service de configuration des prompts système. */
    private final PromptsConfigService promptsConfigService;

    /** RAG Modulaire : advisor complet avec rewrite + multi-query + retrieval. */
    private final RetrievalAugmentationAdvisor retrievalAugmentationAdvisor;

    /** Inspection First Advisor */
    private final InspectionFirstAdvisor inspectionFirstAdvisor;

    // ==================== ROUTER ====================

    /** Port de routing : analyse l'intention et retourne les catégories de tools. */
    private final ToolRouterPort toolRouter;

    /**
     * Résolveur : convertit un {@link Set}&lt;{@link ToolCategory}&gt; en tableau
     * d'instances de tools Spring AI.
     */
    private final ToolCategoryResolver toolCategoryResolver;

    // ==================== CONSTRUCTEUR ====================

    public OllamaAiAdapter(
        @Qualifier("executorChatClient") ChatClient executorChatClient,
        ChatClientFactory chatClientFactory,
        RequestComplexityAnalyzer complexityAnalyzer,
        ChatMemory chatMemory,
        PromptsConfigService promptsConfigService,
        RetrievalAugmentationAdvisor retrievalAugmentationAdvisor,
        ToolRouterPort toolRouter,
        ToolCategoryResolver toolCategoryResolver,
        InspectionFirstAdvisor inspectionFirstAdvisor

    ) {
        this.executorChatClient = executorChatClient;
        this.chatClientFactory = chatClientFactory;
        this.complexityAnalyzer = complexityAnalyzer;
        this.chatMemory = chatMemory;
        this.promptsConfigService = promptsConfigService;
        this.retrievalAugmentationAdvisor = retrievalAugmentationAdvisor;
        this.toolRouter = toolRouter;
        this.toolCategoryResolver = toolCategoryResolver;
        this.inspectionFirstAdvisor = inspectionFirstAdvisor;
    }

    @Override
    public String chat(String conversationId, String userMessage) {
        try {
            // Étape 1 : Complexité
            TaskComplexity complexity = complexityAnalyzer.analyze(userMessage);
            log.info("Processing chat (conversation={}, complexity={}, messageLength={})",
                conversationId, complexity, userMessage.length());

            // Étape 2 : Router
            Set<ToolCategory> categories = toolRouter.route(userMessage);
            log.info("[Router] → categories: {}", categories);

            // Étape 3 : Résolution des tools
            Object[] tools = toolCategoryResolver.resolveTools(categories);
            log.info("[Registry] → {} tool instance(s) selected", tools.length);

            // Étape 4 : Exécuteur
            ChatClient adaptedClient = chatClientFactory.buildForComplexity(complexity);
            String response = adaptedClient.prompt()
                .system(promptsConfigService.getInitialInstructions())
                .user(userMessage)
                .advisors(
                    inspectionFirstAdvisor,
                    retrievalAugmentationAdvisor,
                    new ReReadingAdvisor(),
                    new SimpleLoggerAdvisor()
                )
                .advisors(advisor -> advisor
                .param(CONVERSATION_ID, conversationId)
                .param(InspectionFirstAdvisor.CTX_TOOL_CATEGORIES,
                    categories.stream().map(Enum::name).toList()))
                .tools(tools)
                .call()
                .content();

            // Thinking mode (COMPLEX uniquement)
            logThinkingIfPresent(adaptedClient, userMessage, conversationId);

            log.info("Chat response generated (length={} chars)", response.length());
            return response;
        } catch (Exception e) {
            log.error("Error during AI chat for conversation: {}", conversationId, e);
            throw new ToolExecutionException(
                "AI chat service error for conversation " + conversationId + ": " + e.getMessage(),
                e
            );
        }
    }

    /**
     * Génère un plan de design structuré ({@link DesignPlan}) à partir d'une requête.
     *
     * <p>Utilise toujours le profil COMPLEX (thinking activé) et le
     * {@code StructuredOutputValidationAdvisor} avec 3 tentatives pour garantir
     * un JSON valide conforme au schéma {@link DesignPlan}.</p>
     *
     * <p>Note : le planning n'utilise <b>pas</b> le router — il expose volontairement
     * tous les tools via le RAG advisor pour que le modèle puisse planifier
     * une séquence complète d'opérations.</p>
     *
     * @param conversationId identifiant de la conversation
     * @param userMessage    requête de design à planifier
     * @return le plan structuré, ou un plan {@code explain} en cas d'échec
     */
    public DesignPlan planDesign(String conversationId, String userMessage) {
        try {
            log.info("Planning design for conversation={}", conversationId);

            var validationAdvisor = org.springframework.ai.chat.client.advisor
                .StructuredOutputValidationAdvisor.builder()
                .outputType(DesignPlan.class)
                .maxRepeatAttempts(3)
                .build();

            ChatClient planningClient = chatClientFactory.buildForComplexity(TaskComplexity.COMPLEX);
            DesignPlan plan = planningClient.prompt()
                .system(buildPlanningSystemPrompt())
                .user(userMessage)
                .advisors(
                    validationAdvisor,
                    retrievalAugmentationAdvisor,
                    new SimpleLoggerAdvisor()
                )
                .advisors(advisor -> advisor.param(CONVERSATION_ID, conversationId))
                .call()
                .entity(DesignPlan.class);

            if (plan == null) {
                log.warn("planDesign returned null — falling back to explain plan");
                return DesignPlan.explain("Unable to generate a design plan. Please try rephrasing.");
            }

            log.info("Design plan generated: action={}, shapes={}, complexity={}",
                plan.action(),
                plan.hasShapes() ? plan.shapes().size() : 0,
                plan.complexity());
            return plan;
        } catch (Exception e) {
            log.error("Error during design planning for conversation={}", conversationId, e);
            return DesignPlan.explain(
                "Design planning failed: " + e.getMessage() + ". Please try again."
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
            int countBefore = chatMemory.get(conversationId).size();
            chatMemory.clear(conversationId);
            log.info("Cleared conversation {} ({} messages removed)", conversationId, countBefore);
        } catch (Exception e) {
            log.error("Failed to clear conversation: {}", conversationId, e);
            throw new ToolExecutionException(
                "Failed to clear conversation " + conversationId + ": " + e.getMessage(), e
            );
        }
    }

    /**
     * Construit le prompt système pour la génération de plans de design.
     * Ajoute les instructions JSON au prompt de base de prompts.yml.
     */
    private String buildPlanningSystemPrompt() {
        return promptsConfigService.getInitialInstructions() + """

            ## PLANNING MODE
            You are in PLANNING mode. You must respond ONLY with a valid JSON object
            matching the DesignPlan schema. No explanation, no markdown, no text outside the JSON.

            The JSON must contain:
            - "action": one of [create_design, modify_element, search_template, explain]
            - "complexity": one of [simple, creative, complex]
            - "template_id": RAG template ID or null
            - "shapes": ordered array of shape instructions with tool name and parameters
            - "global_parameters": board dimensions, colors, typography
            - "execution_order": human-readable steps list
            - "user_facing_message": confirmation message for the user

            Each shape in "shapes" must have:
            - "tool": exact tool name (createBoard, createRectangle, createText, etc.)
            - "name": descriptive name
            - "parameters": tool parameters object
            - "depends_on": list of shape names this depends on (can be empty)
            """;
    }

    /**
     * Tente de logger le contenu de thinking si le modèle l'a généré.
     *
     * <p>Le thinking est uniquement produit en mode COMPLEX avec des modèles
     * compatibles (qwen3, deepseek-r1). L'appel est protégé par un try/catch
     * pour ne jamais impacter le flux principal.</p>
     */
    private void logThinkingIfPresent(ChatClient client, String userMessage, String conversationId) {
        try {
            TaskComplexity complexity = complexityAnalyzer.analyze(userMessage);
            if (complexity != TaskComplexity.COMPLEX) return;

            var chatResponse = chatClientFactory.buildForComplexity(TaskComplexity.COMPLEX)
                .prompt()
                .user("summarize previous thinking in one sentence")
                .advisors(advisor -> advisor.param(CONVERSATION_ID, conversationId))
                .call()
                .chatResponse();

            if (chatResponse != null && chatResponse.getResult() != null) {
                Object thinking = chatResponse.getResult().getMetadata().get("thinking");
                if (thinking != null && !thinking.toString().isBlank()) {
                    log.debug("=== MODEL THINKING ===\n{}\n=== END THINKING ===", thinking);
                }
            }
        } catch (Exception e) {
            log.trace("Could not retrieve thinking metadata (non-fatal): {}", e.getMessage());
        }
    }
}