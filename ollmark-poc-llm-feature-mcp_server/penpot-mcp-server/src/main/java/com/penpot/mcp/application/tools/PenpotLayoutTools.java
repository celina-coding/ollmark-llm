package com.penpot.mcp.application.tools;

import com.penpot.mcp.core.ports.in.ExecuteCodeUseCase;
import com.penpot.mcp.core.domain.*;
import com.penpot.mcp.shared.util.JsonUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.*;
import org.springframework.stereotype.Component;
import java.util.List;

/**
 * Tools pour l'alignement et la distribution de formes dans Penpot.
 * 
 * <h2>Responsabilité unique</h2>
 * Gère uniquement les opérations de layout :
 * - Alignement (horizontal, vertical, centre)
 * - Distribution (espacement égal)
 * - Groupement
 * 
 * <h2>Pattern Strategy</h2>
 * Différentes stratégies d'alignement :
 * - AlignLeft, AlignCenter, AlignRight
 * - AlignTop, AlignMiddle, AlignBottom
 * - DistributeHorizontally, DistributeVertically
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

        Horizontal alignments:
        - left: Align left edges
        - center: Align horizontal centers
        - right: Align right edges

        Vertical alignments:
        - top: Align top edges
        - middle: Align vertical centers
        - bottom: Align bottom edges

        Examples:
        - "Align all rectangles to the left"
        - "Center the text elements horizontally"
        - "Align top edges of the shapes"

        Requires at least 2 shapes.
        """)
    public String alignShapes(
        @ToolParam(description = "List of shape IDs to align (comma-separated)") String shapeIds,
        @ToolParam(description = "Alignment type: left, center, right, top, middle, bottom") 
        String alignment
    ) {
        log.info("Tool called: alignShapes (ids='{}', alignment='{}')", 
            shapeIds, alignment);

        List<String> ids = parseShapeIds(shapeIds);

        if (ids.size() < 2) {
            return formatError("At least 2 shapes required for alignment");
        }

        if (!isValidAlignment(alignment)) {
            return formatError("Invalid alignment: " + alignment + 
                ". Valid: left, center, right, top, middle, bottom");
        }

        String code = buildAlignCode(ids, alignment);

        try {
            TaskResult result = executeCodeUseCase.execute(
                ExecuteCodeCommand.of(code)
            );

            if (!result.isSuccess()) {
                return formatError(result.getError().orElse("Unknown error"));
            }

            return formatSuccess("aligned", ids.size(), 
                String.format("Aligned %d shapes to %s", ids.size(), alignment));
        } catch (Exception e) {
            log.error("Failed to align shapes", e);
            return formatError(e.getMessage());
        }
    }

    /**
     * Distribue uniformément les formes le long d'un axe.
     * 
     * @param shapeIds Liste des IDs de formes
     * @param axis Axe de distribution (horizontal ou vertical)
     * @return JSON avec confirmation
     */
    @Tool(description = """
        Distribute shapes evenly along an axis with equal spacing.

        Distribution modes:
        - horizontal: Distribute with equal horizontal spacing
        - vertical: Distribute with equal vertical spacing

        Examples:
        - "Distribute the buttons horizontally"
        - "Space the images evenly vertically"
        - "Distribute shapes with equal gaps"

        Requires at least 3 shapes.
        The first and last shapes define the distribution bounds.
        """)
    public String distributeShapes(
        @ToolParam(description = "List of shape IDs to distribute (comma-separated)") 
        String shapeIds,
        @ToolParam(description = "Distribution axis: horizontal or vertical") 
        String axis
    ) {
        log.info("Tool called: distributeShapes (ids='{}', axis='{}')", 
            shapeIds, axis);

        List<String> ids = parseShapeIds(shapeIds);

        if (ids.size() < 3) {
            return formatError("At least 3 shapes required for distribution");
        }

        if (!axis.equalsIgnoreCase("horizontal") && !axis.equalsIgnoreCase("vertical")) {
            return formatError("Invalid axis: " + axis + ". Valid: horizontal, vertical");
        }

        String code = buildDistributeCode(ids, axis);

        try {
            TaskResult result = executeCodeUseCase.execute(
                ExecuteCodeCommand.of(code)
            );

            if (!result.isSuccess()) {
                return formatError(result.getError().orElse("Unknown error"));
            }

            return formatSuccess("distributed", ids.size(),
                String.format("Distributed %d shapes %s", ids.size(), axis));
        } catch (Exception e) {
            log.error("Failed to distribute shapes", e);
            return formatError(e.getMessage());
        }
    }

    /**
     * Groupe plusieurs formes ensemble.
     * 
     * @param shapeIds Liste des IDs de formes à grouper
     * @param groupName Nom du groupe créé
     * @return JSON avec l'ID du groupe créé
     */
    @Tool(description = """
        Group multiple shapes together into a single container.

        Grouped shapes:
        - Move together as a unit
        - Can be transformed as a group
        - Can be ungrouped later

        Examples:
        - "Group the logo elements together"
        - "Create a group with the header items"
        - "Combine shapes into a group"

        Requires at least 2 shapes.
        """)
    public String groupShapes(
        @ToolParam(description = "List of shape IDs to group (comma-separated)") 
        String shapeIds,
        @ToolParam(description = "Name for the group", required = false) 
        String groupName
    ) {
        log.info("Tool called: groupShapes (ids='{}', name='{}')", 
            shapeIds, groupName);

        List<String> ids = parseShapeIds(shapeIds);

        if (ids.size() < 2) {
            return formatError("At least 2 shapes required for grouping");
        }

        String code = buildGroupCode(ids, groupName);

        try {
            TaskResult result = executeCodeUseCase.execute(
                ExecuteCodeCommand.of(code)
            );

            if (!result.isSuccess()) {
                return formatError(result.getError().orElse("Unknown error"));
            }

            return formatSuccess("grouped", ids.size(),
                String.format("Created group with %d shapes", ids.size()));
        } catch (Exception e) {
            log.error("Failed to group shapes", e);
            return formatError(e.getMessage());
        }
    }

    // ==================== CODE GENERATION METHODS ====================

    private String buildAlignCode(List<String> shapeIds, String alignment) {
        StringBuilder code = new StringBuilder();

        code.append("const shapes = [\n");
        for (int i = 0; i < shapeIds.size(); i++) {
            code.append(String.format("  penpot.getShape('%s')", shapeIds.get(i)));
            if (i < shapeIds.size() - 1) code.append(",");
            code.append("\n");
        }
        code.append("].filter(s => s !== null);\n\n");

        code.append("if (shapes.length < 2) throw new Error('Not enough shapes found');\n\n");

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

    private String buildDistributeCode(List<String> shapeIds, String axis) {
        StringBuilder code = new StringBuilder();

        code.append("const shapes = [\n");
        for (int i = 0; i < shapeIds.size(); i++) {
            code.append(String.format("  penpot.getShape('%s')", shapeIds.get(i)));
            if (i < shapeIds.size() - 1) code.append(",");
            code.append("\n");
        }
        code.append("].filter(s => s !== null);\n\n");

        code.append("if (shapes.length < 3) throw new Error('At least 3 shapes required');\n\n");

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

    private String buildGroupCode(List<String> shapeIds, String groupName) {
        StringBuilder code = new StringBuilder();

        code.append("const shapes = [\n");
        for (int i = 0; i < shapeIds.size(); i++) {
            code.append(String.format("  penpot.getShape('%s')", shapeIds.get(i)));
            if (i < shapeIds.size() - 1) code.append(",");
            code.append("\n");
        }
        code.append("].filter(s => s !== null);\n\n");
        code.append("if (shapes.length < 2) throw new Error('At least 2 shapes required');\n\n");
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
            .filter(s -> !s.isEmpty())
            .toList();
    }

    private boolean isValidAlignment(String alignment) {
        return java.util.Set.of("left", "center", "right", "top", "middle", "bottom")
            .contains(alignment.toLowerCase());
    }

    private String formatSuccess(String operation, int count, String details) {
        return String.format(
            "{\"success\": true, \"operation\": %s, \"shapeCount\": %d, \"details\": %s}",
            JsonUtils.escapeJson(operation),
            count,
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