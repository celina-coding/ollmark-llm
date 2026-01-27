package com.penpot.mcp.adapters.out.ai;

import com.penpot.mcp.core.domain.AiContext;
import com.penpot.mcp.core.ports.out.*;
import com.penpot.mcp.application.service.PromptsConfigService;
import com.penpot.mcp.application.tools.TemplateSearchTools;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.*;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;
import java.util.*;

/**
 * Adapter pour le service AI Ollama.
 * Implémente le port AiServicePort (Hexagonal Architecture).
 * Transforme les requêtes du domaine en appels Ollama.
 * 
 * Now includes RAG template search tools for function calling.
 */
@Slf4j
@Component
@Primary
@RequiredArgsConstructor
public class OllamaAiAdapter implements AiServicePort {

    private final ChatClient chatClient;
    private final PromptsConfigService promptsConfigService;
    private final ApiDocumentationPort apiDocumentationPort;
    private final TemplateSearchTools templateSearchTools;

    @Override
    public String chat(String userMessage, List<Message> conversationHistory) {
        try {
            log.info("Processing chat request (message length: {} chars)", 
                userMessage.length());

            List<Message> messages = new ArrayList<>();
            String systemInstructions = buildChatSystemPrompt();
            messages.add(new SystemMessage(systemInstructions));

            if (conversationHistory != null && !conversationHistory.isEmpty()) {
                messages.addAll(conversationHistory);
                log.debug("Added {} messages from conversation history", 
                    conversationHistory.size());
            }

            messages.add(new UserMessage(userMessage));
            Prompt prompt = new Prompt(messages);
            
            // Use ChatClient with template search tools
            ChatResponse response = chatClient.prompt(prompt)
                    .tools(templateSearchTools)  // Make RAG tools available
                    .call()
                    .chatResponse();

            String result = response.getResult().getOutput().getText();
            log.info("Chat response generated successfully (length: {} chars)", 
                result.length());

            return result;
        } catch (Exception e) {
            log.error("Error during AI chat", e);
            throw new RuntimeException("AI service error: " + e.getMessage(), e);
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
            
            // Use ChatClient with template search tools for code generation too
            ChatResponse response = chatClient.prompt(prompt)
                    .tools(templateSearchTools)  // RAG tools available during code gen
                    .call()
                    .chatResponse();

            String code = response.getResult().getOutput().getText();
            code = cleanGeneratedCode(code);

            log.info("Code generated successfully (length: {} chars)", code.length());
            log.debug("Generated code preview: {}", 
                code.length() > 100 ? code.substring(0, 100) + "..." : code);

            return code;
        } catch (Exception e) {
            log.error("Error during code generation", e);
            throw new RuntimeException("Code generation failed: " + e.getMessage(), e);
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
     * Construit le prompt système pour le chat.
     * Inclut les instructions sur l'utilisation des tools RAG.
     */
    private String buildChatSystemPrompt() {
        StringBuilder prompt = new StringBuilder();
        
        prompt.append(promptsConfigService.getInitialInstructions());
        prompt.append("\n\n");
        
        prompt.append("""
            TEMPLATE SEARCH CAPABILITIES:
            You have access to tools for searching and generating marketing design templates:
            
            1. searchTemplates(query) - Find templates using natural language
               Examples: "social media post", "email newsletter"
            
            2. generateFromTemplate(templateId) - Generate JavaScript code from a template
               Use this after finding a template to create the actual design
            
            3. listTemplateTypes() - Show all available template categories
            
            4. getTemplatesByType(type) - Get templates of a specific category
            
            WHEN TO USE TEMPLATES:
            - User asks to create marketing materials (posts, stories, emails, posters, flyers)
            - User mentions specific design types (social media, email marketing, print)
            - User wants to start from a template or example
            
            WORKFLOW:
            1. Search for relevant templates using searchTemplates()
            2. Present options to user with descriptions
            3. When user selects, use generateFromTemplate() to get the code
            4. Execute the generated code or present it to the user
            
            Always explain what templates you found and let the user choose before generating.
            """);
        
        return prompt.toString();
    }

    /**
     * Construit le prompt système pour la génération de code.
     * Inclut les règles, exemples et contraintes.
     */
    private String buildCodeGenerationSystemPrompt(AiContext context) {
        StringBuilder prompt = new StringBuilder();

        prompt.append("""
            You are an expert in the Penpot Plugin API. Your task is to generate JavaScript code
            that accomplishes the given task using the Penpot API.
            
            CRITICAL RULES:
            1. Return ONLY executable JavaScript code, NO markdown backticks, NO explanations
            2. The code will be executed directly in the Penpot plugin context
            3. Available global objects: penpot, penpotUtils, storage, console
            4. DO NOT use require() or import statements - everything is already available
            5. DO NOT log information that you are also returning
            
            TEMPLATE SEARCH CAPABILITY:
            You can use searchTemplates() to find marketing templates when the user wants to create:
            - Social media content (posts, stories)
            - Email marketing materials
            - Print materials (posters, flyers)
            Then use generateFromTemplate(templateId) to get ready-to-use code.
            """);

        if (!context.getApiDocumentation().isEmpty()) {
            prompt.append("\nRELEVANT API DOCUMENTATION:\n");
            context.getApiDocumentation().forEach((type, doc) -> {
                prompt.append("\n### ").append(type).append("\n");
                prompt.append(doc).append("\n");
            });
            prompt.append("\n");
        }

        if (!context.getExamples().isEmpty()) {
            prompt.append("PENPOT API EXAMPLES:\n\n");
            context.getExamples().forEach(example -> {
                prompt.append(example).append("\n\n");
            });
        }

        if (!context.getBestPractices().isEmpty()) {
            prompt.append("BEST PRACTICES:\n");
            context.getBestPractices().forEach(practice -> {
                prompt.append("- ").append(practice).append("\n");
            });
            prompt.append("\n");
        }

        if (!context.getConstraints().isEmpty()) {
            prompt.append("CONSTRAINTS:\n");
            context.getConstraints().forEach(constraint -> {
                prompt.append("- ").append(constraint).append("\n");
            });
            prompt.append("\n");
        }

        return prompt.toString();
    }

    /**
     * Construit le message utilisateur pour la génération de code.
     */
    private String buildCodeGenerationUserPrompt(AiContext context) {
        StringBuilder prompt = new StringBuilder();

        prompt.append("Generate ONLY executable JavaScript code (no markdown, no explanations) for: ");
        prompt.append(context.getTask());

        if (!context.getUserContext().isBlank()) {
            prompt.append("\n\nAdditional context: ");
            prompt.append(context.getUserContext());
        }

        return prompt.toString();
    }

    /**
     * Nettoie le code généré par l'IA.
     * Supprime les artefacts markdown et les commentaires inutiles.
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