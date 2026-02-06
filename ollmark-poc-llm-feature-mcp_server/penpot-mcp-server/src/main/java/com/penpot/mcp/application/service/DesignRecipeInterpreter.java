package com.penpot.mcp.application.service;

import com.penpot.mcp.model.MarketingTemplate;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import java.util.*;

/**
 * Service d'interprétation de design recipes.
 * Convertit une "design_recipe" (structure déclarative) en code JavaScript
 * exécutable dans Penpot.
 * 
 * Principe :
 * 1. Analyse la design_recipe du template
 * 2. Identifie les composants à créer (board, shapes, text, etc.)
 * 3. Génère le code JavaScript Penpot correspondant
 * 4. Retourne le code prêt à être exécuté
 */
@Slf4j
@Service
public class DesignRecipeInterpreter {

    /**
     * Interprète une design recipe et génère le code JavaScript Penpot.
     *
     * @param template le template contenant la design_recipe
     * @return le code JavaScript généré
     */
    public String interpretDesignRecipe(MarketingTemplate template) {
        log.info("Interpreting design recipe for template: {}", template.getId());

        Map<String, Object> recipe = template.getDesignRecipe();
        if (recipe == null || recipe.isEmpty()) {
            throw new IllegalArgumentException(
                "Template " + template.getId() + " has no design_recipe"
            );
        }

        StringBuilder code = new StringBuilder();
        Dimensions dimensions = extractDimensions(recipe, template.getType());

        code.append("// Template: ").append(template.getId()).append("\n");
        code.append("// Type: ").append(template.getType()).append("\n");
        code.append("// Generated from design_recipe\n\n");

        code.append(generateBoardCreation(dimensions, template));

        String backgroundMode = getString(recipe, "background_mode", "color");
        code.append(generateBackground(backgroundMode, recipe, dimensions));

        String layoutMode = getString(recipe, "layout_mode", "default");
        code.append(generateLayout(layoutMode, recipe, dimensions));

        code.append(generateTypeSpecificElements(template.getType(), recipe, dimensions));

        code.append("\n// Return the main board\n");
        code.append("return board;\n");

        String generatedCode = code.toString();
        log.debug("Generated {} characters of code", generatedCode.length());

        return generatedCode;
    }

    /**
     * Extrait les dimensions depuis la design_recipe ou utilise des valeurs par défaut.
     */
    private Dimensions extractDimensions(Map<String, Object> recipe, String type) {
        String canvasSize = getString(recipe, "canvas_size", null);
        if (canvasSize != null && canvasSize.contains("x")) {
            String[] parts = canvasSize.split("x");
            return new Dimensions(
                Integer.parseInt(parts[0].trim()),
                Integer.parseInt(parts[1].trim())
            );
        }

        String format = getString(recipe, "format", null);
        if (format != null) return getDimensionsFromFormat(format);

        return getDefaultDimensionsForType(type);
    }

    /**
     * Génère le code de création du board principal.
     */
    private String generateBoardCreation(Dimensions dims, MarketingTemplate template) {
        return String.format(
            "// Create main board\n" +
            "const board = penpot.createBoard();\n" +
            "board.name = '%s';\n" +
            "board.resize(%d, %d);\n\n",
            template.getId().replace("_", " "),
            dims.width,
            dims.height
        );
    }

