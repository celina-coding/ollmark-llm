package com.penpot.ai.adapters.out.ai;

import com.penpot.ai.core.domain.*;
import com.penpot.ai.core.ports.out.AiServicePort;
import com.penpot.ai.application.service.PromptsConfigService;
import com.penpot.ai.application.tools.*;
import com.penpot.ai.infrastructure.config.OllamaConfig.ChatClientFactory;
import com.penpot.ai.shared.exception.ToolExecutionException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.rag.advisor.RetrievalAugmentationAdvisor;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import static org.springframework.ai.chat.memory.ChatMemory.CONVERSATION_ID;

/**
 * Adaptateur sortant centralisé vers le service d'IA conversationnelle Ollama.
 *
 * <h2>Fonctionnalités</h2>
 * <ul>
 *     <li><b>Complexité dynamique</b> : détecte automatiquement si la requête est
 *         simple / créative / complexe et configure les options Ollama en conséquence
 *         via {@link RequestComplexityAnalyzer} + {@link ChatClientFactory}.</li>
 *
 *     <li><b>RAG modulaire</b> : utilise {@link RetrievalAugmentationAdvisor} avec
 *         {@code RewriteQueryTransformer} et {@code MultiQueryExpander} pour améliorer
 *         la pertinence des templates trouvés.</li>
 *
 *     <li><b>Structured Output</b> : la méthode {@link #planDesign(String, String)}
 *         retourne un {@link DesignPlan} typé via {@code entity(DesignPlan.class)},
 *         avec validation et retry automatique via {@code StructuredOutputValidationAdvisor}.</li>
 * </ul>
 *
 * <h2>Flux d'appel pour le chat standard</h2>
 * <pre>
 * 1. Analyser la complexité du message
 * 2. Obtenir un ChatClient adapté (options SIMPLE / CREATIVE / COMPLEX)
 * 3. Construire le prompt (system + user + advisors)
 * 4. Inclure : RAG advisor + Memory advisor + Logger advisor
 * 5. Appeler l'IA avec les tools Penpot
 * 6. Retourner la réponse textuelle
 * </pre>
 *
 * @see RequestComplexityAnalyzer Détection de complexité
 * @see ChatClientFactory Factory de ChatClient par complexité
 * @see RetrievalAugmentationAdvisor RAG modulaire
 * @see DesignPlan Structured output pour la planification
 */
@Slf4j
@Component
@Primary
@RequiredArgsConstructor
public class OllamaAiAdapter implements AiServicePort {

    // ==================== DÉPENDANCES CORE ====================

    /** Client par défaut */
    private final ChatClient chatClient;

    /** Factory pour obtenir un client adapté à chaque complexité. */
    private final ChatClientFactory chatClientFactory;

    /** Analyseur de complexité des requêtes. */
    private final RequestComplexityAnalyzer complexityAnalyzer;

    /** Mémoire de conversation persistée. */
    private final ChatMemory chatMemory;

    /** Service de configuration des prompts système. */
    private final PromptsConfigService promptsConfigService;

    /** RAG Modulaire : advisor complet avec rewrite + multi-query + retrieval. */
    private final RetrievalAugmentationAdvisor retrievalAugmentationAdvisor;

    // ==================== TOOLS PENPOT ====================

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

    /** Tools Penpot pour la gestion du contenu. */
    private final PenpotDeleteTools penpotDeleteTools;

    /** Tools Penpot pour l'inspection de la page. */
    private final PenpotInspectorTools penpotInspectorTools;

    // ==================== CHAT PRINCIPAL ====================

