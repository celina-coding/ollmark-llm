package com.penpot.mcp.tools;

import com.penpot.mcp.model.MarketingTemplate;
import com.penpot.mcp.service.MarketingTemplateRagService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.*;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * Outil MCP pour la recherche de templates marketing via RAG.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MarketingTemplateTools {

    private final MarketingTemplateRagService ragService;

    @Tool(description = "Recherche des templates marketing similaires à une description donnée")
    public String searchMarketingTemplates(
        @ToolParam(description = "Description du besoin marketing", required = true) 
        String description,

        @ToolParam(description = "Nombre de résultats à retourner", required = false) 
        Integer limit
    ) {
        try {
            int topK = limit != null ? limit : 3;
            List<MarketingTemplate> templates = ragService.searchTemplates(description, topK, 0.7);

            if (templates.isEmpty()) {
                return "Aucun template trouvé pour cette description.";
            }

            StringBuilder result = new StringBuilder();
            result.append("Templates trouvés:\n\n");

            for (int i = 0; i < templates.size(); i++) {
                MarketingTemplate template = templates.get(i);
                result.append(i + 1).append(". ").append(template.getId()).append("\n");
                result.append("   Type: ").append(template.getType()).append("\n");
                result.append("   Description: ").append(template.getDescription()).append("\n");
                result.append("   Tags: ").append(String.join(", ", template.getTags())).append("\n\n");
            }

            return result.toString();
        } catch (Exception e) {
            log.error("Erreur lors de la recherche de templates", e);
            return "Erreur: " + e.getMessage();
        }
    }

    @Tool(description = "Trouve le meilleur template pour une description spécifique et génère le code Penpot correspondant")
    public String generatePenpotFromTemplate(
        @ToolParam(description = "Description du design souhaité", required = true) 
        String description
    ) {
        try {
            Optional<MarketingTemplate> templateOpt = ragService.findBestTemplate(description);
            if (templateOpt.isEmpty()) return "Aucun template approprié trouvé.";

            MarketingTemplate template = templateOpt.get();
            String penpotCode = generatePenpotCode(template);

            return String.format(
                "Template sélectionné: %s\n\nCode Penpot généré:\n\n%s",
                template.getId(),
                penpotCode
            );
        } catch (Exception e) {
            log.error("Erreur lors de la génération du code Penpot", e);
            return "Erreur: " + e.getMessage();
        }
    }

    /**
     * Génère le code Penpot à partir d'un template.
     */
    private String generatePenpotCode(MarketingTemplate template) {
        StringBuilder code = new StringBuilder();
        MarketingTemplate.DesignRecipe recipe = template.getDesignRecipe();

        code.append("// Création du board pour: ").append(template.getDescription()).append("\n");
        code.append("const board = penpot.createBoard();\n");

        if (recipe.getCanvasSize() != null) {
            String[] dimensions = recipe.getCanvasSize().split("x");
            code.append("board.resize(").append(dimensions[0]).append(", ").append(dimensions[1]).append(");\n");
        }

        code.append("board.name = '").append(template.getId()).append("';\n\n");

        if (recipe.getMainElement() != null) {
            MarketingTemplate.MainElement mainElement = recipe.getMainElement();

            if ("headline_text".equals(mainElement.getType())) {
                code.append("// Texte principal\n");
                code.append("const headline = penpot.createText('Votre titre ici');\n");
                code.append("board.appendChild(headline);\n");

                int fontSize = mapFontSize(mainElement.getFontSize());
                code.append("headline.fontSize = ").append(fontSize).append(";\n");
                code.append("headline.horizontalAlign = '").append(mapAlignment(mainElement.getAlignment())).append("';\n");
                code.append("headline.x = board.width / 2 - headline.width / 2;\n");
                code.append("headline.y = board.height / 2 - headline.height / 2;\n\n");
            }
        }

        // Créer le badge prix si présent
        if (recipe.getPriceBadge() != null) {
            MarketingTemplate.PriceBadge badge = recipe.getPriceBadge();
            code.append("// Badge prix\n");

            if ("circle".equals(badge.getShape())) {
                code.append("const priceBadge = penpot.createEllipse();\n");
                code.append("priceBadge.resize(100, 100);\n");
            } else {
                code.append("const priceBadge = penpot.createRectangle();\n");
                code.append("priceBadge.resize(120, 60);\n");
            }

            code.append("board.appendChild(priceBadge);\n");
            code.append("priceBadge.fills = [{ fillColor: '#FF6B6B', fillOpacity: 1 }];\n");

            if ("top_right".equals(badge.getPosition())) {
                code.append("priceBadge.x = board.width - priceBadge.width - 20;\n");
                code.append("priceBadge.y = 20;\n");
            }

            code.append("\n// Texte du prix\n");
            code.append("const priceText = penpot.createText('2.99€');\n");
            code.append("priceBadge.appendChild(priceText);\n");
            code.append("priceText.fontSize = 24;\n");
            code.append("priceText.fills = [{ fillColor: '#FFFFFF' }];\n");
            code.append("priceText.horizontalAlign = 'center';\n");
        }

        code.append("\nreturn board;");
        return code.toString();
    }

    private int mapFontSize(String size) {
        return switch (size != null ? size : "medium") {
            case "giant" -> 72;
            case "large" -> 48;
            case "medium" -> 32;
            case "small" -> 24;
            default -> 32;
        };
    }

    private String mapAlignment(String alignment) {
        return switch (alignment != null ? alignment : "left") {
            case "center" -> "center";
            case "right" -> "right";
            default -> "left";
        };
    }
}