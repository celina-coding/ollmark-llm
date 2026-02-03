package com.penpot.mcp.application.tools;

import com.penpot.mcp.core.ports.in.ExecuteCodeUseCase;
import com.penpot.mcp.core.domain.*;
import com.penpot.mcp.shared.util.JsonUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.*;
import org.springframework.stereotype.Component;

/**
 * Tools pour les transformations géométriques dans Penpot.
 * 
 * <h2>Responsabilité unique</h2>
 * Gère uniquement les transformations :
 * - Rotation
 * - Mise à l'échelle (scale)
 * - Déplacement (move)
 * - Redimensionnement (resize)
 * 
 * <h2>Design Pattern: Command</h2>
 * Chaque outil encapsule une commande de transformation
 * qui sera exécutée dans Penpot.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PenpotTransformTools {

    private final ExecuteCodeUseCase executeCodeUseCase;

    /**
     * Effectue une rotation d'une forme.
     * 
     * @param shapeId ID de la forme à faire pivoter
     * @param angle Angle de rotation en degrés (positif = horaire)
     * @return JSON avec confirmation de la transformation
     */
    @Tool(description = """
        Rotate a shape by a specified angle in degrees.
        Positive angles rotate clockwise, negative counter-clockwise.

        Examples:
        - "Rotate the rectangle 45 degrees"
        - "Turn the logo 90 degrees clockwise"

        The rotation is applied around the shape's center point.
        """)
    public String rotateShape(
        @ToolParam(description = "ID of the shape to rotate") String shapeId,
        @ToolParam(description = "Rotation angle in degrees (positive = clockwise)") int angle
    ) {
        log.info("Tool called: rotateShape (id={}, angle={}°)", shapeId, angle);

        String code = buildRotateCode(shapeId, angle);

        try {
            TaskResult result = executeCodeUseCase.execute(
                ExecuteCodeCommand.of(code)
            );

            if (!result.isSuccess()) {
                return formatError(result.getError().orElse("Unknown error"));
            }

            return formatSuccess("rotated", shapeId, 
                String.format("Rotated by %.1f degrees", angle));
        } catch (Exception e) {
            log.error("Failed to rotate shape", e);
            return formatError(e.getMessage());
        }
    }

    /**
     * Met une forme à l'échelle (agrandissement/réduction).
     * 
     * @param shapeId ID de la forme
     * @param scaleX Facteur d'échelle horizontal (1.0 = taille originale)
     * @param scaleY Facteur d'échelle vertical (1.0 = taille originale)
     * @return JSON avec confirmation
     */
    @Tool(description = """
        Scale a shape by specified factors on X and Y axes.

        Scale factors:
        - 1.0 = original size
        - 2.0 = double size
        - 0.5 = half size

        Examples:
        - "Make the rectangle twice as big"
        - "Scale the circle to 50%"
        - "Stretch horizontally by 1.5x"

        For uniform scaling, use the same value for both axes.
        """)
    public String scaleShape(
        @ToolParam(description = "ID of the shape to scale") String shapeId,
        @ToolParam(description = "Horizontal scale factor (1.0 = original)") int scaleX,
        @ToolParam(description = "Vertical scale factor (1.0 = original)") int scaleY
    ) {
        log.info("Tool called: scaleShape (id={}, scaleX={}, scaleY={})", 
            shapeId, scaleX, scaleY);

        String code = buildScaleCode(shapeId, scaleX, scaleY);

        try {
            TaskResult result = executeCodeUseCase.execute(
                ExecuteCodeCommand.of(code)
            );

            if (!result.isSuccess()) {
                return formatError(result.getError().orElse("Unknown error"));
            }

            return formatSuccess("scaled", shapeId,
                String.format("Scaled by %.2fx, %.2f", scaleX, scaleY));
        } catch (Exception e) {
            log.error("Failed to scale shape", e);
            return formatError(e.getMessage());
        }
    }

    /**
     * Déplace une forme vers une nouvelle position.
     * 
     * @param shapeId ID de la forme
     * @param newX Nouvelle position X
     * @param newY Nouvelle position Y
     * @param relative Si true, déplacement relatif, sinon absolu
     * @return JSON avec confirmation
     */
    @Tool(description = """
        Move a shape to a new position.

        Position modes:
        - Absolute (relative=false): Move to exact coordinates
        - Relative (relative=true): Move by offset from current position

        Examples:
        - "Move the rectangle to (100, 200)"
        - "Shift the text 50 pixels to the right" (relative)
        - "Move down by 30 pixels" (relative with newX=0, newY=30)
        """)
    public String moveShape(
        @ToolParam(description = "ID of the shape to move") String shapeId,
        @ToolParam(description = "New X position or X offset") int newX,
        @ToolParam(description = "New Y position or Y offset") int newY,
        @ToolParam(description = "If true, move relative to current position", required = false) 
        Boolean relative
    ) {
        log.info("Tool called: moveShape (id={}, x={}, y={}, relative={})", 
            shapeId, newX, newY, relative);

        boolean isRelative = relative != null && relative;
        String code = buildMoveCode(shapeId, newX, newY, isRelative);

        try {
            TaskResult result = executeCodeUseCase.execute(
                ExecuteCodeCommand.of(code)
            );

            if (!result.isSuccess()) {
                return formatError(result.getError().orElse("Unknown error"));
            }

            String action = isRelative 
                ? String.format("Moved by (%.1f, %.1f)", newX, newY)
                : String.format("Moved to (%.1f, %.1f)", newX, newY);

            return formatSuccess("moved", shapeId, action);
        } catch (Exception e) {
            log.error("Failed to move shape", e);
            return formatError(e.getMessage());
        }
    }

    /**
     * Redimensionne une forme à des dimensions spécifiques.
     * 
     * @param shapeId ID de la forme
     * @param newWidth Nouvelle largeur
     * @param newHeight Nouvelle hauteur
     * @return JSON avec confirmation
     */
    @Tool(description = """
        Resize a shape to specific dimensions.

        Examples:
        - "Resize the rectangle to 200x150"
        - "Make the image 500 pixels wide and 300 tall"
        - "Change dimensions to 100x100"

        Note: This sets absolute dimensions, unlike scaleShape which uses factors.
        """)
    public String resizeShape(
        @ToolParam(description = "ID of the shape to resize") String shapeId,
        @ToolParam(description = "New width in pixels") int newWidth,
        @ToolParam(description = "New height in pixels") int newHeight
    ) {
        log.info("Tool called: resizeShape (id={}, w={}, h={})", 
            shapeId, newWidth, newHeight);

        String code = buildResizeCode(shapeId, newWidth, newHeight);

        try {
            TaskResult result = executeCodeUseCase.execute(
                ExecuteCodeCommand.of(code)
            );

            if (!result.isSuccess()) {
                return formatError(result.getError().orElse("Unknown error"));
            }

            return formatSuccess("resized", shapeId,
                String.format("Resized to %.1f x %.1f", newWidth, newHeight));
        } catch (Exception e) {
            log.error("Failed to resize shape", e);
            return formatError(e.getMessage());
        }
    }

    // ==================== CODE GENERATION METHODS ====================

    private String buildRotateCode(String shapeId, int angle) {
        return String.format("""
            const shape = penpot.getShape('%s');
            if (!shape) throw new Error('Shape not found: %s');
            shape.rotation = (shape.rotation || 0) + %d;
            return { id: shape.id, rotation: shape.rotation };
            """,
            shapeId, shapeId, angle
        );
    }

    private String buildScaleCode(String shapeId, int scaleX, int scaleY) {
        return String.format("""
            const shape = penpot.getShape('%s');
            if (!shape) throw new Error('Shape not found: %s');
            const currentWidth = shape.width || 1;
            const currentHeight = shape.height || 1;
            shape.resize(currentWidth * %d, currentHeight * %d);
            return { id: shape.id, width: shape.width, height: shape.height };
            """,
            shapeId, shapeId, scaleX, scaleY
        );
    }

    private String buildMoveCode(String shapeId, int newX, int newY, boolean relative) {
        if (relative) {
            return String.format("""
                const shape = penpot.getShape('%s');
                if (!shape) throw new Error('Shape not found: %s');
                shape.x = (shape.x || 0) + %d;
                shape.y = (shape.y || 0) + %d;
                return { id: shape.id, x: shape.x, y: shape.y };
                """,
                shapeId, shapeId, newX, newY
            );
        } else {
            return String.format("""
                const shape = penpot.getShape('%s');
                if (!shape) throw new Error('Shape not found: %s');
                shape.x = %d;
                shape.y = %d;
                return { id: shape.id, x: shape.x, y: shape.y };
                """,
                shapeId, shapeId, newX, newY
            );
        }
    }

    private String buildResizeCode(String shapeId, int newWidth, int newHeight) {
        return String.format("""
            const shape = penpot.getShape('%s');
            if (!shape) throw new Error('Shape not found: %s');
            shape.resize(%d, %d);
            return { id: shape.id, width: shape.width, height: shape.height };
            """,
            shapeId, shapeId, newWidth, newHeight
        );
    }

    // ==================== RESPONSE FORMATTING ====================

    private String formatSuccess(String operation, String shapeId, String details) {
        return String.format(
            "{\"success\": true, \"operation\": %s, \"shapeId\": %s, \"details\": %s}",
            JsonUtils.escapeJson(operation),
            JsonUtils.escapeJson(shapeId),
            JsonUtils.escapeJson(details)
        );
    }

    private String formatError(String errorMessage) {
        return String.format(
            "{\"success\": false, \"error\": %s}",
            JsonUtils.escapeJson(errorMessage)
        );
    }
}