package com.penpot.mcp.application.tools;

import com.penpot.mcp.core.ports.in.ExecuteCodeUseCase;
import com.penpot.mcp.core.domain.*;
import com.penpot.mcp.shared.util.JsonUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.*;
import org.springframework.stereotype.Component;

/**
 * Service de gestion des outils de contenu pour Penpot.
 * Cette classe expose des fonctionnalités avancées à l'IA pour la création de textes structurés 
 * (titres, sous-titres, paragraphes) et l'intégration de médias externes.
 * 
 * <p>L'architecture repose sur la génération dynamique de scripts JavaScript exécutés 
 * au sein de la sandbox Penpot via le pont WebSocket.</p>
 * 
 *
 * 
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PenpotContentTools {

    private final ExecuteCodeUseCase executeCodeUseCase;

    // Constantes de design
    private static final int FONT_SIZE_H1 = 48;
    private static final int FONT_SIZE_H2 = 32;
    private static final int FONT_SIZE_P = 16;
    private static final String DEFAULT_COLOR = "#000000";
    private static final String WEIGHT_BOLD = "bold";
    private static final String WEIGHT_NORMAL = "normal";

    /**
     * Crée un titre de niveau 1 (H1) imposant.
     * Idéal pour les en-têtes de page ou les titres principaux de sections marketing.
     * 
     * @param content Le contenu textuel du titre.
     * @param x Coordonnée horizontale.
     * @param y Coordonnée verticale.
     * @param color Couleur au format Hexadécimal (ex: #FF0000).
     * @return Un objet JSON contenant le succès de l'opération et l'ID de l'élément créé.
     */
    @Tool(description = "Create a large, bold H1 title. Use this for main headings or hero sections.")
    public String createTitle(
        @ToolParam(description = "The text content for the H1 title") String content,
        @ToolParam(description = "X coordinate position") Integer x,
        @ToolParam(description = "Y coordinate position") Integer y,
        @ToolParam(description = "Hex color code (default: #000000)", required = false) String color
    ) {
        log.info("Generating H1 Title: '{}' at [{},{}]", content, x, y);
        String code = buildTextScript(content, x, y, FONT_SIZE_H1, WEIGHT_BOLD, color, "H1-Title");
        return processTask(code, "title");
    }

    /**
     * Crée un sous-titre de niveau 2 (H2).
     * Utilisé pour structurer le contenu sous les titres principaux.
     */
    @Tool(description = "Create a medium-sized H2 subtitle. Use this for section headers.")
    public String createSubtitle(
        @ToolParam(description = "The text content for the H2 subtitle") String content,
        @ToolParam(description = "X coordinate position") Integer x,
        @ToolParam(description = "Y coordinate position") Integer y,
        @ToolParam(description = "Hex color code (default: #000000)", required = false) String color
    ) {
        log.info("Generating H2 Subtitle: '{}'", content);
        String code = buildTextScript(content, x, y, FONT_SIZE_H2, WEIGHT_BOLD, color, "H2-Subtitle");
        return processTask(code, "subtitle");
    }

    /**
     * Crée un paragraphe de texte standard.
     * Adapté pour le corps de texte, les descriptions ou les légendes.
     */
    @Tool(description = "Create a standard paragraph. Use this for body text, descriptions, or long content.")
    public String createParagraph(
        @ToolParam(description = "The body text content") String content,
        @ToolParam(description = "X coordinate position") Integer x,
        @ToolParam(description = "Y coordinate position") Integer y,
        @ToolParam(description = "Hex color code (default: #333333)", required = false) String color
    ) {
        log.info("Generating Paragraph content");
        String code = buildTextScript(content, x, y, FONT_SIZE_P, WEIGHT_NORMAL, color, "Body-Text");
        return processTask(code, "paragraph");
    }

    /**
     * Importe et affiche une image à partir d'une URL distante.
     * L'opération est asynchrone dans Penpot : l'image est d'abord uploadée dans les assets 
     * du fichier avant d'être appliquée comme remplissage sur une forme rectangulaire.
     * 
     * @param url URL directe de l'image (doit être accessible par le plugin).
     * @param x Coordonnée horizontale.
     * @param y Coordonnée verticale.
     * @param width Largeur souhaitée (300px par défaut).
     * @param height Hauteur souhaitée (200px par défaut).
     * @return Résultat de l'upload et ID du rectangle contenant l'image.
     */
    @Tool(description = "Import an image from a URL. Uploads media to Penpot and creates a frame for it.")
    public String createImage(
        @ToolParam(description = "Public URL of the image") String url,
        @ToolParam(description = "X position") Integer x,
        @ToolParam(description = "Y position") Integer y,
        @ToolParam(description = "Width of image container", required = false) Integer width,
        @ToolParam(description = "Height of image container", required = false) Integer height
    ) {
        log.info("Uploading media from URL: {}", url);
        
        int finalWidth = (width != null) ? width : 300;
        int finalHeight = (height != null) ? height : 200;

        StringBuilder js = new StringBuilder();
        js.append("const page = penpot.currentPage;\n");
        js.append("if (!page) throw new Error('Active page required for image placement');\n");
        js.append(String.format("const imageData = await penpot.uploadMediaUrl('IA-Upload', '%s');\n", url));
        js.append("const rect = penpot.createRectangle();\n");
        js.append(String.format("rect.resize(%d, %d);\n", finalWidth, finalHeight));
        js.append(String.format("rect.x = %d; rect.y = %d;\n", x, y));
        js.append("rect.fills = [{ fillOpacity: 1, fillImage: imageData }];\n");
        js.append("page.appendChild(rect);\n");
        js.append("return rect.id;");

        return processTask(js.toString(), "image");
    }

    // ==================== MÉTHODES PRIVÉES DE GÉNÉRATION ====================

    /**
     * Construit le script JavaScript standardisé pour la création d'objets Text.
     */
    private String buildTextScript(String content, Integer x, Integer y, int fontSize, String weight, String color, String namePrefix) {
        String fillColor = (color != null && !color.isBlank()) ? color : DEFAULT_COLOR;
        String escapedText = content.replace("'", "\\'").replace("\n", "\\n");

        return String.format("""
            const page = penpot.currentPage;
            if (!page) throw new Error('Active page required');
            const text = penpot.createText('%s');
            if (!text) throw new Error('Failed to instantiate text object');
            text.x = %d;
            text.y = %d;
            text.fontSize = %d;
            text.fontWeight = '%s';
            text.fills = [{ fillColor: '%s' }];
            text.name = '%s';
            page.appendChild(text);
            return text.id;
            """, escapedText, x, y, fontSize, weight, fillColor, namePrefix);
    }

    /**
     * Orchestre l'exécution du code via le UseCase et formate la réponse.
     */
    private String processTask(String code, String elementType) {
        try {
            TaskResult result = executeCodeUseCase.execute(ExecuteCodeCommand.of(code));

            if (!result.isSuccess()) {
                String error = result.getError().orElse("Execution failed in Penpot");
                return formatJsonResponse(false, elementType, null, error);
            }

            String elementId = result.getData().map(Object::toString).orElse("unknown");
            return formatJsonResponse(true, elementType, elementId, null);

        } catch (Exception e) {
            log.error("Critical error executing Penpot tool [{}]", elementType, e);
            return formatJsonResponse(false, elementType, null, e.getMessage());
        }
    }

    /**
     * Formate le résultat de l'outil en JSON standardisé.
     */
    private String formatJsonResponse(boolean success, String type, String id, String error) {
        if (success) {
            return String.format("{\"success\": true, \"type\": \"%s\", \"id\": \"%s\"}", type, id);
        } else {
            return String.format("{\"success\": false, \"type\": \"%s\", \"error\": %s}", 
                type, JsonUtils.escapeJson(error));
        }
    }
}