    /**
     * Génère le code pour le fond (background).
     */
    private String generateBackground(String mode, Map<String, Object> recipe, Dimensions dims) {
        StringBuilder code = new StringBuilder("// Background\n");

        switch (mode) {
            case "full_image":
                code.append("const bgRect = penpot.createRectangle();\n");
                code.append("bgRect.resize(").append(dims.width).append(", ").append(dims.height).append(");\n");
                code.append("bgRect.x = 0;\n");
                code.append("bgRect.y = 0;\n");
                code.append("bgRect.fills = [{ fillColor: '#F0F0F0' }]; // Placeholder for image\n");
                code.append("board.appendChild(bgRect);\n\n");

                String overlay = getString(recipe, "overlay_gradient", null);
                if (overlay != null) code.append(generateOverlay(overlay, dims));
                break;
            case "color":
            case "solid":
                String bgColor = getString(recipe, "background_color", "#FFFFFF");
                code.append("const bgRect = penpot.createRectangle();\n");
                code.append("bgRect.resize(").append(dims.width).append(", ").append(dims.height).append(");\n");
                code.append("bgRect.x = 0;\n");
                code.append("bgRect.y = 0;\n");
                code.append("bgRect.fills = [{ fillColor: '").append(bgColor).append("' }];\n");
                code.append("board.appendChild(bgRect);\n\n");
                break;
            default:
                code.append("board.fills = [{ fillColor: '#FFFFFF' }];\n\n");
        }

        return code.toString();
    }

    /**
     * Génère le code pour un overlay (gradient).
     */
    private String generateOverlay(String overlayType, Dimensions dims) {
        StringBuilder code = new StringBuilder();
        code.append("// Overlay gradient\n");
        code.append("const overlay = penpot.createRectangle();\n");
        code.append("overlay.resize(").append(dims.width).append(", ").append(dims.height).append(");\n");
        code.append("overlay.x = 0;\n");
        code.append("overlay.y = 0;\n");

        if ("bottom_dark".equals(overlayType)) {
            code.append("overlay.fills = [{\n");
            code.append("  fillOpacity: 0.5,\n");
            code.append("  fillColor: '#000000'\n");
            code.append("}];\n");
        }

        code.append("board.appendChild(overlay);\n\n");
        return code.toString();
    }

    /**
     * Génère le code selon le mode de layout.
     */
    private String generateLayout(String layoutMode, Map<String, Object> recipe, Dimensions dims) {
        StringBuilder code = new StringBuilder();

        switch (layoutMode) {
            case "hero_product":
                code.append(generateHeroProductLayout(recipe, dims));
                break;
            case "image_dominant":
                code.append(generateImageDominantLayout(recipe, dims));
                break;
            case "vertical_split":
                code.append(generateVerticalSplitLayout(recipe, dims));
                break;
            case "grid_showcase":
                code.append(generateGridLayout(recipe, dims));
                break;
            default:
                code.append("// Default layout (content area)\n\n");
        }

        return code.toString();
    }

    /**
     * Génère les éléments spécifiques au type de template.
     */
    private String generateTypeSpecificElements(String type, Map<String, Object> recipe, Dimensions dims) {
        StringBuilder code = new StringBuilder();

        switch (type) {
            case "social_media_post":
                code.append(generatePostElements(recipe, dims));
                break;
            case "email":
                code.append(generateEmailElements(recipe, dims));
                break;
            case "poster_a3":
            case "poster_a2":
            case "flyer_a5":
                code.append(generatePosterElements(recipe, dims));
                break;
            default:
                code.append("// Generic elements\n");
                code.append(generateGenericElements(recipe, dims));
        }

        return code.toString();
    }

    /**
     * Génère les éléments pour un post social media.
     */
    private String generatePostElements(Map<String, Object> recipe, Dimensions dims) {
        StringBuilder code = new StringBuilder("// Post elements\n");

        @SuppressWarnings("unchecked")
        Map<String, Object> textBlock = (Map<String, Object>) recipe.get("text_block");
        if (textBlock != null) {
            String position = getString(textBlock, "position", "bottom");
            int yPos = "bottom".equals(position) ? dims.height - 150 : 50;

            code.append("const caption = penpot.createText('Caption text here');\n");
            code.append("caption.x = 50;\n");
            code.append("caption.y = ").append(yPos).append(";\n");
            code.append("caption.resize(").append(dims.width - 100).append(", 100);\n");
            code.append("caption.fontSize = 24;\n");
            code.append("board.appendChild(caption);\n\n");
        }

        return code.toString();
    }

