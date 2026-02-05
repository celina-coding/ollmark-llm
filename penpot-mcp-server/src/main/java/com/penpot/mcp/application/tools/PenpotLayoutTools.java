package com.penpot.mcp.application.tools;

import com.penpot.mcp.core.ports.in.ExecuteCodeUseCase;
import com.penpot.mcp.core.domain.*;
import com.penpot.mcp.shared.util.JsonUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.*;
import org.springframework.stereotype.Component;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Tools pour l'alignement et la distribution de formes dans Penpot.
 * 
 * <h2>Responsabilité unique</h2>
 * Gère uniquement les opérations de layout :
 * - Alignement (horizontal, vertical, centre)
 * - Distribution (espacement égal)
 * - Groupement
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PenpotLayoutTools {

    private final ExecuteCodeUseCase executeCodeUseCase;

    /**
     * Aligne plusieurs formes selon un axe et une direction.
     * 
     * @param shapeIds Liste des IDs de formes à aligner
     * @param alignment Type d'alignement (left, center, right, top, middle, bottom)
     * @return JSON avec confirmation de l'alignement
     */
    @Tool(description = """
        Align multiple shapes along a specified axis.

        IMPORTANT: This tool can work in two ways:
        1. With explicit shape IDs (provide comma-separated UUIDs)
        2. With current selection (if IDs are invalid, uses penpot.selection automatically)

        Horizontal alignments: left, center, right
        Vertical alignments: top, middle, bottom

        Examples:
        - "Align all rectangles to the left"
        - "Center the selected shapes horizontally"
        - "Align top edges"

        TIP: If you just created shapes, the user can select them in Penpot before aligning.
        """)
    public String alignShapes(
        @ToolParam(description = "Shape IDs (comma-separated) OR 'selection' to use current selection") 
        String shapeIds,
        @ToolParam(description = "Alignment: left, center, right, top, middle, bottom") 
        String alignment
    ) {
        log.info("Tool called: alignShapes (ids='{}', alignment='{}')", 
            shapeIds, alignment);

        if (!isValidAlignment(alignment)) return formatError("Invalid alignment: " + alignment);

        boolean useSelection = "selection".equalsIgnoreCase(shapeIds.trim());
        List<String> ids = useSelection ? List.of() : parseShapeIds(shapeIds);

        String code = buildAlignCodeWithFallback(ids, alignment, useSelection);

        try {
            TaskResult result = executeCodeUseCase.execute(
                ExecuteCodeCommand.of(code)
            );

            if (!result.isSuccess()) {
                return formatError(result.getError().orElse("Unknown error"));
            }

            return extractAndFormatResult(result, "aligned");
        } catch (Exception e) {
            log.error("Failed to align shapes", e);
            return formatError(e.getMessage());
        }
    }

    @Tool(description = """
        Distribute shapes evenly along an axis.

        Works with explicit IDs or current selection.

        Modes: horizontal, vertical
        Requires at least 3 shapes.
        """)
    public String distributeShapes(
        @ToolParam(description = "Shape IDs (comma-separated) OR 'selection'") 
        String shapeIds,
        @ToolParam(description = "Axis: horizontal or vertical") 
        String axis
    ) {
        log.info("Tool called: distributeShapes (ids='{}', axis='{}')", 
            shapeIds, axis);

        if (!axis.equalsIgnoreCase("horizontal") && !axis.equalsIgnoreCase("vertical")) {
            return formatError("Invalid axis: " + axis);
        }

        boolean useSelection = "selection".equalsIgnoreCase(shapeIds.trim());
        List<String> ids = useSelection ? List.of() : parseShapeIds(shapeIds);

        String code = buildDistributeCodeWithFallback(ids, axis, useSelection);

        try {
            TaskResult result = executeCodeUseCase.execute(
                ExecuteCodeCommand.of(code)
            );

            if (!result.isSuccess()) {
                return formatError(result.getError().orElse("Unknown error"));
            }

            return extractAndFormatResult(result, "distributed");
        } catch (Exception e) {
            log.error("Failed to distribute shapes", e);
            return formatError(e.getMessage());
        }
    }

    @Tool(description = """
        Group shapes together.

        Works with explicit IDs or current selection.
        Returns the ID of the created group.
        """)
    public String groupShapes(
        @ToolParam(description = "Shape IDs (comma-separated) OR 'selection'") 
        String shapeIds,
        @ToolParam(description = "Group name", required = false) 
        String groupName
    ) {
        log.info("Tool called: groupShapes (ids='{}', name='{}')", 
            shapeIds, groupName);

        boolean useSelection = "selection".equalsIgnoreCase(shapeIds.trim());
        List<String> ids = useSelection ? List.of() : parseShapeIds(shapeIds);

        String code = buildGroupCodeWithFallback(ids, groupName, useSelection);

        try {
            TaskResult result = executeCodeUseCase.execute(
                ExecuteCodeCommand.of(code)
            );

            if (!result.isSuccess()) {
                return formatError(result.getError().orElse("Unknown error"));
            }

            String groupId = result.getData()
                .map(obj -> {
                    if (obj instanceof java.util.Map) {
                        return ((java.util.Map<?, ?>) obj).get("groupId");
                    }
                    return obj;
                })
                .map(Object::toString)
                .orElse("unknown");

            return formatGroupSuccess(groupId);
        } catch (Exception e) {
            log.error("Failed to group shapes", e);
            return formatError(e.getMessage());
        }
    }

    // ==================== CODE GENERATION WITH FALLBACK ====================

    private String buildAlignCodeWithFallback(
        List<String> shapeIds,
        String alignment, 
        boolean forceSelection
    ) {
        StringBuilder code = new StringBuilder();

        if (forceSelection || shapeIds.isEmpty()) {
            code.append("const shapes = penpot.selection;\n");
            code.append("console.log('[Align] Using current selection:', shapes.length, 'shapes');\n");
        } else {
            code.append("const shapes = [];\n");

            for (String id : shapeIds) {
                code.append(String.format("""
                    try {
                        const shape = penpot.currentPage.getShapeById('%s');
                        if (shape) shapes.push(shape);
                    } catch (e) {
                        console.log('[Align] Invalid ID or shape not found: %s');
                    }
                    """, id, id));
            }

            code.append("\n");
            code.append("""
                if (shapes.length === 0) {
                    console.log('[Align] No valid IDs, using current selection');
                    shapes.push(...penpot.selection);
                }
                """);
        }

        code.append("\nif (shapes.length < 2) {\n");
        code.append("    throw new Error('Need at least 2 shapes to align. Current: ' + shapes.length + '. Please select shapes in Penpot.');\n");
        code.append("}\n\n");

        switch (alignment.toLowerCase()) {
            case "left":
                code.append("const minX = Math.min(...shapes.map(s => s.x));\n");
                code.append("shapes.forEach(s => s.x = minX);\n");
                break;
            case "center":
                code.append("const avgCenterX = shapes.reduce((sum, s) => sum + s.x + s.width/2, 0) / shapes.length;\n");
                code.append("shapes.forEach(s => s.x = avgCenterX - s.width/2);\n");
                break;
            case "right":
                code.append("const maxX = Math.max(...shapes.map(s => s.x + s.width));\n");
                code.append("shapes.forEach(s => s.x = maxX - s.width);\n");
                break;
            case "top":
                code.append("const minY = Math.min(...shapes.map(s => s.y));\n");
                code.append("shapes.forEach(s => s.y = minY);\n");
                break;
            case "middle":
                code.append("const avgCenterY = shapes.reduce((sum, s) => sum + s.y + s.height/2, 0) / shapes.length;\n");
                code.append("shapes.forEach(s => s.y = avgCenterY - s.height/2);\n");
                break;
            case "bottom":
                code.append("const maxY = Math.max(...shapes.map(s => s.y + s.height));\n");
                code.append("shapes.forEach(s => s.y = maxY - s.height);\n");
                break;
        }

        code.append("\nreturn { aligned: shapes.length, ids: shapes.map(s => s.id) };\n");

        return code.toString();
    }

    private String buildDistributeCodeWithFallback(
        List<String> shapeIds,
        String axis, 
        boolean forceSelection
    ) {
        StringBuilder code = new StringBuilder();

        if (forceSelection || shapeIds.isEmpty()) {
            code.append("const shapes = penpot.selection;\n");
        } else {
            code.append("const shapes = [];\n");

            for (String id : shapeIds) {
                code.append(String.format("""
                    try {
                        const shape = penpot.currentPage.getShapeById('%s');
                        if (shape) shapes.push(shape);
                    } catch (e) {
                        console.log('[Distribute] Invalid ID: %s');
                    }
                    """, id, id));
            }

            code.append("""
                if (shapes.length === 0) {
                    console.log('[Distribute] Using selection');
                    shapes.push(...penpot.selection);
                }
                """);
        }

        code.append("\nif (shapes.length < 3) {\n");
        code.append("    throw new Error('Need at least 3 shapes to distribute. Current: ' + shapes.length);\n");
        code.append("}\n\n");

        if (axis.equalsIgnoreCase("horizontal")) {
            code.append("""
                shapes.sort((a, b) => a.x - b.x);
                const first = shapes[0];
                const last = shapes[shapes.length - 1];
                const totalSpace = (last.x + last.width) - first.x;
                const totalShapeWidth = shapes.reduce((sum, s) => sum + s.width, 0);
                const gap = (totalSpace - totalShapeWidth) / (shapes.length - 1);

                let currentX = first.x;
                shapes.forEach(s => {
                    s.x = currentX;
                    currentX += s.width + gap;
                });
                """);
        } else {
            code.append("""
                shapes.sort((a, b) => a.y - b.y);
                const first = shapes[0];
                const last = shapes[shapes.length - 1];
                const totalSpace = (last.y + last.height) - first.y;
                const totalShapeHeight = shapes.reduce((sum, s) => sum + s.height, 0);
                const gap = (totalSpace - totalShapeHeight) / (shapes.length - 1);

                let currentY = first.y;
                shapes.forEach(s => {
                    s.y = currentY;
                    currentY += s.height + gap;
                });
                """);
        }

        code.append("\nreturn { distributed: shapes.length, ids: shapes.map(s => s.id) };\n");

        return code.toString();
    }

    private String buildGroupCodeWithFallback(
        List<String> shapeIds,
        String groupName, 
        boolean forceSelection
    ) {
        StringBuilder code = new StringBuilder();

        if (forceSelection || shapeIds.isEmpty()) {
            code.append("const shapes = penpot.selection;\n");
        } else {
            code.append("const shapes = [];\n");

            for (String id : shapeIds) {
                code.append(String.format("""
                    try {
                        const shape = penpot.currentPage.getShapeById('%s');
                        if (shape) shapes.push(shape);
                    } catch (e) {
                        console.log('[Group] Invalid ID: %s');
                    }
                    """, id, id));
            }

            code.append("""
                if (shapes.length === 0) {
                    console.log('[Group] Using selection');
                    shapes.push(...penpot.selection);
                }
                """);
        }

        code.append("\nif (shapes.length < 2) {\n");
        code.append("    throw new Error('Need at least 2 shapes to group. Current: ' + shapes.length);\n");
        code.append("}\n\n");

        code.append("const group = penpot.group(shapes);\n");

        if (groupName != null && !groupName.isBlank()) {
            code.append(String.format("group.name = '%s';\n", 
                groupName.replace("'", "\\'")));
        }

        code.append("\nreturn { groupId: group.id, shapeCount: shapes.length };\n");

        return code.toString();
    }

    // ==================== HELPER METHODS ====================

    private List<String> parseShapeIds(String shapeIdsStr) {
        return java.util.Arrays.stream(shapeIdsStr.split(","))
            .map(String::trim)
            .filter(s -> !s.isEmpty() && !s.equalsIgnoreCase("selection"))
            .toList();
    }

    private boolean isValidAlignment(String alignment) {
        return java.util.Set.of("left", "center", "right", "top", "middle", "bottom")
            .contains(alignment.toLowerCase());
    }

    private String extractAndFormatResult(TaskResult result, String operation) {
        Object data = result.getData().orElse(null);

        if (data instanceof java.util.Map) {
            @SuppressWarnings("unchecked")
            java.util.Map<String, Object> resultMap = (java.util.Map<String, Object>) data;

            Object idsObj = resultMap.get("ids");
            if (idsObj instanceof java.util.List) {
                @SuppressWarnings("unchecked")
                java.util.List<String> ids = (java.util.List<String>) idsObj;

                log.info("Successfully {} {} shapes: {}", operation, ids.size(), ids);

                return String.format(
                    "Successfully %s %d shapes.\n\nShape IDs:\n%s\n\n" +
                    "TIP: These shapes are now aligned. You can continue working with them.",
                    operation,
                    ids.size(),
                    ids.stream()
                        .map(id -> "  - " + id)
                        .collect(Collectors.joining("\n"))
                );
            }
        }

        return String.format("Successfully %s shapes.", operation);
    }

    // ==================== RESPONSE FORMATTING ====================

    private String formatGroupSuccess(String groupId) {
        return String.format(
            "Successfully created group.\n\nGroup ID: %s\n\n" +
            "TIP: You can now work with the entire group as a single shape.",
            groupId
        );
    }

    private String formatError(String errorMessage) {
        return String.format("Operation failed: %s", errorMessage);
    }
}