    /**
     * Traite un message utilisateur dans le cadre d'une conversation persistée.
     *
     * <h3>Pipeline</h3>
     * <ol>
     *     <li>Détection de complexité ({@link RequestComplexityAnalyzer#analyze})</li>
     *     <li>Sélection du {@link ChatClient} adapté (options SIMPLE/CREATIVE/COMPLEX)</li>
     *     <li>Appel avec : RAG advisor, Memory advisor, Logger advisor, Tools Penpot</li>
     * </ol>
     *
     * <h3>Note sur le RAG</h3>
     * <p>Le {@link RetrievalAugmentationAdvisor} remplace les appels manuels à
     * {@code RagTemplateService} effectués via les tools. Il opère en amont du LLM,
     * enrichissant le prompt avec les templates pertinents avant même que le modèle
     * ne décide d'appeler un tool.</p>
     *
     * @param conversationId identifiant unique de la conversation
     * @param userMessage    message de l'utilisateur
     * @return réponse textuelle de l'IA
     */
    @Override
    public String chat(String conversationId, String userMessage) {
        try {
            // 1. Détection de complexité
            TaskComplexity complexity = complexityAnalyzer.analyze(userMessage);
            log.info(
                "Processing chat (conversation={}, complexity={}, messageLength={})",
                conversationId, complexity, userMessage.length()
            );

            // 2. Client adapté à la complexité détectée
            ChatClient adaptedClient = chatClientFactory.buildForComplexity(complexity);

            // 3. Appel IA avec tous les advisors et tools
            String response = adaptedClient.prompt()
                .system(promptsConfigService.getInitialInstructions())
                .user(userMessage)
                .advisors(
                    // RAG modulaire : enrichit le prompt avec les templates pertinents
                    retrievalAugmentationAdvisor,
                    new SimpleLoggerAdvisor()
                )
                .advisors(advisor -> advisor.param(CONVERSATION_ID, conversationId))
                .tools(
                    templateSearchTools,
                    penpotShapeTools,
                    penpotTransformTools,
                    penpotLayoutTools,
                    penpotAssetTools,
                    penpotContentTools,
                    penpotDeleteTools,
                    penpotInspectorTools
                )
                .call()
                .content();

            // 4. Log du thinking si présent (mode COMPLEX avec qwen3/deepseek)
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

    // ==================== STRUCTURED OUTPUT ====================

    /**
     * Génère un plan de design structuré ({@link DesignPlan}) à partir d'une requête.
     *
     * <p>Utilise {@code entity(DesignPlan.class)} pour mapper directement la réponse
     * du LLM vers un record Java sans parsing manuel.</p>
     *
     * <p>Le {@code StructuredOutputValidationAdvisor} valide la réponse contre le
     * schéma JSON généré depuis {@link DesignPlan} et retente jusqu'à 3 fois si
     * la validation échoue. Chaque retry inclut les erreurs de validation pour
     * guider le modèle vers une correction.</p>
     *
     * <h3>Quand utiliser cette méthode ?</h3>
     * <p>Appelée pour les tâches COMPLEX ou CREATIVE où le modèle doit planifier
     * explicitement une séquence d'opérations avant de les exécuter.
     * Les tâches SIMPLE utilisent directement {@link #chat} avec les tools.</p>
     *
     * @param conversationId identifiant de la conversation
     * @param userMessage    la requête de design
     * @return le plan structuré ou un plan "explain" en cas d'échec
     */
    public DesignPlan planDesign(String conversationId, String userMessage) {
        try {
            log.info("Planning design for conversation={}", conversationId);

            // Advisor de validation avec 3 tentatives max
            var validationAdvisor = org.springframework.ai.chat.client.advisor
                .StructuredOutputValidationAdvisor.builder()
                .outputType(DesignPlan.class)
                .maxRepeatAttempts(3)
                .build();

            // Client COMPLEX pour la planification (thinking activé)
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

            log.info(
                "Design plan generated: action={}, shapes={}, complexity={}",
                plan.action(), plan.hasShapes() ? plan.shapes().size() : 0, plan.complexity()
            );
            return plan;

        } catch (Exception e) {
            log.error("Error during design planning for conversation={}", conversationId, e);
            return DesignPlan.explain(
                "Design planning failed: " + e.getMessage() + ". Please try again."
            );
        }
    }

    // ==================== GESTION MÉMOIRE ====================

    /**
     * Efface l'historique d'une conversation.
     *
     * @param conversationId l'ID de la conversation à effacer
     * @throws IllegalArgumentException si l'ID est null ou vide
     */
    @Override
    public void clearConversation(String conversationId) {
        if (conversationId == null || conversationId.isBlank()) {
            throw new IllegalArgumentException("Conversation ID cannot be null or empty");
        }
        try {
            log.info("Clearing conversation history for: {}", conversationId);
            int messageBefore = chatMemory.get(conversationId).size();
            chatMemory.clear(conversationId);
            log.info("Cleared conversation {} ({} messages removed)", conversationId, messageBefore);
        } catch (Exception e) {
            log.error("Failed to clear conversation: {}", conversationId, e);
            throw new ToolExecutionException(
                "Failed to clear conversation " + conversationId + ": " + e.getMessage(), e
            );
        }
    }

    // ==================== MÉTHODES PRIVÉES ====================

    /**
     * Construit le prompt système spécialisé pour la génération de plans de design.
     * Explique au modèle le format JSON attendu et les champs obligatoires.
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
     * Silencieux si non disponible (modèles sans thinking mode).
     *
     * <p>Le thinking est uniquement disponible en mode COMPLEX avec des modèles
     * compatibles comme qwen3 ou deepseek-r1.</p>
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
            log.trace("Could not retrieve thinking metadata: {}", e.getMessage());
        }
    }
}