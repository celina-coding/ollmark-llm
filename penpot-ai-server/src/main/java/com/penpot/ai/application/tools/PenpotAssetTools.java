package com.penpot.ai.application.tools;

import com.penpot.ai.core.ports.in.ExecuteCodeUseCase;
import com.penpot.ai.core.domain.*;
import com.penpot.ai.shared.util.JsonUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.*;
import org.springframework.stereotype.Component;

/**
 * Tools pour la gestion des assets et styles dans Penpot.
 * 
 * <h2>Responsabilité unique</h2>
 * Gère uniquement les opérations sur les assets :
 * - Upload d'images
 * - Création de gradients
 * - Gestion des couleurs et remplissages
 * - Application de styles
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PenpotAssetTools {

    private final ExecuteCodeUseCase executeCodeUseCase;

    /**
     * Applique une couleur de remplissage à une forme.
     * 
     * @param shapeId ID de la forme
     * @param fillColor Couleur en format hex (#RRGGBB)
     * @param opacity Opacité (0.0 à 1.0)
     * @return JSON avec confirmation
     */
    @Tool(description = """
        Apply a solid color fill to a shape.

        Color format: Hexadecimal #RRGGBB
        Examples: #FF0000 (red), #00FF00 (green), #0000FF (blue)

        Opacity: 0.0 (transparent) to 1.0 (opaque)

        Examples:
        - "Fill the rectangle with red"
        - "Make the circle blue with 50% opacity"
        - "Change background to #F0F0F0"
        """)
    public String applyFillColor(
        @ToolParam(description = "ID of the shape to fill") String shapeId,
        @ToolParam(description = "Fill color in hex format (#RRGGBB)") String fillColor,
        @ToolParam(description = "Opacity from 0.0 to 1.0 (default: 1.0)", required = false) 
        Float opacity
    ) {
        log.info("Tool called: applyFillColor (id={}, color={}, opacity={})", 
            shapeId, fillColor, opacity);

        float finalOpacity = (opacity != null && opacity >= 0.0f && opacity <= 1.0f)
            ? opacity : 1.0f;


        String code = buildFillColorCode(shapeId, fillColor, finalOpacity);

        try {
            TaskResult result = executeCodeUseCase.execute(
                ExecuteCodeCommand.of(code)
            );

            if (!result.isSuccess()) {
                return formatError(result.getError().orElse("Unknown error"));
            }

            return formatSuccess("fillApplied", shapeId,
                String.format("Applied %s with opacity %.2f", fillColor, finalOpacity));
        } catch (Exception e) {
            log.error("Failed to apply fill color", e);
            return formatError(e.getMessage());
        }
    }

    /**
     * Crée et applique un gradient linéaire à une forme.
     * 
     * @param shapeId ID de la forme
     * @param startColor Couleur de début
     * @param endColor Couleur de fin
     * @param angle Angle du gradient en degrés (0 = gauche à droite)
     * @return JSON avec confirmation
     */
    @Tool(description = """
        Apply a linear gradient fill to a shape.

        Gradient direction (angle):
        - 0° = left to right (horizontal)
        - 90° = top to bottom (vertical)
        - 45° = diagonal top-left to bottom-right

        Examples:
        - "Add a gradient from blue to green"
        - "Create a vertical gradient from white to black"
        - "Apply diagonal gradient #FF6B6B to #4ECDC4"

        Colors in hex format (#RRGGBB).
        """)
    public String applyGradient(
        @ToolParam(description = "ID of the shape") String shapeId,
        @ToolParam(description = "Start color in hex format") String startColor,
        @ToolParam(description = "End color in hex format") String endColor,
        @ToolParam(description = "Gradient angle in degrees (default: 0)", required = false) 
        Float angle
    ) {
        log.info("Tool called: applyGradient (id={}, start={}, end={}, angle={})", 
            shapeId, startColor, endColor, angle);

        float finalAngle = (angle != null) ? angle : 0.0f;
        String code = buildGradientCode(shapeId, startColor, endColor, finalAngle);

        try {
            TaskResult result = executeCodeUseCase.execute(
                ExecuteCodeCommand.of(code)
            );

            if (!result.isSuccess()) {
                return formatError(result.getError().orElse("Unknown error"));
            }

            return formatSuccess("gradientApplied", shapeId,
                String.format("Applied gradient %s → %s at %.0f°", 
                    startColor, endColor, finalAngle));
        } catch (Exception e) {
            log.error("Failed to apply gradient", e);
            return formatError(e.getMessage());
        }
    }

    /**
     * Applique un contour (stroke) à une forme.
     * 
     * @param shapeId ID de la forme
     * @param strokeColor Couleur du contour
     * @param strokeWidth Largeur du contour en pixels
     * @return JSON avec confirmation
     */
    @Tool(description = """
        Apply a stroke (border/outline) to a shape.

        Examples:
        - "Add a 2px black border"
        - "Create a thick red outline"
        - "Apply 1px stroke color #333333"

        Stroke width: Thickness in pixels (typical: 1-5px)
        """)
    public String applyStroke(
        @ToolParam(description = "ID of the shape") String shapeId,
        @ToolParam(description = "Stroke color in hex format") String strokeColor,
        @ToolParam(description = "Stroke width in pixels (default: 1)", required = false) 
        Float strokeWidth
    ) {
        log.info("Tool called: applyStroke (id={}, color={}, width={})", 
            shapeId, strokeColor, strokeWidth);

        float finalWidth = (strokeWidth != null && strokeWidth > 0) ? strokeWidth : 1.0f;

        String code = buildStrokeCode(shapeId, strokeColor, finalWidth);

        try {
            TaskResult result = executeCodeUseCase.execute(
                ExecuteCodeCommand.of(code)
            );

            if (!result.isSuccess()) {
                return formatError(result.getError().orElse("Unknown error"));
            }

            return formatSuccess("strokeApplied", shapeId,
                String.format("Applied %.1fpx %s stroke", finalWidth, strokeColor));
        } catch (Exception e) {
            log.error("Failed to apply stroke", e);
            return formatError(e.getMessage());
        }
    }

    /**
     * Applique une ombre portée à une forme.
     * 
     * @param shapeId ID de la forme
     * @param offsetX Décalage horizontal
     * @param offsetY Décalage vertical
     * @param blur Rayon de flou
     * @param shadowColor Couleur de l'ombre
     * @return JSON avec confirmation
     */
    @Tool(description = """
        Apply a drop shadow effect to a shape.

        Parameters:
        - offsetX/Y: Shadow offset in pixels (positive = right/down)
        - blur: Blur radius in pixels (0 = sharp, higher = softer)
        - color: Shadow color (typically black or gray)

        Examples:
        - "Add a subtle shadow to the button"
        - "Create a drop shadow 5px down, 3px blur"
        - "Apply black shadow with offset (2, 2)"

        Common presets:
        - Subtle: offset (2, 2), blur 4, color #00000033
        - Medium: offset (4, 4), blur 8, color #00000066
        - Strong: offset (8, 8), blur 16, color #000000AA
        """)
    public String applyShadow(
        @ToolParam(description = "ID of the shape") String shapeId,
        @ToolParam(description = "Horizontal shadow offset in pixels") float offsetX,
        @ToolParam(description = "Vertical shadow offset in pixels") float offsetY,
        @ToolParam(description = "Shadow blur radius in pixels") float blur,
        @ToolParam(description = "Shadow color in hex format (with alpha: #RRGGBBAA)", 
                   required = false) 
        String shadowColor
    ) {
        log.info("Tool called: applyShadow (id={}, offset=({}, {}), blur={}, color={})", 
            shapeId, offsetX, offsetY, blur, shadowColor);

        String finalColor = (shadowColor != null && !shadowColor.isBlank()) 
            ? shadowColor : "#00000066";

        String code = buildShadowCode(shapeId, offsetX, offsetY, blur, finalColor);

        try {
            TaskResult result = executeCodeUseCase.execute(
                ExecuteCodeCommand.of(code)
            );

            if (!result.isSuccess()) {
                return formatError(result.getError().orElse("Unknown error"));
            }

            return formatSuccess("shadowApplied", shapeId,
                String.format("Applied shadow offset(%.1f, %.1f) blur %.1f", 
                    offsetX, offsetY, blur));
        } catch (Exception e) {
            log.error("Failed to apply shadow", e);
            return formatError(e.getMessage());
        }
    }

    // ==================== CODE GENERATION METHODS ====================

    private String buildFillColorCode(String shapeId, String fillColor, float opacity) {
        return String.format("""
            let shape = null;
            try {
                shape = penpot.currentPage.getShapeById('%s');
            } catch (e) {
                console.log('[Fill] Invalid ID, using first selected shape');
                if (penpot.selection.length > 0) {
                    shape = penpot.selection[0];
                }
            }

            if (!shape) {
                throw new Error('Shape not found. ID: %s. Please select a shape.');
            }

            shape.fills = [{
              fillColor: '%s',
              fillOpacity: %.2f
            }];
            return { id: shape.id, fill: '%s', opacity: %.2f };
            """,
            shapeId, shapeId, fillColor, opacity, fillColor, opacity
        );
    }

    private String buildGradientCode(
        String shapeId,
        String startColor, 
        String endColor,
        float angle
    ) {
        float radians = (float) Math.toRadians(angle);
        float endX = (float) Math.cos(radians);
        float endY = (float) Math.sin(radians);

        return String.format("""
            let shape = null;
            try {
                shape = penpot.currentPage.getShapeById('%s');
            } catch (e) {
                console.log('[Gradient] Invalid ID, using first selected shape');
                if (penpot.selection.length > 0) {
                    shape = penpot.selection[0];
                }
            }

            if (!shape) {
                throw new Error('Shape not found. ID: %s');
            }

            shape.fills = [{
              fillColorGradient: {
                type: 'linear',
                startX: 0,
                startY: 0,
                endX: %.2f,
                endY: %.2f,
                stops: [
                  { color: '%s', offset: 0 },
                  { color: '%s', offset: 1 }
                ]
              }
            }];
            return { id: shape.id, gradient: 'linear', angle: %.0f };
            """,
            shapeId, shapeId, endX, endY, startColor, endColor, angle
        );
    }

    private String buildStrokeCode(String shapeId, String strokeColor, float strokeWidth) {
        return String.format("""
            let shape = null;
            try {
                shape = penpot.currentPage.getShapeById('%s');
            } catch (e) {
                console.log('[Stroke] Invalid ID, using first selected shape');
                if (penpot.selection.length > 0) {
                    shape = penpot.selection[0];
                }
            }

            if (!shape) {
                throw new Error('Shape not found. ID: %s');
            }

            shape.strokes = [{
              strokeColor: '%s',
              strokeWidth: %.1f,
              strokeAlignment: 'center'
            }];
            return { id: shape.id, stroke: '%s', width: %.1f };
            """,
            shapeId, shapeId, strokeColor, strokeWidth, strokeColor, strokeWidth
        );
    }

    private String buildShadowCode(
        String shapeId,
        float offsetX,
        float offsetY, 
        float blur,
        String shadowColor
    ) {
        return String.format("""
            let shape = null;
            try {
                shape = penpot.currentPage.getShapeById('%s');
            } catch (e) {
                console.log('[Shadow] Invalid ID, using first selected shape');
                if (penpot.selection.length > 0) {
                    shape = penpot.selection[0];
                }
            }

            if (!shape) {
                throw new Error('Shape not found. ID: %s');
            }

            shape.shadows = [{
              offsetX: %.1f,
              offsetY: %.1f,
              blur: %.1f,
              spread: 0,
              hidden: false,
              color: '%s'
            }];
            return { id: shape.id, shadow: { offsetX: %.1f, offsetY: %.1f, blur: %.1f } };
            """,
            shapeId, shapeId, offsetX, offsetY, blur, shadowColor, 
            offsetX, offsetY, blur
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