    /**
     * Génère les éléments pour un email.
     */
    private String generateEmailElements(Map<String, Object> recipe, Dimensions dims) {
        StringBuilder code = new StringBuilder("// Email sections\n");

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> sections = (List<Map<String, Object>>) recipe.get("sections");
        if (sections != null) {
            int yOffset = 0;

            for (Map<String, Object> section : sections) {
                String sectionType = getString(section, "type", "text");
                int height = getInt(section, "height", 200);

                code.append("// Section: ").append(sectionType).append("\n");
                code.append("const section_").append(sectionType).append(" = penpot.createRectangle();\n");
                code.append("section_").append(sectionType).append(".x = 0;\n");
                code.append("section_").append(sectionType).append(".y = ").append(yOffset).append(";\n");
                code.append("section_").append(sectionType).append(".resize(").append(dims.width).append(", ").append(height).append(");\n");
                code.append("section_").append(sectionType).append(".fills = [{ fillColor: '#F5F5F5' }];\n");
                code.append("board.appendChild(section_").append(sectionType).append(");\n\n");

                yOffset += height + 20;
            }
        }

        return code.toString();
    }

    /**
     * Génère les éléments pour un poster/flyer.
     */
    private String generatePosterElements(Map<String, Object> recipe, Dimensions dims) {
        return "// Poster elements\n" +
               "const title = penpot.createText('Poster Title');\n" +
               "title.fontSize = 72;\n" +
               "title.fontWeight = 'bold';\n" +
               "title.x = 100;\n" +
               "title.y = 100;\n" +
               "board.appendChild(title);\n\n";
    }

    /**
     * Génère des éléments génériques.
     */
    private String generateGenericElements(Map<String, Object> recipe, Dimensions dims) {
        return "// Generic content placeholder\n" +
               "const placeholder = penpot.createText('Content goes here');\n" +
               "placeholder.x = " + (dims.width / 4) + ";\n" +
               "placeholder.y = " + (dims.height / 2) + ";\n" +
               "placeholder.fontSize = 32;\n" +
               "board.appendChild(placeholder);\n\n";
    }

    // ===== LAYOUTS =====

    private String generateHeroProductLayout(Map<String, Object> recipe, Dimensions dims) {
        return "// Hero product layout\n" +
               "const productImage = penpot.createRectangle();\n" +
               "productImage.resize(" + (dims.width * 0.8) + ", " + (dims.height * 0.6) + ");\n" +
               "productImage.x = " + (dims.width * 0.1) + ";\n" +
               "productImage.y = " + (dims.height * 0.2) + ";\n" +
               "productImage.fills = [{ fillColor: '#E0E0E0' }];\n" +
               "board.appendChild(productImage);\n\n";
    }

    private String generateImageDominantLayout(Map<String, Object> recipe, Dimensions dims) {
        return "// Image dominant layout\n" +
               "const mainImage = penpot.createRectangle();\n" +
               "mainImage.resize(" + dims.width + ", " + (dims.height * 0.75) + ");\n" +
               "mainImage.x = 0;\n" +
               "mainImage.y = 0;\n" +
               "mainImage.fills = [{ fillColor: '#D0D0D0' }];\n" +
               "board.appendChild(mainImage);\n\n";
    }

    private String generateVerticalSplitLayout(Map<String, Object> recipe, Dimensions dims) {
        return "// Vertical split layout\n" +
               "const leftPanel = penpot.createRectangle();\n" +
               "leftPanel.resize(" + (dims.width / 2) + ", " + dims.height + ");\n" +
               "leftPanel.x = 0;\n" +
               "leftPanel.y = 0;\n" +
               "leftPanel.fills = [{ fillColor: '#F0F0F0' }];\n" +
               "board.appendChild(leftPanel);\n\n";
    }

