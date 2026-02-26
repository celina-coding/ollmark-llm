error id: file://<HOME>/Bureau/ollmark-poc-llm%20/penpot-ai-server/src/main/java/com/penpot/ai/application/tools/PenpotTransformTools.java:_empty_/PenpotJsSnippets#
file://<HOME>/Bureau/ollmark-poc-llm%20/penpot-ai-server/src/main/java/com/penpot/ai/application/tools/PenpotTransformTools.java
empty definition using pc, found symbol in pc: _empty_/PenpotJsSnippets#
empty definition using semanticdb
empty definition using fallback
non-local guesses:

offset: 4372
uri: file://<HOME>/Bureau/ollmark-poc-llm%20/penpot-ai-server/src/main/java/com/penpot/ai/application/tools/PenpotTransformTools.java
text:
```scala
package com.penpot.ai.application.tools;

import com.penpot.ai.application.tools.support.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.*;
import org.springframework.stereotype.Component;

import java.util.Locale;

/**
 * Tools pour les transformations géométriques dans Penpot.
 *
 * <p>Gère uniquement les transformations (rotate, scale, move, resize).</p>
 * <p>Le lookup de forme réutilise {@link PenpotJsSnippets#findShapeOrFallback()}.</p>
 * <p>L'exécution est déléguée à {@link PenpotToolExecutor#transformShape()}.</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PenpotTransformTools {

    private final PenpotToolExecutor toolExecutor;

    @Tool(description = """
        Rotate a shape by a specified angle in degrees.
        Positive angles rotate clockwise, negative counter-clockwise.
        The rotation is applied around the shape's center point.
        Returns the shape ID so you can continue working with it.
        """)
    public String rotateShape(
        @ToolParam(description = "ID of the shape to rotate") String shapeId,
        @ToolParam(description = "Rotation angle in degrees (positive = clockwise)") int angle
    ) {
        log.info("Tool called: rotateShape (id={}, angle={}°)", shapeId, angle);
        return toolExecutor.transformShape(buildRotateCode(shapeId, angle), "rotated", shapeId);
    }

    @Tool(description = """
        Scale a shape by specified factors on X and Y axes.
        Scale factors: 1.0 = original, 2.0 = double, 0.5 = half.
        For uniform scaling, use the same value for both axes.
        Returns the shape ID so you can continue working with it.
        """)
    public String scaleShape(
        @ToolParam(description = "ID of the shape to scale") String shapeId,
        @ToolParam(description = "Horizontal scale factor (1.0 = original)") float scaleX,
        @ToolParam(description = "Vertical scale factor (1.0 = original)") float scaleY
    ) {
        log.info("Tool called: scaleShape (id={}, scaleX={}, scaleY={})", shapeId, scaleX, scaleY);
        return toolExecutor.transformShape(buildScaleCode(shapeId, scaleX, scaleY), "scaled", shapeId);
    }

    @Tool(description = """
        Move a shape to a new position.
        Position modes:
        - Absolute (relative=false): Move to exact coordinates
        - Relative (relative=true): Move by offset from current position
        Returns the shape ID so you can continue working with it.
        """)
    public String moveShape(
        @ToolParam(description = "ID of the shape to move") String shapeId,
        @ToolParam(description = "New X position or X offset") float newX,
        @ToolParam(description = "New Y position or Y offset") float newY,
        @ToolParam(description = "If true, move relative to current position", required = false) Boolean relative
    ) {
        boolean isRelative = relative != null && relative;
        log.info("Tool called: moveShape (id={}, x={}, y={}, relative={})", shapeId, newX, newY, isRelative);
        return toolExecutor.transformShape(buildMoveCode(shapeId, newX, newY, isRelative), "moved", shapeId);
    }

    @Tool(description = """
        Resize a shape to specific dimensions.
        Note: sets absolute dimensions, unlike scaleShape which uses factors.
        Returns the shape ID so you can continue working with it.
        """)
    public String resizeShape(
        @ToolParam(description = "ID of the shape to resize") String shapeId,
        @ToolParam(description = "New width in pixels") float newWidth,
        @ToolParam(description = "New height in pixels") float newHeight
    ) {
        log.info("Tool called: resizeShape (id={}, w={}, h={})", shapeId, newWidth, newHeight);
        return toolExecutor.transformShape(buildResizeCode(shapeId, newWidth, newHeight), "resized", shapeId);
    }

    // ==================== CODE GENERATION ====================

    private String buildRotateCode(String shapeId, int angle) {
        return PenpotJsSnippets.findShapeOrFallback(shapeId)
            + String.format("shape.rotation = (shape.rotation || 0) + %d;\n", angle)
            + "return { id: shape.id, rotation: shape.rotation };\n";
    }

    private String buildScaleCode(String shapeId, float scaleX, float scaleY) {
        return PenpotJsSnip@@pets.findShapeOrFallback(shapeId)
            + String.format(Locale.US,
                "const currentWidth = shape.width || 1;\n"
                + "const currentHeight = shape.height || 1;\n"
                + "shape.resize(currentWidth * %.2f, currentHeight * %.2f);\n",
                scaleX, scaleY)
            + "return { id: shape.id, width: shape.width, height: shape.height };\n";
    }

    private String buildMoveCode(String shapeId, float newX, float newY, boolean relative) {
        String movement = relative
            ? String.format(Locale.US,
                "shape.x = (shape.x || 0) + %.2f;\nshape.y = (shape.y || 0) + %.2f;\n", newX, newY)
            : String.format(Locale.US,
                "shape.x = %.2f;\nshape.y = %.2f;\n", newX, newY);

        return PenpotJsSnippets.findShapeOrFallback(shapeId)
            + movement
            + "return { id: shape.id, x: shape.x, y: shape.y };\n";
    }

    private String buildResizeCode(String shapeId, float newWidth, float newHeight) {
        return PenpotJsSnippets.findShapeOrFallback(shapeId)
            + String.format(Locale.US, "shape.resize(%.2f, %.2f);\n", newWidth, newHeight)
            + "return { id: shape.id, width: shape.width, height: shape.height };\n";
    }
}
```


#### Short summary: 

empty definition using pc, found symbol in pc: _empty_/PenpotJsSnippets#