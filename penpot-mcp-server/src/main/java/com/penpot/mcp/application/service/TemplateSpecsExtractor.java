package com.penpot.mcp.application.service;

import com.penpot.mcp.model.MarketingTemplate;
import com.penpot.mcp.core.domain.spec.*;
import com.penpot.mcp.core.domain.TemplateSpecs;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import java.util.*;

/**
 * Service d'extraction des spécifications de design depuis les templates.
 */
@Slf4j
@Service
public class TemplateSpecsExtractor {

    /**
     * Extrait toutes les spécifications d'un template.
     * 
     * @param template le template source
     * @return objet de spécifications structurées
     */
    public TemplateSpecs extractSpecs(MarketingTemplate template) {
        log.debug("Extracting specs from template: {}", template.getId());

        Map<String, Object> recipe = template.getDesignRecipe();
        if (recipe == null || recipe.isEmpty()) {
            throw new IllegalArgumentException(
                "Template " + template.getId() + " has no design_recipe"
            );
        }

        return TemplateSpecs.builder()
            .templateId(template.getId())
            .type(template.getType())
            .description(template.getDescription())
            .tags(template.getTags())
            .dimensions(extractDimensions(recipe, template.getType()))
            .layout(extractLayout(recipe))
            .background(extractBackground(recipe))
            .typography(extractTypography(recipe))
            .colors(extractColors(recipe))
            .elements(extractElements(recipe))
            .sections(extractSections(recipe))
            .usageHints(generateUsageHints(template, recipe))
            .build();
    }

    // ==================== EXTRACTION METHODS ====================

    private DimensionsSpec extractDimensions(Map<String, Object> recipe, String type) {
        String canvasSize = getString(recipe, "canvas_size", null);
        if (canvasSize != null && canvasSize.contains("x")) {
            String[] parts = canvasSize.split("x");
            return DimensionsSpec.builder()
                .width(Integer.parseInt(parts[0].trim()))
                .height(Integer.parseInt(parts[1].trim()))
                .canvasSize(canvasSize)
                .format(getString(recipe, "format", null))
                .source("explicit")
                .build();
        }

        int[] defaultDims = getDefaultDimensionsForType(type);
        return DimensionsSpec.builder()
            .width(defaultDims[0])
            .height(defaultDims[1])
            .format(getString(recipe, "format", null))
            .source("default_from_type")
            .build();
    }

    private LayoutSpec extractLayout(Map<String, Object> recipe) {
        String mode = getString(recipe, "layout_mode", "default");
        return LayoutSpec.builder()
            .mode(mode)
            .direction(getString(recipe, "layout_direction", null))
            .type(getString(recipe, "layout_type", null))
            .hint(getLayoutHint(mode))
            .build();
    }

    private BackgroundSpec extractBackground(Map<String, Object> recipe) {
        Object bgObj = recipe.get("background");
        BackgroundSpec.BackgroundSpecBuilder builder = BackgroundSpec.builder();

        if (bgObj instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> bgMap = (Map<String, Object>) bgObj;

            builder.color(getString(bgMap, "color", null))
                   .texture(getString(bgMap, "texture", null))
                   .pattern(getString(bgMap, "pattern", null));

            Object gradient = bgMap.get("gradient");
            if (gradient instanceof List) {
                @SuppressWarnings("unchecked")
                List<String> gradientList = (List<String>) gradient;
                builder.gradient(gradientList);
            }

            Object shapes = bgMap.get("shapes");
            if (shapes instanceof List) {
                @SuppressWarnings("unchecked")
                List<String> shapesList = (List<String>) shapes;
                builder.shapes(shapesList);
            }
        } else {
            String bgColorDirect = getString(recipe, "background_color", "#FFFFFF");
            builder.color(bgColorDirect);
        }

        return builder.build();
    }

    private TypographySpec extractTypography(Map<String, Object> recipe) {
        Object typoObj = recipe.get("typography");
        Map<String, TypeStyle> styles = new HashMap<>();

        if (typoObj instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> typoMap = (Map<String, Object>) typoObj;

            for (Map.Entry<String, Object> entry : typoMap.entrySet()) {
                if (entry.getValue() instanceof Map) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> styleMap = (Map<String, Object>) entry.getValue();

                    String size = getString(styleMap, "size", "medium");
                    styles.put(entry.getKey(), TypeStyle.builder()
                        .font(getString(styleMap, "font", null))
                        .color(getString(styleMap, "color", null))
                        .size(size)
                        .sizePx(mapFontSize(size))
                        .textCase(getString(styleMap, "case", null))
                        .tracking(getString(styleMap, "tracking", null))
                        .align(getString(styleMap, "align", null))
                        .build());
                }
            }
        }

