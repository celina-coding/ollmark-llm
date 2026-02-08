package com.penpot.ai.application.tools;

import com.penpot.ai.application.service.*;
import com.penpot.ai.core.domain.TemplateSpecs;
import com.penpot.ai.infrastructure.strategy.TemplateSpecsFormatter;
import com.penpot.ai.model.MarketingTemplate;
import com.penpot.ai.shared.util.JsonUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.*;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Tools pour la recherche d'informations de templates marketing via RAG.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TemplateSearchTools {

    private final RagTemplateService ragTemplateService;
    private final TemplateSpecsExtractor specsExtractor;
    private final TemplateSpecsFormatter specsFormatter;

    @Tool(description = """
        Search for marketing design templates using semantic search (RAG).

        Use this to find templates that match the user's intention.
        This tool only SEARCHES and LISTS templates - it doesn't create anything.

        After finding relevant templates, use getTemplateDesignSpecs() to extract
        detailed design specifications (colors, dimensions, layout) that you'll
        need to create the actual design.

        Categories typically include:
        - social_media_post: Instagram, Facebook posts
        - email: Email marketing layouts
        - poster_a3, poster_a4: Print posters
        - flyer_a5: Promotional flyers

        Returns:
        - List of matching templates with id, type, description, tags
        - Use template.id with getTemplateDesignSpecs() to get full design info
        """)
    public String searchTemplates(
        @ToolParam(description = "Natural language description of the desired marketing material.")
        String query
    ) {
        log.info("Tool called: searchTemplates with query: '{}'", query);

        if (query == null || query.isBlank()) {
            return formatError("Query cannot be empty");
        }

        try {
            List<MarketingTemplate> templates = ragTemplateService.searchTemplates(query);
            if (templates.isEmpty()) return formatNoResults(query);

            log.info("Found {} templates for query: '{}'", templates.size(), query);
            return formatTemplateList(templates);
        } catch (Exception e) {
            log.error("Error searching templates for query: '{}'", query, e);
            return formatError("Search failed: " + e.getMessage());
        }
    }

    @Tool(description = """
        Get detailed design specifications from a template.

        This is the KEY tool for extracting design information that you'll use
        to create the actual design by calling creation tools (createBoard,
        createRectangle, createText, etc.).

        Returns complete design specifications including:
        - Dimensions (width, height, format)
        - Colors (primary, secondary, background, text)
        - Layout (mode, direction, positioning hints)
        - Typography (fonts, sizes, styles)
        - Background (color, texture, gradient, pattern)
        - Elements (badges, icons, decorative elements)
        - Usage hints (creation order, type-specific guidance)

        Use these specs as parameters for creation tools.
        """)
    public String getTemplateDesignSpecs(
        @ToolParam(description = "The template ID obtained from searchTemplates().")
        String templateId
    ) {
        log.info("Tool called: getTemplateDesignSpecs with templateId: '{}'", templateId);

        if (templateId == null || templateId.isBlank()) {
            return formatError("Template ID cannot be empty");
        }

        try {
            MarketingTemplate template = ragTemplateService.getTemplateById(templateId)
                .orElseThrow(() -> new IllegalArgumentException(
                    "Template not found: " + templateId
                ));

            log.info("Extracting design specs from template: {} (type: {})", 
                template.getId(), template.getType());

            TemplateSpecs specs = specsExtractor.extractSpecs(template);
            return specsFormatter.format(specs);
        } catch (IllegalArgumentException e) {
            log.warn("Template not found: {}", templateId);
            return formatError("Template not found: " + templateId);
        } catch (Exception e) {
            log.error("Error extracting design specs from template: {}", templateId, e);
            return formatError("Failed to extract specs: " + e.getMessage());
        }
    }

    @Tool(description = """
        List all available template types, categories, and tags.

        Returns:
        - types: All template categories (social_media_post, email, poster_a3, etc.)
        - tags: Available style tags (modern, professional, colorful, minimal, etc.)
        - count: Statistics about templates

        Call this when user asks "what can you create?" or wants to browse options.
        """)
    public String listTemplateTypes() {
        log.info("Tool called: listTemplateTypes");

        try {
            var types = ragTemplateService.getAvailableTypes();
            var tags = ragTemplateService.getAvailableTags();
            int totalTemplates = ragTemplateService.getTemplateCount();

            return formatTypesAndTags(types, tags, totalTemplates);
        } catch (Exception e) {
            log.error("Error listing template types", e);
            return formatError("Failed to list types: " + e.getMessage());
        }
    }

    @Tool(description = """
        Get all templates of a specific type/category.

        Type mapping:
        - "post" or "social media post" → social_media_post
        - "email" or "newsletter" → email
        - "poster" → poster_a3 or poster_a4
        - "flyer" → flyer_a5

        Returns a list of templates with id, description, and tags.
        """)
    public String getTemplatesByType(
        @ToolParam(description = "Template type: social_media_post, social_media_story, email, poster_a3, poster_a4, flyer_a5")
        String type
    ) {
        log.info("Tool called: getTemplatesByType with type: '{}'", type);
        if (type == null || type.isBlank()) {
            return formatError("Type cannot be empty");
        }

        try {
            List<MarketingTemplate> templates = ragTemplateService.getTemplatesByType(type);

            if (templates.isEmpty()) {
                var availableTypes = ragTemplateService.getAvailableTypes();
                return formatNoTypeResults(type, availableTypes);
            }

            log.info("Found {} templates for type: '{}'", templates.size(), type);
            return formatTemplatesByType(type, templates);
        } catch (Exception e) {
            log.error("Error getting templates by type: '{}'", type, e);
            return formatError("Failed to get templates: " + e.getMessage());
        }
    }

    @Tool(description = """
        Get templates filtered by a specific style tag.

        Tags represent design styles, contexts, or purposes:
        - Style tags: modern, classic, minimal, colorful, professional
        - Context tags: bakery, tech, fashion, food, product
        - Purpose tags: promotion, announcement, seasonal, deal

        Use this to narrow down templates based on aesthetic or context.
        """)
    public String getTemplatesByTag(
        @ToolParam(description = "Style or context tag to filter by.")
        String tag
    ) {
        log.info("Tool called: getTemplatesByTag with tag: '{}'", tag);
        if (tag == null || tag.isBlank()) return formatError("Tag cannot be empty");

        try {
            List<MarketingTemplate> templates = ragTemplateService.getTemplatesByTag(tag);

            if (templates.isEmpty()) {
                var availableTags = ragTemplateService.getAvailableTags();
                return formatNoTagResults(tag, availableTags);
            }

            log.info("Found {} templates for tag: '{}'", templates.size(), tag);
            return formatTemplatesByTag(tag, templates);
        } catch (Exception e) {
            log.error("Error getting templates by tag: '{}'", tag, e);
            return formatError("Failed to get templates: " + e.getMessage());
        }
    }

    // ==================== SIMPLE FORMATTING METHODS ====================

    private String formatTemplateList(List<MarketingTemplate> templates) {
        return String.format(
            "{\"success\": true, \"count\": %d, \"templates\": %s, \"message\": \"Use getTemplateDesignSpecs(templateId) to get full design specifications\"}",
            templates.size(),
            formatTemplatesArray(templates)
        );
    }

    private String formatTemplatesArray(List<MarketingTemplate> templates) {
        return templates.stream()
            .map(t -> String.format(
                "{\"id\": %s, \"type\": %s, \"description\": %s, \"tags\": %s}",
                JsonUtils.escapeJson(t.getId()),
                JsonUtils.escapeJson(t.getType()),
                JsonUtils.escapeJson(t.getDescription()),
                formatTags(t.getTags())
            ))
            .collect(Collectors.joining(", ", "[", "]"));
    }

    private String formatTemplatesByType(String type, List<MarketingTemplate> templates) {
        return String.format(
            "{\"success\": true, \"type\": %s, \"count\": %d, \"templates\": %s}",
            JsonUtils.escapeJson(type),
            templates.size(),
            formatTemplatesArray(templates)
        );
    }

    private String formatTemplatesByTag(String tag, List<MarketingTemplate> templates) {
        return String.format(
            "{\"success\": true, \"tag\": %s, \"count\": %d, \"templates\": %s}",
            JsonUtils.escapeJson(tag),
            templates.size(),
            formatTemplatesArray(templates)
        );
    }

    private String formatTypesAndTags(java.util.Set<String> types, java.util.Set<String> tags, int total) {
        String typesJson = types.stream()
            .map(JsonUtils::escapeJson)
            .collect(Collectors.joining(", ", "[", "]"));

        String tagsJson = tags.stream()
            .map(JsonUtils::escapeJson)
            .collect(Collectors.joining(", ", "[", "]"));

        return String.format(
            "{\"success\": true, \"types\": %s, \"tags\": %s, \"count\": {\"types\": %d, \"tags\": %d, \"total_templates\": %d}}",
            typesJson, tagsJson, types.size(), tags.size(), total
        );
    }

    private String formatNoResults(String query) {
        return String.format(
            "{\"success\": true, \"templates\": [], \"count\": 0, \"message\": \"No templates found for query\", \"query\": %s, \"suggestion\": \"Try listTemplateTypes() to see available categories or use broader search terms\"}",
            JsonUtils.escapeJson(query)
        );
    }

    private String formatNoTypeResults(String type, java.util.Set<String> availableTypes) {
        String typesJson = availableTypes.stream()
            .map(JsonUtils::escapeJson)
            .collect(Collectors.joining(", ", "[", "]"));

        return String.format(
            "{\"success\": false, \"message\": \"No templates found for type\", \"requestedType\": %s, \"availableTypes\": %s}",
            JsonUtils.escapeJson(type), typesJson
        );
    }

    private String formatNoTagResults(String tag, java.util.Set<String> availableTags) {
        String tagsJson = availableTags.stream()
            .map(JsonUtils::escapeJson)
            .collect(Collectors.joining(", ", "[", "]"));

        return String.format(
            "{\"success\": false, \"message\": \"No templates found for tag\", \"requestedTag\": %s, \"availableTags\": %s}",
            JsonUtils.escapeJson(tag), tagsJson
        );
    }

    private String formatTags(List<String> tags) {
        if (tags == null || tags.isEmpty()) return "[]";
        return tags.stream()
            .map(JsonUtils::escapeJson)
            .collect(Collectors.joining(", ", "[", "]"));
    }

    private String formatError(String message) {
        return String.format("{\"success\": false, \"error\": %s}", 
            JsonUtils.escapeJson(message));
    }
}