package com.penpot.mcp.application.tools;

import com.penpot.mcp.application.service.*;
import com.penpot.mcp.model.MarketingTemplate;
import com.penpot.mcp.shared.util.JsonUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.*;
import org.springframework.stereotype.Component;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Tools for searching and generating marketing templates using RAG.
 * These tools are available to the AI model via function calling.
 * 
 * The AI can call these tools when users ask about:
 * - Finding design templates
 * - Creating marketing materials
 * - Generating social media posts, stories, emails, posters, etc.
 * 
 * Architecture:
 * - Uses RAG (Retrieval-Augmented Generation) for semantic search
 * - Interprets design_recipe to generate JavaScript code
 * - Returns structured data for the AI to process
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TemplateSearchTools {

    private final RagTemplateService ragTemplateService;
    private final DesignRecipeInterpreter designRecipeInterpreter;

    /**
     * Search for marketing templates using semantic search (RAG).
     * This tool finds templates based on natural language queries.
     * 
     * Examples:
     * - "social media post for product launch"
     * - "email newsletter for tech company"
     * 
     * @param query Natural language description of what the user wants to create
     * @return JSON string with matching templates (id, type, description, tags)
     */
    @Tool(description = """
        Search for marketing design templates using semantic search. 
        Use this when users want to create marketing materials like:
        - Social media posts (Instagram, Facebook)
        - Email marketing templates
        - Posters (A2, A3) and flyers (A5)
        - Product announcements or promotions

        Returns a list of relevant templates with their metadata.
        """)
    public String searchTemplates(
        @ToolParam(description = "Natural language query describing what the user wants to create")
        String query
    ) {
        log.info("Tool called: searchTemplates with query: {}", query);
        if (query == null || query.isBlank()) return "{\"error\": \"Query cannot be empty\"}";

        try {
            List<MarketingTemplate> templates = ragTemplateService.searchTemplates(query);
            if (templates.isEmpty()) {
                return String.format(
                    "{\"message\": %s, \"suggestions\": %s}",
                    JsonUtils.escapeJson("No templates found for query: " + query),
                    listAvailableTypes()
                );
            }

            StringBuilder json = new StringBuilder("{\"templates\": [");

            for (int i = 0; i < templates.size(); i++) {
                MarketingTemplate t = templates.get(i);
                if (i > 0) json.append(",");

                json.append("{")
                    .append("\"id\":").append(JsonUtils.escapeJson(t.getId())).append(",")
                    .append("\"type\":").append(JsonUtils.escapeJson(t.getType())).append(",")
                    .append("\"description\":").append(JsonUtils.escapeJson(t.getDescription())).append(",")
                    .append("\"tags\":").append(formatTags(t.getTags()))
                    .append("}");
            }
            json.append("]}");

            log.info("Found {} templates for query: {}", templates.size(), query);
            return json.toString();
        } catch (Exception e) {
            log.error("Error searching templates", e);
            return String.format("{\"error\": %s}", JsonUtils.escapeJson(e.getMessage()));
        }
    }

    /**
     * Generate JavaScript code from a template's design recipe.
     * This tool converts a template into executable Penpot code.
     * 
     * @param templateId The ID of the template to generate code from
     * @return JavaScript code ready to execute in Penpot
     */
    @Tool(description = """
        Generate JavaScript code from a marketing template's design recipe.
        Use this after finding a template with searchTemplates to actually create the design.

        The generated code can be executed in Penpot to create the design elements:
        - Boards with proper dimensions
        - Backgrounds (colors, images, gradients)
        - Text elements with styling
        - Shapes and layouts
        - Complete design structure
        
        Returns executable JavaScript code for Penpot plugin.
        """)
    public String generateFromTemplate(
        @ToolParam(description = "The template ID to generate code from (obtained from searchTemplates)")
        String templateId
    ) {
        log.info("Tool called: generateFromTemplate with templateId: {}", templateId);
        if (templateId == null || templateId.isBlank()) {
            return "{\"error\": \"Template ID cannot be empty\"}";
        }

        try {
            MarketingTemplate template = ragTemplateService.getTemplateById(templateId)
                    .orElseThrow(() -> new IllegalArgumentException(
                            "Template not found: " + templateId));

            String code = designRecipeInterpreter.interpretDesignRecipe(template);

            log.info("Generated {} characters of code from template: {}", 
                    code.length(), templateId);

            return String.format(
                "{\"templateId\": %s, \"code\": %s, \"type\": %s}",
                JsonUtils.escapeJson(templateId),
                JsonUtils.escapeJson(code),
                JsonUtils.escapeJson(template.getType())
            );
        } catch (IllegalArgumentException e) {
            log.warn("Template not found: {}", templateId);
            return String.format("{\"error\": %s}", 
                    JsonUtils.escapeJson("Template not found: " + templateId));
        } catch (Exception e) {
            log.error("Error generating code from template", e);
            return String.format("{\"error\": %s}", JsonUtils.escapeJson(e.getMessage()));
        }
    }

    /**
     * List all available template types/categories.
     * Useful for helping users understand what kinds of templates exist.
     * 
     * @return JSON string with available types
     */
    @Tool(description = """
        List all available template types and categories.
        Use this to help users understand what kinds of marketing materials can be created.

        Returns categories like:
        - social_media_post
        - social_media_story  
        - email
        - poster_a3, poster_a2
        - flyer_a5
        """)
    public String listTemplateTypes() {
        log.info("Tool called: listTemplateTypes");

        try {
            return listAvailableTypes();
        } catch (Exception e) {
            log.error("Error listing template types", e);
            return String.format("{\"error\": %s}", JsonUtils.escapeJson(e.getMessage()));
        }
    }

    /**
     * Get templates by specific type/category.
     * More targeted than semantic search.
     * 
     * @param type The template type (e.g., "social_media_post", "email")
     * @return JSON string with templates of that type
     */
    @Tool(description = """
        Get all templates of a specific type/category.
        Use this when the user explicitly mentions a category like:
        - "social media post" -> type: social_media_post
        - "email" -> type: email
        - "poster" -> type: poster_a3 or poster_a2
        - "flyer" -> type: flyer_a5

        Returns all templates matching that exact type.
        """)
    public String getTemplatesByType(
        @ToolParam(description = "The template type (social_media_post, email, poster_a3, poster_a2, flyer_a5)")
        String type
    ) {
        log.info("Tool called: getTemplatesByType with type: {}", type);
        if (type == null || type.isBlank()) return "{\"error\": \"Type cannot be empty\"}";

        try {
            List<MarketingTemplate> templates = ragTemplateService.getTemplatesByType(type);

            if (templates.isEmpty()) {
                return String.format(
                    "{\"message\": %s, \"availableTypes\": %s}",
                    JsonUtils.escapeJson("No templates found for type: " + type),
                    listAvailableTypes()
                );
            }

            StringBuilder json = new StringBuilder("{\"type\": ")
                    .append(JsonUtils.escapeJson(type))
                    .append(", \"templates\": [");

            for (int i = 0; i < templates.size(); i++) {
                MarketingTemplate t = templates.get(i);
                if (i > 0) json.append(",");

                json.append("{")
                    .append("\"id\":").append(JsonUtils.escapeJson(t.getId())).append(",")
                    .append("\"description\":").append(JsonUtils.escapeJson(t.getDescription())).append(",")
                    .append("\"tags\":").append(formatTags(t.getTags()))
                    .append("}");
            }
            json.append("]}");

            log.info("Found {} templates for type: {}", templates.size(), type);
            return json.toString();
        } catch (Exception e) {
            log.error("Error getting templates by type", e);
            return String.format("{\"error\": %s}", JsonUtils.escapeJson(e.getMessage()));
        }
    }

    /**
     * List all available template types using JsonUtils.
     */
    private String listAvailableTypes() {
        var types = ragTemplateService.getAvailableTypes();
        return types.stream()
            .map(JsonUtils::escapeJson)
            .collect(Collectors.joining(", ", "[", "]"));
    }

    /**
     * Format tags as JSON array using JsonUtils.
     */
    private String formatTags(List<String> tags) {
        if (tags == null || tags.isEmpty()) return "[]";
        return tags.stream()
            .map(JsonUtils::escapeJson)
            .collect(Collectors.joining(", ", "[", "]"));
    }
}