    private String generateGridLayout(Map<String, Object> recipe, Dimensions dims) {
        @SuppressWarnings("unchecked")
        Map<String, Object> grid = (Map<String, Object>) recipe.get("grid");
        int columns = grid != null ? getInt(grid, "columns", 3) : 3;

        StringBuilder code = new StringBuilder("// Grid layout\n");
        int cellWidth = dims.width / columns;
        int cellHeight = dims.height / 2;

        for (int i = 0; i < columns * 2; i++) {
            int col = i % columns;
            int row = i / columns;

            code.append("const gridCell").append(i).append(" = penpot.createRectangle();\n");
            code.append("gridCell").append(i).append(".resize(").append(cellWidth - 20).append(", ").append(cellHeight - 20).append(");\n");
            code.append("gridCell").append(i).append(".x = ").append(col * cellWidth + 10).append(";\n");
            code.append("gridCell").append(i).append(".y = ").append(row * cellHeight + 10).append(";\n");
            code.append("gridCell").append(i).append(".fills = [{ fillColor: '#E8E8E8' }];\n");
            code.append("board.appendChild(gridCell").append(i).append(");\n\n");
        }

        return code.toString();
    }

    private String generatePriceBadge(Map<String, Object> badge, Dimensions dims) {
        String position = getString(badge, "position", "top_right");
        int x = position.contains("right") ? dims.width - 150 : 50;
        int y = position.contains("bottom") ? dims.height - 150 : 50;

        return "// Price badge\n" +
               "const badge = penpot.createEllipse();\n" +
               "badge.resize(120, 120);\n" +
               "badge.x = " + x + ";\n" +
               "badge.y = " + y + ";\n" +
               "badge.fills = [{ fillColor: '#FF4444' }];\n" +
               "board.appendChild(badge);\n" +
               "const badgeText = penpot.createText('Price');\n" +
               "badgeText.x = " + (x + 20) + ";\n" +
               "badgeText.y = " + (y + 45) + ";\n" +
               "badgeText.fontSize = 24;\n" +
               "badgeText.fontWeight = 'bold';\n" +
               "badgeText.fills = [{ fillColor: '#FFFFFF' }];\n" +
               "board.appendChild(badgeText);\n\n";
    }

    // ===== HELPERS =====

    private String getString(Map<String, Object> map, String key, String defaultValue) {
        Object value = map.get(key);
        return value != null ? value.toString() : defaultValue;
    }

    private int getInt(Map<String, Object> map, String key, int defaultValue) {
        Object value = map.get(key);
        if (value instanceof Number) return ((Number) value).intValue();
        return defaultValue;
    }

    private int mapFontSize(String size) {
        return switch (size.toLowerCase()) {
            case "giant" -> 96;
            case "huge" -> 72;
            case "large" -> 48;
            case "medium" -> 32;
            case "small" -> 24;
            default -> 32;
        };
    }

    private Dimensions getDimensionsFromFormat(String format) {
        return switch (format.toLowerCase()) {
            case "square" -> new Dimensions(1080, 1080);
            case "landscape" -> new Dimensions(1200, 630);
            case "portrait" -> new Dimensions(1080, 1350);
            case "carousel" -> new Dimensions(1080, 1080);
            default -> new Dimensions(1080, 1080);
        };
    }

    private Dimensions getDefaultDimensionsForType(String type) {
        return switch (type) {
            case "social_media_post" -> new Dimensions(1080, 1080);
            case "poster_a3" -> new Dimensions(2480, 3508); // 297x420mm à 300dpi
            case "poster_a2" -> new Dimensions(3508, 4961); // 420x594mm à 300dpi
            case "flyer_a5" -> new Dimensions(1748, 2480); // 148x210mm à 300dpi
            case "email" -> new Dimensions(600, 1200);
            default -> new Dimensions(1080, 1080);
        };
    }

    /**
     * Classe interne pour représenter des dimensions.
     */
    @lombok.Data
    @lombok.AllArgsConstructor
    private static class Dimensions {
        int width;
        int height;
    }
}