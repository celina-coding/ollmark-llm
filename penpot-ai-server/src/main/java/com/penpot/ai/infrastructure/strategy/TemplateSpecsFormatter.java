package com.penpot.ai.infrastructure.strategy;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.penpot.ai.core.domain.*;
import com.penpot.ai.core.domain.spec.*;
import com.penpot.ai.shared.util.JsonUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import java.util.*;

/**
 * Formatter pour convertir TemplateSpecs en JSON.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TemplateSpecsFormatter {

    private final ObjectMapper objectMapper;

    /**
     * Formate les specs en JSON structuré.
     * 
     * @param specs les spécifications à formater
     * @return JSON formaté
     */
    public String format(TemplateSpecs specs) {
        try {
            Map<String, Object> json = new LinkedHashMap<>();

            json.put("success", true);
            json.put("templateId", specs.getTemplateId());
            json.put("type", specs.getType());
            json.put("description", specs.getDescription());
            json.put("tags", specs.getTags());

            json.put("dimensions", formatDimensions(specs.getDimensions()));
            json.put("layout", formatLayout(specs.getLayout()));
            json.put("background", formatBackground(specs.getBackground()));
            json.put("typography", formatTypography(specs.getTypography()));
            json.put("colors", formatColors(specs.getColors()));
            json.put("elements", specs.getElements());

            if (!specs.getSections().isEmpty()) {
                json.put("sections", specs.getSections());
            }

            json.put("usage_hints", formatUsageHints(specs.getUsageHints()));

            return objectMapper.writeValueAsString(json);
        } catch (JsonProcessingException e) {
            log.error("Failed to format TemplateSpecs", e);
            return formatError("Failed to format specs: " + e.getMessage());
        }
    }

    private Map<String, Object> formatDimensions(DimensionsSpec dims) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("width", dims.getWidth());
        map.put("height", dims.getHeight());

        if (dims.getCanvasSize() != null) {
            map.put("canvas_size", dims.getCanvasSize());
        }
        if (dims.getFormat() != null) map.put("format", dims.getFormat());
        map.put("source", dims.getSource());

        return map;
    }

    private Map<String, Object> formatLayout(LayoutSpec layout) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("mode", layout.getMode());

        if (layout.getDirection() != null) {
            map.put("direction", layout.getDirection());
        }
        if (layout.getType() != null) {
            map.put("type", layout.getType());
        }
        if (layout.getHint() != null) map.put("hint", layout.getHint());

        return map;
    }

    private Map<String, Object> formatBackground(BackgroundSpec bg) {
        Map<String, Object> map = new LinkedHashMap<>();

        if (bg.getColor() != null) map.put("color", bg.getColor());
        if (bg.getTexture() != null) map.put("texture", bg.getTexture());
        if (bg.getPattern() != null) map.put("pattern", bg.getPattern());
        if (bg.getGradient() != null && !bg.getGradient().isEmpty()) {
            map.put("gradient", bg.getGradient());
        }
        if (bg.getShapes() != null && !bg.getShapes().isEmpty()) {
            map.put("shapes", bg.getShapes());
        }

        if (map.isEmpty()) map.put("color", "#FFFFFF");

        return map;
    }

    private Map<String, Object> formatTypography(TypographySpec typo) {
        Map<String, Object> map = new LinkedHashMap<>();

        for (Map.Entry<String, TypeStyle> entry : typo.getStyles().entrySet()) {
            Map<String, Object> style = new LinkedHashMap<>();
            TypeStyle ts = entry.getValue();

            if (ts.getFont() != null) style.put("font", ts.getFont());
            if (ts.getColor() != null) style.put("color", ts.getColor());
            if (ts.getSize() != null) {
                style.put("size", ts.getSize());
                style.put("size_px", ts.getSizePx());
            }
            if (ts.getTextCase() != null) style.put("case", ts.getTextCase());
            if (ts.getTracking() != null) style.put("tracking", ts.getTracking());
            if (ts.getAlign() != null) style.put("align", ts.getAlign());

            map.put(entry.getKey(), style);
        }

        if (map.isEmpty()) {
            Map<String, Object> defaultStyle = new LinkedHashMap<>();
            defaultStyle.put("font", "Sans-Serif");
            defaultStyle.put("size", "medium");
            defaultStyle.put("size_px", 32);
            map.put("default", defaultStyle);
        }

        return map;
    }

    private Map<String, Object> formatColors(ColorsSpec colors) {
        Map<String, Object> map = new LinkedHashMap<>();

        if (colors.getPrimary() != null) map.put("primary", colors.getPrimary());
        if (colors.getSecondary() != null) map.put("secondary", colors.getSecondary());
        if (colors.getText() != null) map.put("text", colors.getText());
        if (colors.getBackground() != null) map.put("background", colors.getBackground());

        if (map.isEmpty()) {
            map.put("note", "Use brand colors or extract from template context");
        }

        return map;
    }

    private Map<String, Object> formatUsageHints(UsageHints hints) {
        Map<String, Object> map = new LinkedHashMap<>();

        map.put("type_hint", hints.getTypeHint());
        if (hints.getLayoutHint() != null) {
            map.put("layout_hint", hints.getLayoutHint());
        }
        map.put("creation_order", hints.getCreationOrder());

        return map;
    }

    private String formatError(String message) {
        return String.format("{\"success\": false, \"error\": %s}", 
            JsonUtils.escapeJson(message));
    }
}