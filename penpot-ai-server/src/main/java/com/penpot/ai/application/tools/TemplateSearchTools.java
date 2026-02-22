package com.penpot.ai.application.tools;

import com.penpot.ai.application.service.*;
import com.penpot.ai.application.tools.support.ToolResponseBuilder;
import com.penpot.ai.core.domain.TemplateSpecs;
import com.penpot.ai.infrastructure.strategy.TemplateSpecsFormatter;
import com.penpot.ai.model.MarketingTemplate;
import com.penpot.ai.shared.util.JsonUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.*;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Tools pour la recherche de templates marketing via RAG.
 *
 * <p>Les erreurs utilisent désormais {@link ToolResponseBuilder#error} (DRY).</p>
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
        After finding templates, use getTemplateDesignSpecs() to get full design specifications.
        Categories: social_media_post, email, poster_a3, poster_a4, flyer_a5.
        Returns: list of matching templates with id, type, description, tags.
        """)
    public String searchTemplates(
        @ToolParam(description = "Natural language description of the desired marketing material.")
        String query
    ) {
        log.info("Tool called: searchTemplates (query='{}')", query);
        if (query == null || query.isBlank()) return ToolResponseBuilder.error("Query cannot be empty");

        try {
            List<MarketingTemplate> templates = ragTemplateService.searchTemplates(query);
            if (templates.isEmpty()) return formatNoResults(query);
            log.info("Found {} templates for query: '{}'", templates.size(), query);
            return formatTemplateList(templates);
        } catch (Exception e) {
            log.error("Error searching templates for query: '{}'", query, e);
            return ToolResponseBuilder.error("Search failed: " + e.getMessage());
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
        log.info("Tool called: getTemplateDesignSpecs (templateId='{}')", templateId);
        if (templateId == null || templateId.isBlank()) return ToolResponseBuilder.error("Template ID cannot be empty");

        try {
            MarketingTemplate template = ragTemplateService.getTemplateById(templateId)
                .orElseThrow(() -> new IllegalArgumentException("Template not found: " + templateId));
            TemplateSpecs specs = specsExtractor.extractSpecs(template);
            return specsFormatter.format(specs);
        } catch (IllegalArgumentException e) {
            log.warn("Template not found: {}", templateId);
            return ToolResponseBuilder.error("Template not found: " + templateId);
        } catch (Exception e) {
            log.error("Error extracting design specs from template: {}", templateId, e);
            return ToolResponseBuilder.error("Failed to extract specs: " + e.getMessage());
        }
    }

    @Tool(description = """
        List all available template types, categories, and tags.
        Call this when user asks "what can you create?" or wants to browse options.
        """)
    public String listTemplateTypes() {
        log.info("Tool called: listTemplateTypes");
        try {
            Set<String> types = ragTemplateService.getAvailableTypes();
            Set<String> tags  = ragTemplateService.getAvailableTags();
            int total         = ragTemplateService.getTemplateCount();
            return formatTypesAndTags(types, tags, total);
        } catch (Exception e) {
            log.error("Error listing template types", e);
            return ToolResponseBuilder.error("Failed to list types: " + e.getMessage());
        }
    }

    @Tool(description = """
        Get all templates of a specific type/category.
        Type mapping: "post" → social_media_post, "email" → email, "poster" → poster_a3/a4, "flyer" → flyer_a5.
        """)
    public String getTemplatesByType(
        @ToolParam(description = "Template type: social_media_post, social_media_story, email, poster_a3, poster_a4, flyer_a5")
        String type
    ) {
        log.info("Tool called: getTemplatesByType (type='{}')", type);
        if (type == null || type.isBlank()) return ToolResponseBuilder.error("Type cannot be empty");

        try {
            List<MarketingTemplate> templates = ragTemplateService.getTemplatesByType(type);
            if (templates.isEmpty()) return formatNoTypeResults(type, ragTemplateService.getAvailableTypes());
            return formatTemplatesByType(type, templates);
        } catch (Exception e) {
            log.error("Error getting templates by type: '{}'", type, e);
            return ToolResponseBuilder.error("Failed to get templates: " + e.getMessage());
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
        log.info("Tool called: getTemplatesByTag (tag='{}')", tag);
        if (tag == null || tag.isBlank()) return ToolResponseBuilder.error("Tag cannot be empty");

        try {
            List<MarketingTemplate> templates = ragTemplateService.getTemplatesByTag(tag);
            if (templates.isEmpty()) return formatNoTagResults(tag, ragTemplateService.getAvailableTags());
            return formatTemplatesByTag(tag, templates);
        } catch (Exception e) {
            log.error("Error getting templates by tag: '{}'", tag, e);
            return ToolResponseBuilder.error("Failed to get templates: " + e.getMessage());
        }
    }

    // ==================== FORMATTING (specific to template responses) ====================

    private String formatTemplateList(List<MarketingTemplate> templates) {
        return String.format(
            "{\"success\": true, \"count\": %d, \"templates\": %s, "
            + "\"message\": \"Use getTemplateDesignSpecs(templateId) to get full design specifications\"}",
            templates.size(), formatTemplatesArray(templates)
        );
    }

    private String formatTemplatesArray(List<MarketingTemplate> templates) {
        return templates.stream()
            .map(t -> String.format("{\"id\": %s, \"type\": %s, \"description\": %s, \"tags\": %s}",
                JsonUtils.escapeJson(t.getId()), JsonUtils.escapeJson(t.getType()),
                JsonUtils.escapeJson(t.getDescription()), formatTags(t.getTags())))
            .collect(Collectors.joining(", ", "[", "]"));
    }

    private String formatTemplatesByType(String type, List<MarketingTemplate> templates) {
        return String.format("{\"success\": true, \"type\": %s, \"count\": %d, \"templates\": %s}",
            JsonUtils.escapeJson(type), templates.size(), formatTemplatesArray(templates));
    }

    private String formatTemplatesByTag(String tag, List<MarketingTemplate> templates) {
        return String.format("{\"success\": true, \"tag\": %s, \"count\": %d, \"templates\": %s}",
            JsonUtils.escapeJson(tag), templates.size(), formatTemplatesArray(templates));
    }

    private String formatTypesAndTags(Set<String> types, Set<String> tags, int total) {
        String typesJson = types.stream().map(JsonUtils::escapeJson).collect(Collectors.joining(", ", "[", "]"));
        String tagsJson  = tags.stream().map(JsonUtils::escapeJson).collect(Collectors.joining(", ", "[", "]"));
        return String.format(
            "{\"success\": true, \"types\": %s, \"tags\": %s, "
            + "\"count\": {\"types\": %d, \"tags\": %d, \"total_templates\": %d}}",
            typesJson, tagsJson, types.size(), tags.size(), total);
    }

    private String formatNoResults(String query) {
        return String.format(
            "{\"success\": true, \"templates\": [], \"count\": 0, \"query\": %s, "
            + "\"suggestion\": \"Try listTemplateTypes() to see available categories\"}",
            JsonUtils.escapeJson(query));
    }

    private String formatNoTypeResults(String type, Set<String> available) {
        return String.format("{\"success\": false, \"requestedType\": %s, \"availableTypes\": %s}",
            JsonUtils.escapeJson(type),
            available.stream().map(JsonUtils::escapeJson).collect(Collectors.joining(", ", "[", "]")));
    }

    private String formatNoTagResults(String tag, Set<String> available) {
        return String.format("{\"success\": false, \"requestedTag\": %s, \"availableTags\": %s}",
            JsonUtils.escapeJson(tag),
            available.stream().map(JsonUtils::escapeJson).collect(Collectors.joining(", ", "[", "]")));
    }

    private String formatTags(List<String> tags) {
        if (tags == null || tags.isEmpty()) return "[]";
        return tags.stream().map(JsonUtils::escapeJson).collect(Collectors.joining(", ", "[", "]"));
    }
}