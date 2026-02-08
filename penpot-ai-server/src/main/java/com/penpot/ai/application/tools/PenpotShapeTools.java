package com.penpot.ai.application.tools;

import com.penpot.ai.core.ports.in.ExecuteCodeUseCase;
import com.penpot.ai.core.domain.*;
import com.penpot.ai.shared.util.JsonUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.*;
import org.springframework.stereotype.Component;

/**
 * Tools pour la création de formes graphiques dans Penpot.
 * Ces tools sont exposés à l'IA via function calling.
 * 
 * <h2>Principe de responsabilité unique</h2>
 * Cette classe gère uniquement la création de formes basiques :
 * rectangle, ellipse, texte, board, group.
 * 
 * <h2>Architecture</h2>
 * - Chaque méthode @Tool génère du code JavaScript Penpot
 * - Exécution déléguée à ExecuteCodeUseCase
 * - Retour de résultat formaté en JSON
 * 
 * @see PenpotLayoutTools pour l'alignement
 * @see PenpotTransformTools pour les transformations
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PenpotShapeTools {

    private final ExecuteCodeUseCase executeCodeUseCase;

    /**
     * Crée un rectangle dans Penpot.
     * 
     * @param x Position X du rectangle
     * @param y Position Y du rectangle
     * @param width Largeur du rectangle
     * @param height Hauteur du rectangle
     * @param fillColor Couleur de remplissage (format hex: #RRGGBB)
     * @param name Nom optionnel du rectangle
     * @return JSON avec l'ID de la forme créée
     */
    @Tool(description = """
        Create a rectangle shape in Penpot.

        CRITICAL: This tool returns a UUID that you MUST use in subsequent operations.
        Save this ID immediately after receiving it!

        Use this when the user wants to create a rectangular element.

        Examples:
        - "Create a red rectangle 100x50"
        - "Add a blue box at position (10, 20)"

        Returns the UUID of the created shape that you must save for later use.
        """)
    public String createRectangle(
        @ToolParam(description = "X position in pixels") Integer x,
        @ToolParam(description = "Y position in pixels") Integer y,
        @ToolParam(description = "Width in pixels") Integer width,
        @ToolParam(description = "Height in pixels") Integer height,
        @ToolParam(description = "Fill color in hex format (#RRGGBB)", required = false) String fillColor,
        @ToolParam(description = "Optional name for the rectangle", required = false) String name
    ) {
        log.info("Tool called: createRectangle (x={}, y={}, w={}, h={}, color={})", 
            x, y, width, height, fillColor);

        String code = buildRectangleCode(x, y, width, height, fillColor, name);

        try {
            TaskResult result = executeCodeUseCase.execute(
                ExecuteCodeCommand.of(code)
            );

            if (!result.isSuccess()) {
                return formatError(result.getError().orElse("Unknown error"));
            }

            log.debug(code);
            String shapeId = result.getData()
                .map(Object::toString)
                .orElse("unknown");

            return formatSuccessWithId("rectangle", shapeId);
        } catch (Exception e) {
            log.error("Failed to create rectangle", e);
            return formatError(e.getMessage());
        }
    }

    /**
     * Crée une ellipse (cercle ou ovale) dans Penpot.
     * 
     * @param x Position X du centre
     * @param y Position Y du centre
     * @param width Largeur
     * @param height Hauteur
     * @param fillColor Couleur de remplissage
     * @param name Nom optionnel
     * @return JSON avec l'ID de la forme créée
     */
    @Tool(description = """
        Create an ellipse (circle or oval) shape in Penpot.

        CRITICAL: Returns a UUID that you MUST save for later operations!

        Use this for circular or oval elements.

        Examples:
        - "Create a circle with radius 50"
        - "Add an oval 100x50"

        For a perfect circle, use the same width and height.
        """)
    public String createEllipse(
        @ToolParam(description = "X position of center in pixels") Integer x,
        @ToolParam(description = "Y position of center in pixels") Integer y,
        @ToolParam(description = "Width in pixels") Integer width,
        @ToolParam(description = "Height in pixels (same as width for circle)") Integer height,
        @ToolParam(description = "Fill color in hex format", required = false) String fillColor,
        @ToolParam(description = "Optional name for the ellipse", required = false) String name
    ) {
        log.info("Tool called: createEllipse (x={}, y={}, w={}, h={})", 
            x, y, width, height);

        String code = buildEllipseCode(x, y, width, height, fillColor, name);

        try {
            TaskResult result = executeCodeUseCase.execute(
                ExecuteCodeCommand.of(code)
            );

            if (!result.isSuccess()) {
                return formatError(result.getError().orElse("Unknown error"));
            }

            log.debug(code);
            String shapeId = result.getData()
                .map(Object::toString)
                .orElse("unknown");

            return formatSuccessWithId("ellipse", shapeId);
        } catch (Exception e) {
            log.error("Failed to create ellipse", e);
            return formatError(e.getMessage());
        }
    }

    /**
     * Crée un élément texte dans Penpot.
     * 
     * @param content Contenu du texte
     * @param x Position X
     * @param y Position Y
     * @param fontSize Taille de la police
     * @param fontWeight Graisse (normal, bold)
     * @param fillColor Couleur du texte
     * @param name Nom optionnel
     * @return JSON avec l'ID du texte créé
     */
    @Tool(description = """
        Create a text element in Penpot.

        CRITICAL: Returns a UUID that you MUST save!

        Use this when the user wants to add text content.

        Examples:
        - "Add text 'Hello World' at position (50, 100)"
        - "Create a title with size 48"

        Font sizes: small=14, medium=18, large=24, xlarge=36, xxlarge=48
        Font weights: normal, bold
        """)
    public String createText(
        @ToolParam(description = "Text content to display") String content,
        @ToolParam(description = "X position in pixels") Integer x,
        @ToolParam(description = "Y position in pixels") Integer y,
        @ToolParam(description = "Font size in pixels (default: 16)", required = false) Integer fontSize,
        @ToolParam(description = "Font weight: normal or bold (default: normal)", required = false) String fontWeight,
        @ToolParam(description = "Text color in hex format (default: #000000)", required = false) String fillColor,
        @ToolParam(description = "Optional name for the text element", required = false) String name
    ) {
        log.info("Tool called: createText (content='{}', x={}, y={})", 
            content, x, y);

        String code = buildTextCode(content, x, y, fontSize, fontWeight, fillColor, name);

        try {
            TaskResult result = executeCodeUseCase.execute(
                ExecuteCodeCommand.of(code)
            );

            if (!result.isSuccess()) {
                return formatError(result.getError().orElse("Unknown error"));
            }

            log.debug(code);
            String shapeId = result.getData()
                .map(Object::toString)
                .orElse("unknown");

            return formatSuccessWithId("text", shapeId);
        } catch (Exception e) {
            log.error("Failed to create text", e);
            return formatError(e.getMessage());
        }
    }

    /**
     * Crée un board (plan de travail) dans Penpot.
     * 
     * @param width Largeur du board
     * @param height Hauteur du board
     * @param name Nom du board
     * @param backgroundColor Couleur de fond
     * @return JSON avec l'ID du board créé
     */
    @Tool(description = """
        Create a board (artboard/canvas) in Penpot.
        Use this as a container for design elements.

        CRITICAL: Returns a UUID that you MUST save!

        Common sizes:
        - Social media post: 1080x1080
        - Instagram story: 1080x1920
        - A4 portrait: 2480x3508 (at 300dpi)
        - Email: 600x1200

        Examples:
        - "Create a board for Instagram post"
        - "Add a canvas 1920x1080"
        """)
    public String createBoard(
        @ToolParam(description = "Board width in pixels") Integer width,
        @ToolParam(description = "Board height in pixels") Integer height,
        @ToolParam(description = "Name of the board") String name,
        @ToolParam(description = "Background color in hex format", required = false) String backgroundColor
    ) {
        log.info("Tool called: createBoard (w={}, h={}, name='{}')", 
            width, height, name);

        String code = buildBoardCode(width, height, name, backgroundColor);

        try {
            TaskResult result = executeCodeUseCase.execute(
                ExecuteCodeCommand.of(code)
            );

            if (!result.isSuccess()) {
                return formatError(result.getError().orElse("Unknown error"));
            }

            log.debug(code);
            String shapeId = result.getData()
                .map(Object::toString)
                .orElse("unknown");

            return formatSuccessWithId("board", shapeId);
        } catch (Exception e) {
            log.error("Failed to create board", e);
            return formatError(e.getMessage());
        }
    }

    // ==================== CODE GENERATION METHODS ====================

    private String buildRectangleCode(
        Integer x,
        Integer y,
        Integer width,
        Integer height, 
        String fillColor,
        String name
    ) {
        StringBuilder code = new StringBuilder();
        code.append("const rect = penpot.createRectangle();\n");
        code.append(String.format("rect.x = %d;\n", x));
        code.append(String.format("rect.y = %d;\n", y));
        code.append(String.format("rect.resize(%d, %d);\n", width, height));

        if (fillColor != null && !fillColor.isBlank()) {
            code.append(String.format("rect.fills = [{ fillColor: '%s' }];\n", fillColor));
        }

        if (name != null && !name.isBlank()) {
            code.append(String.format("rect.name = '%s';\n", 
                name.replace("'", "\\'")));
        }

        code.append("return rect.id;\n");
        return code.toString();
    }

    private String buildEllipseCode(
        Integer x,
        Integer y,
        Integer width,
        Integer height,
        String fillColor,
        String name
    ) {
        StringBuilder code = new StringBuilder();
        code.append("const ellipse = penpot.createEllipse();\n");
        code.append(String.format("ellipse.x = %d;\n", x));
        code.append(String.format("ellipse.y = %d;\n", y));
        code.append(String.format("ellipse.resize(%d, %d);\n", width, height));

        if (fillColor != null && !fillColor.isBlank()) {
            code.append(String.format("ellipse.fills = [{ fillColor: '%s' }];\n", fillColor));
        }

        if (name != null && !name.isBlank()) {
            code.append(String.format("ellipse.name = '%s';\n", 
                name.replace("'", "\\'")));
        }

        code.append("return ellipse.id;\n");
        return code.toString();
    }

    private String buildTextCode(
        String content, Integer x, Integer y,
        Integer fontSize, String fontWeight, String fillColor, String name
    ) {
        StringBuilder code = new StringBuilder();

        String escapedContent = content.replace("'", "\\'")
                                       .replace("\n", "\\n");

        code.append(String.format("const text = penpot.createText('%s');\n", escapedContent));
        code.append(String.format("text.x = %d;\n", x));
        code.append(String.format("text.y = %d;\n", y));

        if (fontSize != null && fontSize > 0) {
            code.append(String.format("text.fontSize = %d;\n", fontSize));
        }

        if (fontWeight != null && !fontWeight.isBlank()) {
            code.append(String.format("text.fontWeight = '%s';\n", fontWeight));
        }

        if (fillColor != null && !fillColor.isBlank()) {
            code.append(String.format("text.fills = [{ fillColor: '%s' }];\n", fillColor));
        }

        if (name != null && !name.isBlank()) {
            code.append(String.format("text.name = '%s';\n", 
                name.replace("'", "\\'")));
        }

        code.append("return text.id;\n");
        return code.toString();
    }

    private String buildBoardCode(
        Integer width, Integer height, String name, String backgroundColor
    ) {
        StringBuilder code = new StringBuilder();
        code.append("const board = penpot.createBoard();\n");
        code.append(String.format("board.resize(%d, %d);\n", width, height));

        if (name != null && !name.isBlank()) {
            code.append(String.format("board.name = '%s';\n", 
                name.replace("'", "\\'")));
        }

        if (backgroundColor != null && !backgroundColor.isBlank()) {
            code.append(String.format("board.fills = [{ fillColor: '%s' }];\n", 
                backgroundColor));
        }

        code.append("return board.id;\n");
        return code.toString();
    }

    /**
     * Format de réponse OPTIMISÉ pour extraction d'ID par l'IA.
     * 
     * Le format est conçu pour que l'IA puisse facilement extraire l'UUID :
     * - ID clairement marqué avec "SHAPE_ID:"
     * - UUID sur une ligne séparée
     * - Instructions explicites pour l'utilisation
     */
    private String formatSuccessWithId(String shapeType, String shapeId) {
        return String.format(
            "%s created successfully!\n\n" +
            "SHAPE_ID: %s\n\n" +
            "SAVE THIS ID! Use it in subsequent operations like:\n" +
            "- alignShapes(shapeIds=\"%s,...\", alignment=\"top\")\n" +
            "- rotateShape(shapeId=\"%s\", angle=45)\n" +
            "- moveShape(shapeId=\"%s\", newX=200, newY=300)",
            shapeType,
            shapeId,
            shapeId,
            shapeId,
            shapeId
        );
    }

    private String formatSuccess(String shapeType, Object data) {
        return String.format(
            "{\"success\": true, \"shapeType\": %s, \"id\": %s}",
            JsonUtils.escapeJson(shapeType),
            data != null ? JsonUtils.escapeJson(data.toString()) : "null"
        );
    }

    private String formatError(String errorMessage) {
        return String.format(
            "{\"success\": false, \"error\": %s}",
            JsonUtils.escapeJson(errorMessage)
        );
    }
}