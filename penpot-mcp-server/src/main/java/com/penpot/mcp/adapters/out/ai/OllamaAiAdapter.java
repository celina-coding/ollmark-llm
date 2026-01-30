package com.penpot.mcp.adapters.out.ai;

import com.penpot.mcp.core.domain.AiContext;
import com.penpot.mcp.core.ports.out.*;
import com.penpot.mcp.application.service.PromptsConfigService;
import com.penpot.mcp.application.tools.TemplateSearchTools;
import com.penpot.mcp.shared.exception.ToolExecutionException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.*;
import org.springframework.ai.chat.model.ChatResponse;
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
 *     <li>Accès à la documentation API</li>
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

    /** Port d'accès à la documentation de l'API Penpot. */
    private final ApiDocumentationPort apiDocumentationPort;

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

            log.info("Chat response generated successfully (length: {} chars)", 
                response.length());

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

    @Override
    public String getApiTypeInfo(String typeName, String memberName) {
        log.debug("Getting API type info for: {} (member: {})", typeName, memberName);
        return apiDocumentationPort.getTypeInfo(typeName, memberName)
            .orElse("Type '" + typeName + "' not found in API documentation.");
    }

    @Override
    public String getPenpotOverview() {
        log.debug("Getting Penpot API overview");
        return apiDocumentationPort.getOverview();
    }

    /**
     * Construit le prompt système pour le chat conversationnel.
     * 
     * <h3>Contenu du prompt</h3>
     * <ul>
     *     <li>Instructions initiales (depuis PromptsConfigService)</li>
     *     <li>Capacités de recherche de templates (tools RAG)</li>
     *     <li>Workflow recommandé pour l'utilisation des templates</li>
     *     <li>Contexte conversationnel (géré automatiquement par ChatMemory)</li>
     * </ul>
     *
     * @return prompt système formaté pour le chat
     */
    private String buildChatSystemPrompt() {
        StringBuilder prompt = new StringBuilder();

        prompt.append(promptsConfigService.getInitialInstructions());
        prompt.append("\n\n");
        prompt.append("""
            # CAPACITÉS DE RECHERCHE DE TEMPLATES MARKETING

            Tu as accès à des tools pour rechercher et générer des templates de design marketing :

            ## Tools disponibles

            1. **searchTemplates(query)** - Recherche sémantique de templates
               - Exemples : "post sur les réseaux sociaux", "newsletter par email"
               - Retourne : liste de templates pertinents avec ID, type, description, tags

            2. **generateFromTemplate(templateId)** - Génère le code JavaScript depuis un template
               - Utilise ceci après avoir trouvé un template pour créer le design
               - Retourne : code JavaScript exécutable pour Penpot

            3. **listTemplateTypes()** - Liste toutes les catégories de templates
               - Exemples : social_media_post, email, poster_a3, flyer_a5

            4. **getTemplatesByType(type)** - Récupère tous les templates d'une catégorie

            ## Quand utiliser les templates

            Utilise les tools de templates lorsque l'utilisateur :
            - Demande à créer du contenu marketing (posts, stories, emails, posters, flyers)
            - Mentionne des types de design spécifiques (réseaux sociaux, email marketing, print)
            - Veut partir d'un template ou d'un exemple
            - Parle de contenu promotionnel ou publicitaire

            ## Workflow recommandé

            1. **Recherche** : Utilise `searchTemplates()` avec une requête décrivant le besoin
            2. **Présentation** : Présente les options trouvées à l'utilisateur avec leurs descriptions
            3. **Sélection** : Demande à l'utilisateur de choisir (ou choisis le plus pertinent)
            4. **Génération** : Utilise `generateFromTemplate(templateId)` pour obtenir le code
            5. **Personnalisation** : Explique comment personnaliser le résultat si nécessaire

            ## Contexte conversationnel

            - Tu as accès à tout l'historique de la conversation automatiquement
            - Fais référence aux messages précédents naturellement
            - Maintiens le contexte sur plusieurs tours de conversation
            - Pose des questions de clarification si nécessaire

            ## Principes importants

            - Explique toujours quels templates tu as trouvés
            - Laisse l'utilisateur choisir avant de générer (sauf si évident)
            - Sois conversationnel et amical
            - Adapte-toi au niveau technique de l'utilisateur
            - Propose des améliorations et des suggestions créatives
            """);

        return prompt.toString();
    }

    /**
     * Construit le prompt système pour la génération de code.
     * Inclut les règles strictes, la documentation API, exemples et contraintes.
     *
     * @param context contexte enrichi avec documentation et exemples
     * @return prompt système formaté pour la génération
     */
    private String buildCodeGenerationSystemPrompt(AiContext context) {
        StringBuilder prompt = new StringBuilder();

        prompt.append("""
            # EXPERT PENPOT PLUGIN API - GÉNÉRATION DE CODE JAVASCRIPT

            Tu es un expert de l'API Penpot Plugin. Ta tâche est de générer du code JavaScript
            qui accomplit la tâche demandée en utilisant l'API Penpot.

            ## RÈGLES CRITIQUES (ABSOLUES)

            1. Retourne UNIQUEMENT du code JavaScript exécutable
               - PAS de backticks markdown (```javascript ou ```)
               - PAS d'explications avant ou après le code
               - PAS de commentaires sauf si demandés explicitement

            2. Le code sera exécuté directement dans le contexte du plugin Penpot
               - Objets globaux disponibles : penpot, penpotUtils, storage, console
               - N'utilise PAS require() ou import
               - N'utilise PAS localStorage ou sessionStorage

            3. Ne log PAS d'informations que tu retournes déjà
               - Si tu retournes une valeur, n'utilise pas console.log() pour la même info

            ## CAPACITÉ DE RECHERCHE DE TEMPLATES

            Tu peux utiliser `searchTemplates()` pour trouver des templates marketing quand :
            - L'utilisateur veut créer du contenu pour les réseaux sociaux
            - L'utilisateur veut créer du matériel d'email marketing
            - L'utilisateur veut créer du matériel imprimé (posters, flyers)

            Ensuite utilise `generateFromTemplate(templateId)` pour obtenir du code prêt à l'emploi.
            """);

        // Documentation API pertinente
        if (!context.getApiDocumentation().isEmpty()) {
            prompt.append("\n## DOCUMENTATION API PERTINENTE\n\n");
            context.getApiDocumentation().forEach((type, doc) -> {
                prompt.append("### ").append(type).append("\n\n");
                prompt.append(doc).append("\n\n");
            });
        }

        // Exemples de code
        if (!context.getExamples().isEmpty()) {
            prompt.append("## EXEMPLES D'UTILISATION DE L'API PENPOT\n\n");
            context.getExamples().forEach(example -> {
                prompt.append(example).append("\n\n");
            });
        }

        // Best practices
        if (!context.getBestPractices().isEmpty()) {
            prompt.append("## BONNES PRATIQUES\n\n");
            context.getBestPractices().forEach(practice -> {
                prompt.append("- ").append(practice).append("\n");
            });
            prompt.append("\n");
        }

        // Contraintes
        if (!context.getConstraints().isEmpty()) {
            prompt.append("## CONTRAINTES TECHNIQUES\n\n");
            context.getConstraints().forEach(constraint -> {
                prompt.append("- ").append(constraint).append("\n");
            });
            prompt.append("\n");
        }

        return prompt.toString();
    }

    /**
     * Construit le message utilisateur pour la génération de code.
     *
     * @param context contexte contenant la tâche et le contexte utilisateur
     * @return message utilisateur formaté
     */
    private String buildCodeGenerationUserPrompt(AiContext context) {
        StringBuilder prompt = new StringBuilder();

        prompt.append("Génère UNIQUEMENT du code JavaScript exécutable (pas de markdown, pas d'explications) pour : ");
        prompt.append(context.getTask());

        if (!context.getUserContext().isBlank()) {
            prompt.append("\n\nContexte additionnel : ");
            prompt.append(context.getUserContext());
        }

        return prompt.toString();
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