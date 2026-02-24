package com.penpot.ai.application.tools;

import com.penpot.ai.application.tools.support.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.*;
import org.springframework.stereotype.Component;

import java.util.Locale;

/**
 * Tools pour la gestion des assets et styles dans Penpot.
 *
 * <p>Gère uniquement les opérations de style (fill, gradient, stroke, shadow...).</p>
 * <p>Le lookup de forme réutilise {@link PenpotJsSnippets#findShapeOrFallback}.</p>
 * <p>L'exécution est déléguée à {@link PenpotToolExecutor#applyStyle}.</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PenpotAssetTools {

    private final PenpotToolExecutor toolExecutor;

    @Tool(description = """
        Apply a solid color fill to a shape.
        Color format: Hexadecimal #RRGGBB
        Opacity: 0.0 (transparent) to 1.0 (opaque)
        """)
    public String applyFillColor(
        @ToolParam(description = "ID of the shape to fill") String shapeId,
        @ToolParam(description = "Fill color in hex format (#RRGGBB)") String fillColor,
        @ToolParam(description = "Opacity from 0.0 to 1.0 (default: 1.0)", required = false) Float opacity
    ) {
        float o = (opacity != null && opacity >= 0f && opacity <= 1f) ? opacity : 1.0f;
        log.info("Tool called: applyFillColor (id={}, color={}, opacity={})", shapeId, fillColor, o);
        return toolExecutor.applyStyle(
            buildFillCode(shapeId, fillColor, o),
            "fillApplied", shapeId,
            String.format("Applied %s with opacity %.2f", fillColor, o)
        );
    }

    @Tool(description = """
        Apply a linear gradient fill to a shape.
        Angle: 0° = left to right, 90° = top to bottom, 45° = diagonal.
        Colors in hex format (#RRGGBB).
        """)
    public String applyGradient(
        @ToolParam(description = "ID of the shape") String shapeId,
        @ToolParam(description = "Start color in hex format") String startColor,
        @ToolParam(description = "End color in hex format") String endColor,
        @ToolParam(description = "Gradient angle in degrees (default: 0)", required = false) Float angle
    ) {
        float a = (angle != null) ? angle : 0f;
        log.info("Tool called: applyGradient (id={}, start={}, end={}, angle={})", shapeId, startColor, endColor, a);
        return toolExecutor.applyStyle(
            buildGradientCode(shapeId, startColor, endColor, a),
            "gradientApplied", shapeId,
            String.format("Applied gradient %s → %s at %.0f°", startColor, endColor, a)
        );
    }

    @Tool(description = """
        Apply a stroke (border/outline) to a shape.
        Stroke width: Thickness in pixels (typical: 1-5px).
        """)
    public String applyStroke(
        @ToolParam(description = "ID of the shape") String shapeId,
        @ToolParam(description = "Stroke color in hex format") String strokeColor,
        @ToolParam(description = "Stroke width in pixels (default: 1)", required = false) Float strokeWidth
    ) {
        float w = (strokeWidth != null && strokeWidth > 0) ? strokeWidth : 1f;
        log.info("Tool called: applyStroke (id={}, color={}, width={})", shapeId, strokeColor, w);
        return toolExecutor.applyStyle(
            buildStrokeCode(shapeId, strokeColor, w),
            "strokeApplied", shapeId,
            String.format("Applied %.1fpx %s stroke", w, strokeColor)
        );
    }

    @Tool(description = """
        Apply a drop shadow effect to a shape.
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
        @ToolParam(description = "Shadow color in hex format (with alpha: #RRGGBBAA)", required = false) String shadowColor
    ) {
        String color = (shadowColor != null && !shadowColor.isBlank()) ? shadowColor : "#00000066";
        log.info("Tool called: applyShadow (id={}, offset=({},{}), blur={}, color={})", shapeId, offsetX, offsetY, blur, color);
        return toolExecutor.applyStyle(
            buildShadowCode(shapeId, offsetX, offsetY, blur, color),
            "shadowApplied", shapeId,
            String.format("Applied shadow offset(%.1f, %.1f) blur %.1f", offsetX, offsetY, blur)
        );
    }

    @Tool(description = """
        Update the overall opacity of a shape.
        Opacity range: 0.0 (fully transparent) to 1.0 (fully opaque).
        """)
    public String updateOpacity(
        @ToolParam(description = "ID of the shape") String shapeId,
        @ToolParam(description = "Opacity value between 0.0 and 1.0") Float opacity
    ) {
        if (opacity == null || opacity < 0f || opacity > 1f) {
            return com.penpot.ai.application.tools.support.ToolResponseBuilder.error(
                "Opacity must be between 0.0 and 1.0"
            );
        }
        log.info("Tool called: updateOpacity (id={}, opacity={})", shapeId, opacity);
        return toolExecutor.applyStyle(
            buildOpacityCode(shapeId, opacity),
            "opacityUpdated", shapeId,
            String.format("Opacity set to %.2f", opacity)
        );
    }

    @Tool(description = """
        Update the border radius (corner rounding) of a shape.
        Radius is in pixels.
        """)
    public String updateBorderRadius(
        @ToolParam(description = "ID of the shape") String shapeId,
        @ToolParam(description = "Border radius in pixels") Float radius
    ) {
        if (radius == null || radius < 0) {
            return com.penpot.ai.application.tools.support.ToolResponseBuilder.error(
                "Border radius must be >= 0"
            );
        }
        log.info("Tool called: updateBorderRadius (id={}, radius={})", shapeId, radius);
        return toolExecutor.applyStyle(
            buildBorderRadiusCode(shapeId, radius),
            "borderRadiusUpdated", shapeId,
            String.format(Locale.US, "Border radius set to %.1fpx", radius)
        );
    }

    @Tool(description = """
        Replace a specific color in a shape with another color.
        The new color can be a hex color (#RRGGBB), a named color, or "rainbow"/"arc-en-ciel".
        """)
    public String replaceColor(
        @ToolParam(description = "ID of the shape") String shapeId,
        @ToolParam(description = "Color to replace (hex or name)") String oldColor,
        @ToolParam(description = "New color (hex, name, or special style)") String newColor
    ) {
        if (oldColor == null || oldColor.isBlank())
            return com.penpot.ai.application.tools.support.ToolResponseBuilder.error("oldColor is required");
        if (newColor == null || newColor.isBlank())
            return com.penpot.ai.application.tools.support.ToolResponseBuilder.error("newColor is required");

        log.info("Tool called: replaceColor (id={}, old={}, new={})", shapeId, oldColor, newColor);
        return toolExecutor.applyStyle(
            buildReplaceColorCode(shapeId, oldColor, newColor),
            "colorReplaced", shapeId,
            String.format("Replaced %s with %s", oldColor, newColor)
        );
    }

    // ==================== CODE GENERATION ====================

    private String buildFillCode(String shapeId, String fillColor, float opacity) {
        return PenpotJsSnippets.findShapeOrFallback(shapeId)
            + String.format(Locale.US,
                "shape.fills = [{ fillColor: '%s', fillOpacity: %.2f }];\n"
                + "return { id: shape.id, fill: '%s', opacity: %.2f };\n",
                fillColor, opacity, fillColor, opacity);
    }

    /**
     * Construit le code JS pour appliquer un gradient linéaire.
     *
     * <p>startX/startY/endX/endY sont des coordonnées normalisées (0.0→1.0)
     * relatives au bounding box de la forme, centrées sur le milieu (0.5).</p>
     *
     * <p>Exemples : 0°→gauche/droite, 90°→haut/bas, 45°→diagonal.</p>
     *
     * <p>{@code width: 1} est requis par le type {@code Gradient} de l'API Penpot.</p>
     */
    private String buildGradientCode(String shapeId, String startColor, String endColor, float angle) {
        double radians = Math.toRadians(angle);
        float cosA = (float) Math.cos(radians);
        float sinA = (float) Math.sin(radians);

        float startX = 0.5f - 0.5f * cosA;
        float startY = 0.5f - 0.5f * sinA;
        float endX   = 0.5f + 0.5f * cosA;
        float endY   = 0.5f + 0.5f * sinA;

        return PenpotJsSnippets.findShapeOrFallback(shapeId)
            + String.format(Locale.US, """
                shape.fills = [{ fillColorGradient: {
                    type: 'linear',
                    startX: %.4f, startY: %.4f,
                    endX: %.4f,   endY: %.4f,
                    width: 1,
                    stops: [
                        { color: '%s', opacity: 1, offset: 0 },
                        { color: '%s', opacity: 1, offset: 1 }
                    ]
                }}];
                return { id: shape.id, gradient: 'linear', angle: %.0f };
                """, startX, startY, endX, endY, startColor, endColor, angle);
    }

    private String buildStrokeCode(String shapeId, String strokeColor, float strokeWidth) {
        return PenpotJsSnippets.findShapeOrFallback(shapeId)
            + String.format(Locale.US,
                "shape.strokes = [{ strokeColor: '%s', strokeWidth: %.1f, strokeAlignment: 'center' }];\n"
                + "return { id: shape.id, stroke: '%s', width: %.1f };\n",
                strokeColor, strokeWidth, strokeColor, strokeWidth);
    }

    private String buildShadowCode(String shapeId, float offsetX, float offsetY, float blur, String color) {
        return PenpotJsSnippets.findShapeOrFallback(shapeId)
            + String.format(Locale.US, """
                shape.shadows = [{ offsetX: %.1f, offsetY: %.1f, blur: %.1f, spread: 0, hidden: false, color: '%s' }];
                return { id: shape.id, shadow: { offsetX: %.1f, offsetY: %.1f, blur: %.1f } };
                """, offsetX, offsetY, blur, color, offsetX, offsetY, blur);
    }

    private String buildOpacityCode(String shapeId, float opacity) {
        return PenpotJsSnippets.findShapeOrFallback(shapeId)
            + String.format(Locale.US,
                "shape.opacity = %.2f;\nreturn { id: shape.id, opacity: %.2f };\n",
                opacity, opacity);
    }

    private String buildBorderRadiusCode(String shapeId, float radius) {
        return PenpotJsSnippets.findShapeOrFallback(shapeId)
            + String.format(Locale.US, """
                if (!('borderRadius' in shape)) throw new Error('This shape does not support border radius');
                shape.borderRadius = %.2f;
                return { id: shape.id, borderRadius: %.2f };
                """, radius, radius);
    }

    private String buildReplaceColorCode(String shapeId, String oldColor, String newColor) {
        String helpers = """
            function normalizeHex(hex) {
                if (!hex) return null;
                hex = hex.toUpperCase();
                if (hex.length === 9) hex = hex.substring(0, 7);
                return hex;
            }
            function hexToRgb(hex) {
                hex = normalizeHex(hex);
                if (!hex || !hex.startsWith('#')) return null;
                const bigint = parseInt(hex.substring(1), 16);
                return { r: (bigint >> 16) & 255, g: (bigint >> 8) & 255, b: bigint & 255 };
            }
            function isSimilarColor(c1, c2, tolerance = 5) {
                if (!c1 || !c2) return false;
                return Math.abs(c1.r - c2.r) <= tolerance &&
                       Math.abs(c1.g - c2.g) <= tolerance &&
                       Math.abs(c1.b - c2.b) <= tolerance;
            }
            function isRainbow(value) {
                if (!value) return false;
                value = value.toLowerCase();
                return value.includes("rainbow") || value.includes("arc-en-ciel") || value.includes("arc en ciel");
            }
            """;

        String logic = String.format(Locale.US, """
            let replaced = false;
            const targetRgb = hexToRgb('%s');

            if (isRainbow('%s')) {
                shape.fills = [{ fillColorGradient: {
                    type: 'linear',
                    startX: 0, startY: 0.5,
                    endX: 1,   endY: 0.5,
                    width: 1,
                    stops: [
                        { color: '#FF0000', opacity: 1, offset: 0    },
                        { color: '#FF7F00', opacity: 1, offset: 0.16 },
                        { color: '#FFFF00', opacity: 1, offset: 0.33 },
                        { color: '#00FF00', opacity: 1, offset: 0.5  },
                        { color: '#0000FF', opacity: 1, offset: 0.66 },
                        { color: '#4B0082', opacity: 1, offset: 0.83 },
                        { color: '#8F00FF', opacity: 1, offset: 1    }
                    ]
                }}];
                return { id: shape.id, gradient: 'rainbow' };
            }

            if (shape.fills?.length > 0) {
                shape.fills = shape.fills.map(fill => {
                    if (isSimilarColor(hexToRgb(fill.fillColor), targetRgb)) {
                        replaced = true;
                        return { ...fill, fillColor: '%s' };
                    }
                    return fill;
                });
            }
            if (shape.strokes?.length > 0) {
                shape.strokes = shape.strokes.map(stroke => {
                    if (isSimilarColor(hexToRgb(stroke.strokeColor), targetRgb)) {
                        replaced = true;
                        return { ...stroke, strokeColor: '%s' };
                    }
                    return stroke;
                });
            }
            if (!replaced) throw new Error('No similar color found to replace');
            return { id: shape.id, replacedColor: '%s', newColor: '%s' };
            """, oldColor, newColor, newColor, newColor, oldColor, newColor);

        return helpers + PenpotJsSnippets.findShapeOrFallback(shapeId) + logic;
    }
}