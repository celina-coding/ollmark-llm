package com.penpot.mcp.application.tools;

import com.penpot.mcp.core.ports.in.ExecuteCodeUseCase;
import com.penpot.mcp.core.domain.*;
import com.penpot.mcp.shared.util.JsonUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.*;
import org.springframework.stereotype.Component;

/**
 * Service de création de contenu pour Penpot (Version Minimaliste).
 * 
 * <p>Cette classe génère des scripts JavaScript ultra-légers pour la création 
 * de textes et d'images, en se basant uniquement sur les fonctions natives 
 * de création de l'objet global 'penpot'.</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PenpotContentTools {

    private final ExecuteCodeUseCase executeCodeUseCase;

    // Constantes de style
    private static final int H1_SIZE = 48;
    private static final int H2_SIZE = 32;
    private static final int P_SIZE = 16;
    private static final String BOLD = "bold";
    private static final String NORMAL = "normal";

    /**
     * Crée un titre de niveau 1 (H1).
     */
    @Tool(description = "Create a large H1 title.")
    public String createTitle(
        @ToolParam(description = "Text content") String content,
        @ToolParam(description = "X coordinate") Integer x,
        @ToolParam(description = "Y coordinate") Integer y,
        @ToolParam(description = "Hex color", required = false) String color
    ) {
        String code = buildTextCode(content, x, y, H1_SIZE, BOLD, color, "Title");
        return process(code, "title");
    }

    /**
     * Crée un sous-titre de niveau 2 (H2).
     */
    @Tool(description = "Create a medium H2 subtitle.")
    public String createSubtitle(
        @ToolParam(description = "Text content") String content,
        @ToolParam(description = "X coordinate") Integer x,
        @ToolParam(description = "Y coordinate") Integer y,
        @ToolParam(description = "Hex color", required = false) String color
    ) {
        String code = buildTextCode(content, x, y, H2_SIZE, BOLD, color, "Subtitle");
        return process(code, "subtitle");
    }

    /**
     * Crée un paragraphe standard.
     */
    @Tool(description = "Create a standard text paragraph.")
    public String createParagraph(
        @ToolParam(description = "Text content") String content,
        @ToolParam(description = "X coordinate") Integer x,
        @ToolParam(description = "Y coordinate") Integer y,
        @ToolParam(description = "Hex color", required = false) String color
    ) {
        String code = buildTextCode(content, x, y, P_SIZE, NORMAL, color, "Paragraph");
        return process(code, "paragraph");
    }

    /**
     * Importe une image depuis une URL et l'affiche dans un rectangle.
     */
    @Tool(description = "Create an image from a URL.")
    public String createImage(
        @ToolParam(description = "Image URL") String url,
        @ToolParam(description = "X position") Integer x,
        @ToolParam(description = "Y position") Integer y,
        @ToolParam(description = "Width", required = false) Integer width,
        @ToolParam(description = "Height", required = false) Integer height
    ) {
        int w = (width != null) ? width : 300;
        int h = (height != null) ? height : 200;

        StringBuilder code = new StringBuilder();
        // Upload asynchrone et création directe
        code.append(String.format("const imageData = await penpot.uploadMediaUrl('IA-Upload', '%s');\n", url));
        code.append("const rect = penpot.createRectangle();\n");
        code.append(String.format("rect.resize(%d, %d);\n", w, h));
        code.append(String.format("rect.x = %d;\n", x));
        code.append(String.format("rect.y = %d;\n", y));
        code.append("rect.fills = [{ fillOpacity: 1, fillImage: imageData }];\n");
        code.append("return rect.id;");

        return process(code.toString(), "image");
    }

    /**
     * Génère le code JavaScript minimaliste pour un élément texte.
     */
    private String buildTextCode(
        String content, Integer x, Integer y,
        Integer fontSize, String fontWeight, String fillColor, String name
    ) {
        StringBuilder code = new StringBuilder();
        String escapedContent = content.replace("'", "\\'").replace("\n", "\\n");

        code.append(String.format("const text = penpot.createText('%s');\n", escapedContent));
        code.append(String.format("text.x = %d;\n", x));
        code.append(String.format("text.y = %d;\n", y));

        if (fontSize != null) code.append(String.format("text.fontSize = %d;\n", fontSize));
        if (fontWeight != null) code.append(String.format("text.fontWeight = '%s';\n", fontWeight));
        if (fillColor != null) code.append(String.format("text.fills = [{ fillColor: '%s' }];\n", fillColor));
        if (name != null) code.append(String.format("text.name = '%s';\n", name.replace("'", "\\'")));

        code.append("return text.id;\n");
        return code.toString();
    }

    /**
     * Exécute le code généré.
     */
    private String process(String code, String type) {
        try {
            TaskResult result = executeCodeUseCase.execute(ExecuteCodeCommand.of(code));
            if (!result.isSuccess()) {
                return String.format("{\"success\": false, \"error\": \"%s\"}", 
                    result.getError().orElse("Execution failed"));
            }
            String shapeId = result.getData().map(Object::toString).orElse("unknown");
            return String.format("{\"success\": true, \"type\": \"%s\", \"id\": \"%s\"}", 
                type, shapeId);
        } catch (Exception e) {
            return String.format("{\"success\": false, \"error\": \"%s\"}", e.getMessage());
        }
    }
}



































