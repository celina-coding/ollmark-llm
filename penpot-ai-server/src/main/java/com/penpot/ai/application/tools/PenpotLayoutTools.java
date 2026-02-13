package com.penpot.ai.application.tools;

import com.penpot.ai.core.ports.in.ExecuteCodeUseCase;
import com.penpot.ai.core.domain.*;
import com.penpot.ai.shared.util.JsonUtils;
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

    

    // ==================== HIERARCHI ====================

    @Tool(description = """
        Send shapes one step backward in the layer order (z-index).

        Works with explicit IDs or current selection.
        """)
    public String sendShapeBackward(
        @ToolParam(description = "Shape IDs (comma-separated) OR 'selection'") String shapeIds
    ) {
        log.info("Tool called: sendShapeBackward (ids='{}')", shapeIds);

        boolean useSelection = "selection".equalsIgnoreCase(shapeIds.trim());
        List<String> ids = useSelection ? List.of() : parseShapeIds(shapeIds);

        String code = buildZOrderCodeWithFallback(ids, "sendBackward", useSelection);

        return executeAndFormatIds(code, "sent backward");
    }

    @Tool(description = """
        Bring shapes one step forward in the layer order (z-index).

        Works with explicit IDs or current selection.
        """)
    public String sendShapeFrontward(
        @ToolParam(description = "Shape IDs (comma-separated) OR 'selection'") String shapeIds
    ) {
        log.info("Tool called: sendShapeFrontward (ids='{}')", shapeIds);

        boolean useSelection = "selection".equalsIgnoreCase(shapeIds.trim());
        List<String> ids = useSelection ? List.of() : parseShapeIds(shapeIds);

        String code = buildZOrderCodeWithFallback(ids, "bringForward", useSelection);

        return executeAndFormatIds(code, "brought forward");
    }

    @Tool(description = """
        Send shapes to the very back (bottom) of their siblings list.

        Works with explicit IDs or current selection.
        """)
    public String sendShapeToTheBack(
        @ToolParam(description = "Shape IDs (comma-separated) OR 'selection'") String shapeIds
    ) {
        log.info("Tool called: sendShapeToTheBack (ids='{}')", shapeIds);

        boolean useSelection = "selection".equalsIgnoreCase(shapeIds.trim());
        List<String> ids = useSelection ? List.of() : parseShapeIds(shapeIds);

        String code = buildZOrderCodeWithFallback(ids, "sendToBack", useSelection);

        return executeAndFormatIds(code, "sent to back");
    }

    @Tool(description = """
        Bring shapes to the very front (top) of their siblings list.

        Works with explicit IDs or current selection.
        """)
    public String sendShapeToTheFront(
        @ToolParam(description = "Shape IDs (comma-separated) OR 'selection'") String shapeIds
    ) {
        log.info("Tool called: sendShapeToTheFront (ids='{}')", shapeIds);

        boolean useSelection = "selection".equalsIgnoreCase(shapeIds.trim());
        List<String> ids = useSelection ? List.of() : parseShapeIds(shapeIds);

        String code = buildZOrderCodeWithFallback(ids, "bringToFront", useSelection);

        return executeAndFormatIds(code, "brought to front");
    }

    @Tool(description = """
        Move one or more shapes into a specific board.

        Notes:
        - The shapes will be re-parented to the target board.
        - Their position is preserved using board coordinates.

        Works with explicit shape IDs or current selection.
        """)
    public String addShapeToBoard(
        @ToolParam(description = "Shape IDs (comma-separated) OR 'selection'") String shapeIds,
        @ToolParam(description = "Target board ID") String boardId
    ) {
        log.info("Tool called: addShapeToBoard (shapeIds='{}', boardId='{}')", shapeIds, boardId);

        boolean useSelection = "selection".equalsIgnoreCase(shapeIds.trim());
        List<String> ids = useSelection ? List.of() : parseShapeIds(shapeIds);

        String code = buildMoveToBoardCodeWithFallback(ids, boardId, useSelection);

        return executeAndFormatIds(code, "moved to board");
    }

    @Tool(description = """
        Remove one or more shapes from their parent (delete from the document tree).

        Works with explicit IDs or current selection.
        """)
    public String removeShapeFromParent(
        @ToolParam(description = "Shape IDs (comma-separated) OR 'selection'") String shapeIds
    ) {
        log.info("Tool called: removeShapeFromParent (ids='{}')", shapeIds);

        boolean useSelection = "selection".equalsIgnoreCase(shapeIds.trim());
        List<String> ids = useSelection ? List.of() : parseShapeIds(shapeIds);

        String code = buildRemoveCodeWithFallback(ids, useSelection);

        return executeAndFormatIds(code, "removed");
    }

    @Tool(description = """
        Clone a shape (duplicate it). Returns the new cloned shape ID.

        Works with explicit ID or current selection (uses the first selected shape).
        You can optionally offset the clone position.
        """)
    public String cloneShape(
        @ToolParam(description = "Shape ID OR 'selection'") String shapeId,
        @ToolParam(description = "Offset X for the clone (default 20)", required = false) Integer offsetX,
        @ToolParam(description = "Offset Y for the clone (default 20)", required = false) Integer offsetY
    ) {
        log.info("Tool called: cloneShape (id='{}', dx={}, dy={})", shapeId, offsetX, offsetY);

        boolean useSelection = "selection".equalsIgnoreCase(shapeId.trim());
        List<String> ids = useSelection ? List.of() : parseShapeIds(shapeId);
        if (!useSelection && ids.isEmpty()) {
            useSelection = true;
            ids = List.of();
        }

        int dx = offsetX != null ? offsetX : 20;
        int dy = offsetY != null ? offsetY : 20;

        String code = buildCloneCodeWithFallback(ids, dx, dy, useSelection);

        try {
            TaskResult result = executeCodeUseCase.execute(ExecuteCodeCommand.of(code));
            if (!result.isSuccess()) return formatError(result.getError().orElse("Unknown error"));

            Object data = result.getData().orElse(null);
            if (data instanceof java.util.Map) {
                Object id = ((java.util.Map<?, ?>) data).get("cloneId");
                if (id != null) {
                    return String.format(
                        "Successfully cloned shape.\n\nClone ID: %s\n\nTIP: Save this ID to manipulate the clone.",
                        id.toString()
                    );
                }
            }

            return "Successfully cloned shape.";
        } catch (Exception e) {
            log.error("Failed to clone shape", e);
            return formatError(e.getMessage());
        }
    }

    @Tool(description = """
        Ungroup one or more groups.

        Works with explicit IDs or current selection.
        Only groups will be ungrouped (other shapes are ignored).
        """)
    public String ungroupShapes(
        @ToolParam(description = "Group IDs (comma-separated) OR 'selection'") String groupIds
    ) {
        log.info("Tool called: ungroupShapes (ids='{}')", groupIds);

        boolean useSelection = "selection".equalsIgnoreCase(groupIds.trim());
        List<String> ids = useSelection ? List.of() : parseShapeIds(groupIds);

        String code = buildUngroupCodeWithFallback(ids, useSelection);

        return executeAndFormatIds(code, "ungrouped");
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

    private String executeAndFormatIds(String code, String operationLabel) {
        try {
            TaskResult result = executeCodeUseCase.execute(ExecuteCodeCommand.of(code));

            if (!result.isSuccess()) {
                return formatError(result.getError().orElse("Unknown error"));
            }

            return extractAndFormatResult(result, operationLabel);
        } catch (Exception e) {
            log.error("Failed to {}", operationLabel, e);
            return formatError(e.getMessage());
        }
    }

    private String buildZOrderCodeWithFallback(
        List<String> shapeIds,
        String methodName,
        boolean forceSelection
    ) {
        StringBuilder code = new StringBuilder();

        if (forceSelection || shapeIds.isEmpty()) {
            code.append("const shapes = penpot.selection;\n");
            code.append("console.log('[ZOrder] Using selection:', shapes.length);\n");
        } else {
            code.append("const shapes = [];\n");
            for (String id : shapeIds) {
                code.append(String.format("""
                    try {
                        const shape = penpot.currentPage.getShapeById('%s');
                        if (shape) shapes.push(shape);
                    } catch (e) {
                        console.log('[ZOrder] Invalid ID: %s');
                    }
                    """, id, id));
            }
            code.append("""
                if (shapes.length === 0) {
                    console.log('[ZOrder] No valid IDs, using selection');
                    shapes.push(...penpot.selection);
                }
                """);
        }

        code.append("""
            if (!shapes || shapes.length === 0) {
                throw new Error('No shapes to reorder. Provide valid IDs or select shapes in Penpot.');
            }
            shapes.forEach(s => {
                if (s && typeof s.%s === 'function') s.%s();
            });
            return { ids: shapes.map(s => s.id) };
            """.formatted(methodName, methodName));

        return code.toString();
    }

    private String buildMoveToBoardCodeWithFallback(
        List<String> shapeIds,
        String boardId,
        boolean forceSelection
    ) {
        StringBuilder code = new StringBuilder();

        code.append(String.format("""
            const board = penpot.currentPage.getShapeById('%s');
            if (!board) throw new Error('Board not found: %s');
            """, boardId, boardId));

        if (forceSelection || shapeIds.isEmpty()) {
            code.append("const shapes = penpot.selection;\n");
            code.append("console.log('[MoveToBoard] Using selection:', shapes.length);\n");
        } else {
            code.append("const shapes = [];\n");
            for (String id : shapeIds) {
                code.append(String.format("""
                    try {
                        const shape = penpot.currentPage.getShapeById('%s');
                        if (shape) shapes.push(shape);
                    } catch (e) {
                        console.log('[MoveToBoard] Invalid ID: %s');
                    }
                    """, id, id));
            }
            code.append("""
                if (shapes.length === 0) {
                    console.log('[MoveToBoard] No valid IDs, using selection');
                    shapes.push(...penpot.selection);
                }
                """);
        }

        code.append("""
            if (!shapes || shapes.length === 0) {
                throw new Error('No shapes to move. Provide valid IDs or select shapes in Penpot.');
            }

            shapes.forEach(s => {
                // Preserve board position if available
                const bx = (typeof s.boardX === 'number') ? s.boardX : s.x;
                const by = (typeof s.boardY === 'number') ? s.boardY : s.y;

                // Re-parent to board
                if (typeof board.appendChild === 'function') {
                    board.appendChild(s);
                } else if (typeof s.setParent === 'function') {
                    s.setParent(board);
                }

                // Restore position (best effort, depending on Penpot object model)
                if (typeof s.boardX === 'number') {
                    s.boardX = bx;
                    s.boardY = by;
                } else {
                    s.x = bx;
                    s.y = by;
                }
            });

            return { ids: shapes.map(s => s.id), boardId: board.id };
            """);

        return code.toString();
    }

    private String buildRemoveCodeWithFallback(
        List<String> shapeIds,
        boolean forceSelection
    ) {
        StringBuilder code = new StringBuilder();

        if (forceSelection || shapeIds.isEmpty()) {
            code.append("const shapes = penpot.selection;\n");
            code.append("console.log('[Remove] Using selection:', shapes.length);\n");
        } else {
            code.append("const shapes = [];\n");
            for (String id : shapeIds) {
                code.append(String.format("""
                    try {
                        const shape = penpot.currentPage.getShapeById('%s');
                        if (shape) shapes.push(shape);
                    } catch (e) {
                        console.log('[Remove] Invalid ID: %s');
                    }
                    """, id, id));
            }
            code.append("""
                if (shapes.length === 0) {
                    console.log('[Remove] No valid IDs, using selection');
                    shapes.push(...penpot.selection);
                }
                """);
        }

        code.append("""
            if (!shapes || shapes.length === 0) {
                throw new Error('No shapes to remove. Provide valid IDs or select shapes in Penpot.');
            }

            const ids = shapes.map(s => s.id);
            shapes.forEach(s => {
                if (s && typeof s.remove === 'function') s.remove();
            });

            return { ids };
            """);

        return code.toString();
    }

    private String buildCloneCodeWithFallback(
        List<String> shapeIds,
        int dx,
        int dy,
        boolean forceSelection
    ) {
        StringBuilder code = new StringBuilder();

        if (forceSelection || shapeIds.isEmpty()) {
            code.append("const shapes = penpot.selection;\n");
            code.append("console.log('[Clone] Using selection:', shapes.length);\n");
            code.append("const shape = shapes && shapes.length ? shapes[0] : null;\n");
        } else {
            String firstId = shapeIds.get(0);
            code.append(String.format("""
                let shape = null;
                try { shape = penpot.currentPage.getShapeById('%s'); } catch (e) {}
                if (!shape) {
                    const sel = penpot.selection;
                    shape = (sel && sel.length) ? sel[0] : null;
                }
                """, firstId));
        }

        code.append("""
            if (!shape) throw new Error('No shape to clone. Provide a valid ID or select a shape in Penpot.');

            if (typeof shape.clone !== 'function') {
                throw new Error('Selected shape cannot be cloned (clone() missing).');
            }

            const clone = shape.clone();
            if (!clone) throw new Error('Clone failed (clone returned null).');

            // Offset position (best effort)
            if (typeof clone.boardX === 'number' && typeof shape.boardX === 'number') {
                clone.boardX = shape.boardX + %d;
                clone.boardY = shape.boardY + %d;
            } else {
                clone.x = shape.x + %d;
                clone.y = shape.y + %d;
            }

            return { cloneId: clone.id };
            """.formatted(dx, dy, dx, dy));

        return code.toString();
    }

    private String buildUngroupCodeWithFallback(
        List<String> groupIds,
        boolean forceSelection
    ) {
        StringBuilder code = new StringBuilder();

        if (forceSelection || groupIds.isEmpty()) {
            code.append("const shapes = penpot.selection;\n");
            code.append("console.log('[Ungroup] Using selection:', shapes.length);\n");
        } else {
            code.append("const shapes = [];\n");
            for (String id : groupIds) {
                code.append(String.format("""
                    try {
                        const shape = penpot.currentPage.getShapeById('%s');
                        if (shape) shapes.push(shape);
                    } catch (e) {
                        console.log('[Ungroup] Invalid ID: %s');
                    }
                    """, id, id));
            }
            code.append("""
                if (shapes.length === 0) {
                    console.log('[Ungroup] No valid IDs, using selection');
                    shapes.push(...penpot.selection);
                }
                """);
        }

        code.append("""
            if (!shapes || shapes.length === 0) {
                throw new Error('No shapes provided to ungroup. Provide group IDs or select groups in Penpot.');
            }

            const groups = shapes.filter(s => {
                const t = (s && s.type) ? String(s.type).toLowerCase() : '';
                return t === 'group' || s.isGroup === true;
            });

            if (groups.length === 0) {
                throw new Error('No groups found to ungroup (selection/IDs contain no groups).');
            }

            if (typeof penpot.ungroup !== 'function') {
                throw new Error('penpot.ungroup() is not available in this Penpot API context.');
            }

            penpot.ungroup(groups);
            return { ids: groups.map(g => g.id) };
            """);

        return code.toString();
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