        return TypographySpec.builder()
            .styles(styles)
            .build();
    }

    private ColorsSpec extractColors(Map<String, Object> recipe) {
        ColorsSpec.ColorsSpecBuilder builder = ColorsSpec.builder();

        builder.primary(getString(recipe, "primary_color", null))
               .secondary(getString(recipe, "secondary_color", null))
               .text(getString(recipe, "text_color", null));

        Object bgObj = recipe.get("background");
        if (bgObj instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> bgMap = (Map<String, Object>) bgObj;
            builder.background(getString(bgMap, "color", null));
        }

        return builder.build();
    }

    private List<Map<String, Object>> extractElements(Map<String, Object> recipe) {
        List<Map<String, Object>> elements = new ArrayList<>();

        Object elementsObj = recipe.get("elements");
        if (elementsObj instanceof List) {
            @SuppressWarnings("unchecked")
            List<Object> elementsList = (List<Object>) elementsObj;

            for (Object elem : elementsList) {
                if (elem instanceof Map) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> elemMap = (Map<String, Object>) elem;
                    elements.add(elemMap);
                }
            }
        }

        Object heroImage = recipe.get("hero_image");
        if (heroImage instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> heroMap = (Map<String, Object>) heroImage;
            elements.add(heroMap);
        }

        return elements;
    }

    private List<Map<String, Object>> extractSections(Map<String, Object> recipe) {
        Object sectionsObj = recipe.get("sections");

        if (sectionsObj instanceof List) {
            @SuppressWarnings("unchecked")
            List<Object> sectionsList = (List<Object>) sectionsObj;

            List<Map<String, Object>> sections = new ArrayList<>();
            for (Object section : sectionsList) {
                if (section instanceof Map) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> sectionMap = (Map<String, Object>) section;
                    sections.add(sectionMap);
                }
            }
            return sections;
        }

        return Collections.emptyList();
    }

    private UsageHints generateUsageHints(MarketingTemplate template, Map<String, Object> recipe) {
        String layoutMode = getString(recipe, "layout_mode", null);

        return UsageHints.builder()
            .typeHint(getTypeSpecificHint(template.getType()))
            .layoutHint(layoutMode != null ? getLayoutHint(layoutMode) : null)
            .creationOrder(Arrays.asList(
                "1. Create board with dimensions",
                "2. Apply background (color/gradient/pattern)",
                "3. Add main visual elements (images, shapes)",
                "4. Add text elements with typography specs",
                "5. Apply alignment and layout",
                "6. Add decorative elements (badges, icons)"
            ))
            .build();
    }

    // ==================== HELPER METHODS ====================

    private String getString(Map<String, Object> map, String key, String defaultValue) {
        Object value = map.get(key);
        return value != null ? value.toString() : defaultValue;
    }

    private int mapFontSize(String size) {
        return switch (size.toLowerCase()) {
            case "giant" -> 96;
            case "huge" -> 72;
            case "large" -> 48;
            case "medium" -> 32;
            case "small" -> 24;
            case "tiny" -> 16;
            default -> 32;
        };
    }

    private int[] getDefaultDimensionsForType(String type) {
        return switch (type.toLowerCase()) {
            case "social_media_post", "social_media_square" -> new int[]{1080, 1080};
            case "email" -> new int[]{600, 1200};
            case "flyer_a5" -> new int[]{1748, 2480};
            case "poster_a3", "poster_a4" -> new int[]{2480, 3508};
            case "card_landscape" -> new int[]{1200, 630};
            default -> new int[]{1080, 1080};
        };
    }

    private String getTypeSpecificHint(String type) {
        return switch (type.toLowerCase()) {
            case "social_media_post", "social_media_square" -> 
                "Square format (1080x1080). Start with createBoard, use bold colors and large text for mobile visibility.";
            case "email" -> 
                "Email format (600px width recommended). Use tables/sections. Keep content above 1200px for preview.";
            case "flyer_a5" -> 
                "A5 flyer (1748x2480 @ 300dpi). High contrast for visibility. Include clear call-to-action.";
            case "poster_a3", "poster_a4" -> 
                "Poster format. Large readable text from distance. Bold visuals. Clear hierarchy.";
            default -> 
                "Use dimensions and colors from specs. Follow layout mode guidelines.";
        };
    }

    private String getLayoutHint(String layoutMode) {
        return switch (layoutMode.toLowerCase()) {
            case "centered_object", "centered_quote", "centered_formal" -> 
                "Center main element. Use symmetry. Leave breathing space around center.";
            case "split_horizontal_60_40", "split_vertical_60_40" -> 
                "Split layout 60/40. Larger area for main visual, smaller for text/info.";
            case "full_image_background" -> 
                "Full-bleed background image. Add dark overlay for text readability.";
            case "grid_2x2", "grid_4" -> 
                "Grid layout. Equal-sized cells. Use padding between elements.";
            case "explosion_center", "central_explosion" -> 
                "Radial burst design. Center focal point. Dynamic outward energy.";
            case "list_view", "vertical_stack", "top_down_stack" -> 
                "Vertical stacking. Clear hierarchy. Proper spacing between sections.";
            default -> 
                "Follow standard layout principles. Maintain visual hierarchy.";
        };
    }
}