// package com.penpot.mcp.application.tools;

// import com.penpot.mcp.core.ports.in.ExecuteCodeUseCase;
// import com.penpot.mcp.core.domain.*;
// import com.penpot.mcp.shared.util.JsonUtils;
// import lombok.RequiredArgsConstructor;
// import lombok.extern.slf4j.Slf4j;
// import org.springframework.ai.tool.annotation.*;
// import org.springframework.stereotype.Component;

// /**
//  * Service de gestion des outils de contenu pour Penpot.
//  * Cette classe expose des fonctionnalités avancées à l'IA pour la création de textes structurés 
//  * (titres, sous-titres, paragraphes) et l'intégration de médias externes.
//  * 
//  * <p>L'architecture repose sur la génération dynamique de scripts JavaScript exécutés 
//  * au sein de la sandbox Penpot via le pont WebSocket.</p>
//  * 
//  *
//  * 
//  */
// @Slf4j
// @Component
// @RequiredArgsConstructor
// public class PenpotContentTools {

//     private final ExecuteCodeUseCase executeCodeUseCase;

//     // Constantes de design
//     private static final int FONT_SIZE_H1 = 48;
//     private static final int FONT_SIZE_H2 = 32;
//     private static final int FONT_SIZE_P = 16;
//     private static final String DEFAULT_COLOR = "#000000";
//     private static final String WEIGHT_BOLD = "bold";
//     private static final String WEIGHT_NORMAL = "normal";

//     /**
//      * Crée un titre de niveau 1 (H1) imposant.
//      * 
//      * @param content Le contenu textuel du titre.
//      * @param x Coordonnée horizontale.
//      * @param y Coordonnée verticale.
//      * @param color Couleur au format Hexadécimal (ex: #FF0000).
//      * @return Un objet JSON contenant le succès de l'opération et l'ID de l'élément créé.
//      */
//     @Tool(description = "Create a large, bold H1 title. Use this for main headings or hero sections.")
//     public String createTitle(
//         @ToolParam(description = "The text content for the H1 title") String content,
//         @ToolParam(description = "X coordinate position") Integer x,
//         @ToolParam(description = "Y coordinate position") Integer y,
//         @ToolParam(description = "Hex color code (default: #000000)", required = false) String color
//     ) {
//         log.info("Generating H1 Title: '{}' at [{},{}]", content, x, y);
//         String code = buildTextScript(content, x, y, FONT_SIZE_H1, WEIGHT_BOLD, color, "H1-Title");
//         return processTask(code, "title");
//     }

//     /**
//      * Crée un sous-titre de niveau 2 (H2).
//      * 
//      */
//     @Tool(description = "Create a medium-sized H2 subtitle. Use this for section headers.")
//     public String createSubtitle(
//         @ToolParam(description = "The text content for the H2 subtitle") String content,
//         @ToolParam(description = "X coordinate position") Integer x,
//         @ToolParam(description = "Y coordinate position") Integer y,
//         @ToolParam(description = "Hex color code (default: #000000)", required = false) String color
//     ) {
//         log.info("Generating H2 Subtitle: '{}'", content);
//         String code = buildTextScript(content, x, y, FONT_SIZE_H2, WEIGHT_BOLD, color, "H2-Subtitle");
//         return processTask(code, "subtitle");
//     }

//     /**
//      * Crée un paragraphe de texte standard.
//      * Adapté pour le corps de texte, les descriptions ou les légendes.
//      */
//     @Tool(description = "Create a standard paragraph. Use this for body text, descriptions, or long content.")
//     public String createParagraph(
//         @ToolParam(description = "The body text content") String content,
//         @ToolParam(description = "X coordinate position") Integer x,
//         @ToolParam(description = "Y coordinate position") Integer y,
//         @ToolParam(description = "Hex color code (default: #333333)", required = false) String color
//     ) {
//         log.info("Generating Paragraph content");
//         String code = buildTextScript(content, x, y, FONT_SIZE_P, WEIGHT_NORMAL, color, "Body-Text");
//         return processTask(code, "paragraph");
//     }

