package com.penpot.mcp.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.*;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.stereotype.Service;
import java.util.*;

/**
 * Service AI principal pour l'intégration Penpot.
 * <p>
 * Ce service orchestre les interactions avec l'IA pour:
 * </p>
 * <ul>
 *   <li>Dialoguer avec les utilisateurs via chat contextuel</li>
 *   <li>Générer du code JavaScript pour l'API Penpot</li>
 *   <li>Enrichir le contexte avec la documentation API pertinente</li>
 *   <li>Fournir l'accès à la documentation de l'API</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PenpotAiService {

    /** Builder pour créer des clients de chat AI */
    private final ChatClient.Builder chatClientBuilder;

    /** Service de configuration des prompts système */
    private final PromptsConfigService promptsConfigService;

    /** Service de documentation API */
    private final ApiDocsService apiDocsService;

    /**
     * Engage une conversation avec l'assistant AI.
     * <p>
     * Le système maintient le contexte conversationnel en incluant
     * l'historique des messages et les instructions système initiales.
     *
     * @param userMessage le message de l'utilisateur
     * @param conversationHistory l'historique des messages précédents (peut être null)
     * @return la réponse de l'assistant AI
     * @throws RuntimeException si une erreur survient pendant l'interaction AI
     */
    public String chat(String userMessage, List<Message> conversationHistory) {
        try {
            List<Message> messages = new ArrayList<>();
            String systemInstructions = promptsConfigService.getInitialInstructions();
            messages.add(new SystemMessage(systemInstructions));

            if (conversationHistory != null) messages.addAll(conversationHistory);
            messages.add(new UserMessage(userMessage));

            Prompt prompt = new Prompt(messages);
            ChatClient chatClient = chatClientBuilder.build();
            ChatResponse response = chatClient.prompt(prompt).call().chatResponse();

            return response.getResult().getOutput().getContent();
        } catch (Exception e) {
            log.error("Error during AI chat", e);
            throw new RuntimeException("AI service error: " + e.getMessage(), e);
        }
    }

    /**
     * Génère du code JavaScript exécutable via AI pour accomplir une tâche.
     * <p>
     * Cette méthode:
     * </p>
     * <ol>
     *   <li>Enrichit le contexte avec la documentation API pertinente</li>
     *   <li>Utilise un prompt spécialisé pour la génération de code</li>
     *   <li>Nettoie le code généré (retire les backticks markdown)</li>
     * </ol>
     * <p>
     * Le code généré est prêt à être exécuté directement dans le contexte
     * du plugin Penpot sans modification.
     *
     * @param task la description de la tâche à accomplir
     * @param context le contexte additionnel (état actuel, contraintes, etc.)
     * @return le code JavaScript exécutable généré
     */
    public String executeCodeWithAi(String task, String context) {
        String enrichedContext = enrichContextWithApiInfo(context);
        String systemPrompt = """
            You are an expert in the Penpot Plugin API. Your task is to generate JavaScript code
            that accomplishes the given task using the Penpot API.

            CRITICAL RULES:
            1. Return ONLY executable JavaScript code, NO markdown backticks, NO explanations
            2. The code will be executed directly in the Penpot plugin context
            3. Available global objects: penpot, penpotUtils, storage, console
            4. DO NOT use require() or import statements - everything is already available
            5. DO NOT log information that you are also returning

            PENPOT API EXAMPLES:

            Create a rectangle:
            const rect = penpot.createRectangle();
            rect.resize(100, 50);
            rect.fills = [{ fillColor: '#FF0000' }];
            return rect;

            Create a board:
            const board = penpot.createBoard();
            board.name = 'My Board';
            board.resize(800, 600);
            return board;

            Create text:
            const text = penpot.createText('Hello World');
            text.fontSize = 24;
            text.fills = [{ fillColor: '#000000' }];
            return text;

            Access current selection:
            const selected = penpot.selection;
            if (selected.length > 0) {
                const shape = selected[0];
                shape.name = 'Modified Shape';
            }
            return selected;

            Find shapes by name:
            const shape = penpotUtils.findShape(s => s.name === 'MyShape');
            return shape;

            Get page structure:
            const structure = penpotUtils.shapeStructure(penpot.root, 3);
            return structure;

            IMPORTANT NOTES:
            - Use penpot.createRectangle(), NOT penpot.createShape()
            - Set fills with array: rect.fills = [{ fillColor: '#RRGGBB' }]
            - Use resize() method: shape.resize(width, height)
            - Return the created shape or result at the end
            - Handle errors with try-catch if needed
            - NO COMMENTS ALLOWED

            Context: """ + enrichedContext;

        List<Message> messages = List.of(
            new SystemMessage(systemPrompt),
            new UserMessage("Generate ONLY executable JavaScript code (no markdown, no explanations) for: " + task)
        );

        Prompt prompt = new Prompt(messages);
        ChatClient chatClient = chatClientBuilder.build();
        ChatResponse response = chatClient.prompt(prompt).call().chatResponse();

        String code = response.getResult().getOutput().getContent();
        code = cleanGeneratedCode(code);

        return code;
    }

    /**
     * Nettoie le code généré par l'IA.
     * <p>
     * Supprime les artefacts markdown comme les backticks (```javascript, ```js, ```)
     * qui pourraient être présents dans la réponse de l'IA.
     *
     * @param code le code brut généré par l'IA
     * @return le code nettoyé, prêt à l'exécution
     */
    private String cleanGeneratedCode(String code) {
        if (code == null) return "";
        code = code.replaceAll("^```(?:javascript|js)?\\s*", "");
        code = code.replaceAll("```\\s*$", "");
        code = code.replaceAll("//[^\n]*", "");
        code = code.replaceAll("/\\*.*?\\*/", "");
        code = code.replaceAll("\\n\\s*\\n", "\n");
        code = code.trim();
        return code;
    }

    /**
     * Enrichit le contexte avec des informations de l'API Penpot.
     * <p>
     * Cette méthode ajoute automatiquement la documentation des types API
     * couramment utilisés (Penpot, Shape, Rectangle, Board) au contexte
     * si ces types ne sont pas déjà mentionnés.
     * </p>
     * <p>
     * La documentation est tronquée à 500 caractères pour chaque type
     * afin d'éviter des prompts trop volumineux.
     *
     * @param context le contexte fourni par l'utilisateur
     * @return le contexte enrichi avec la documentation API
     */
    private String enrichContextWithApiInfo(String context) {
        StringBuilder enriched = new StringBuilder(context);
        List<String> commonTypes = Arrays.asList("Penpot", "Shape", "Rectangle", "Board");

        for (String typeName : commonTypes) {
            ApiDocsService.ApiType apiType = apiDocsService.getType(typeName);
            if (apiType != null && !context.contains(typeName)) {
                enriched.append("\n\n### ").append(typeName).append(" API Overview:\n");
                String overview = apiType.getOverviewText();
                if (overview.length() > 500) {
                    overview = overview.substring(0, 500) + "...";
                }
                enriched.append(overview);
            }
        }

        return enriched.toString();
    }

    /**
     * Retourne la vue d'ensemble de l'API Penpot.
     * <p>
     * Cette documentation de haut niveau provient des instructions
     * initiales configurées dans prompts.yml.
     *
     * @return la documentation d'ensemble de l'API
     */
    public String getPenpotOverview() {
        return promptsConfigService.getInitialInstructions();
    }

    /**
     * Obtient la documentation pour un type ou membre API spécifique.
     * <p>
     * Si seul le type est spécifié, retourne la documentation complète
     * du type (limitée à 2000 caractères pour éviter des réponses trop longues).
     * </p>
     * <p>
     * Si un membre est spécifié, retourne uniquement la documentation
     * de ce membre spécifique.
     *
     * @param typeName le nom du type API
     * @param memberName le nom du membre (optionnel)
     * @return la documentation formatée ou un message d'erreur si non trouvé
     */
    public String getApiTypeInfo(String typeName, String memberName) {
        ApiDocsService.ApiType apiType = apiDocsService.getType(typeName);
        if (apiType == null) return "Type '" + typeName + "' not found in API documentation.";

        if (memberName != null && !memberName.isEmpty()) {
            String memberDoc = apiType.getMember(memberName);
            if (memberDoc != null) {
                return memberDoc;
            } else {
                return "Member '" + memberName + "' not found in type '" + typeName + "'.";
            }
        }

        String fullText = apiType.getFullText();
        if (fullText.length() <= 2000) {
            return fullText;
        } else {
            return apiType.getOverviewText() + 
                "\n\nMember details not provided (too long). " +
                "Request specific member information for more details.";
        }
    }
}