//     /**
//      * Importe et affiche une image à partir d'une URL distante.
//      * L'opération est asynchrone dans Penpot : l'image est d'abord uploadée dans les assets 
//      * du fichier avant d'être appliquée comme remplissage sur une forme rectangulaire.
//      * 
//      * @param url URL directe de l'image (doit être accessible par le plugin).
//      * @param x Coordonnée horizontale.
//      * @param y Coordonnée verticale.
//      * @param width Largeur souhaitée (300px par défaut).
//      * @param height Hauteur souhaitée (200px par défaut).
//      * @return Résultat de l'upload et ID du rectangle contenant l'image.
//      */
//     @Tool(description = "Import an image from a URL. Uploads media to Penpot and creates a frame for it.")
//     public String createImage(
//         @ToolParam(description = "Public URL of the image") String url,
//         @ToolParam(description = "X position") Integer x,
//         @ToolParam(description = "Y position") Integer y,
//         @ToolParam(description = "Width of image container", required = false) Integer width,
//         @ToolParam(description = "Height of image container", required = false) Integer height
//     ) {
//         log.info("Uploading media from URL: {}", url);
        
//         int finalWidth = (width != null) ? width : 300;
//         int finalHeight = (height != null) ? height : 200;

//         StringBuilder js = new StringBuilder();
//         js.append("const page = penpot.currentPage;\n");
//         js.append("if (!page) throw new Error('Active page required for image placement');\n");
//         js.append(String.format("const imageData = await penpot.uploadMediaUrl('IA-Upload', '%s');\n", url));
//         js.append("const rect = penpot.createRectangle();\n");
//         js.append(String.format("rect.resize(%d, %d);\n", finalWidth, finalHeight));
//         js.append(String.format("rect.x = %d; rect.y = %d;\n", x, y));
//         js.append("rect.fills = [{ fillOpacity: 1, fillImage: imageData }];\n");
//         js.append("page.appendChild(rect);\n");
//         js.append("return rect.id;");

//         return processTask(js.toString(), "image");
//     }

//     // ==================== MÉTHODES PRIVÉES DE GÉNÉRATION ====================

//     /**
//      * Construit le script JavaScript standardisé pour la création d'objets Text.
//      */
//     private String buildTextScript(String content, Integer x, Integer y, int fontSize, String weight, String color, String namePrefix) {
//         String fillColor = (color != null && !color.isBlank()) ? color : DEFAULT_COLOR;
//         String escapedText = content.replace("'", "\\'").replace("\n", "\\n");

//         return String.format("""
//             const page = penpot.currentPage;
//             if (!page) throw new Error('Active page required');
//             const text = penpot.createText('%s');
//             if (!text) throw new Error('Failed to instantiate text object');
//             text.x = %d;
//             text.y = %d;
//             text.fontSize = %d;
//             text.fontWeight = '%s';
//             text.fills = [{ fillColor: '%s' }];
//             text.name = '%s';
//             page.appendChild(text);
//             return text.id;
//             """, escapedText, x, y, fontSize, weight, fillColor, namePrefix);
//     }

//     /**
//      * Orchestre l'exécution du code via le UseCase et formate la réponse.
//      */
//     private String processTask(String code, String elementType) {
//         try {
//             TaskResult result = executeCodeUseCase.execute(ExecuteCodeCommand.of(code));

//             if (!result.isSuccess()) {
//                 String error = result.getError().orElse("Execution failed in Penpot");
//                 return formatJsonResponse(false, elementType, null, error);
//             }

//             String elementId = result.getData().map(Object::toString).orElse("unknown");
//             return formatJsonResponse(true, elementType, elementId, null);

//         } catch (Exception e) {
//             log.error("Critical error executing Penpot tool [{}]", elementType, e);
//             return formatJsonResponse(false, elementType, null, e.getMessage());
//         }
//     }

//     /**
//      * Formate le résultat de l'outil en JSON standardisé.
//      */
//     private String formatJsonResponse(boolean success, String type, String id, String error) {
//         if (success) {
//             return String.format("{\"success\": true, \"type\": \"%s\", \"id\": \"%s\"}", type, id);
//         } else {
//             return String.format("{\"success\": false, \"type\": \"%s\", \"error\": %s}", 
//                 type, JsonUtils.escapeJson(error));
//         }
//     }
// }
