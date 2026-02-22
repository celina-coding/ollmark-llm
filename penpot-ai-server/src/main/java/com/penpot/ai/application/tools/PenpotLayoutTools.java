package com.penpot.ai.application.tools;

import com.penpot.ai.application.tools.support.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.*;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * Tools pour l'alignement, distribution et hiérarchie de formes dans Penpot.
 *
 * <p>La collection de formes réutilise {@link PenpotJsSnippets#collectShapesOrFallback}.</p>
 * <p>L'exécution est déléguée à {@link PenpotToolExecutor}.</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PenpotLayoutTools {

    private final PenpotToolExecutor toolExecutor;

    private static final Set<String> VALID_ALIGNMENTS =
        Set.of("left", "center", "right", "top", "middle", "bottom");

    // ==================== ALIGN / DISTRIBUTE / GROUP ====================

    @Tool(description = """
        Align multiple shapes along a specified axis.
        Horizontal alignments: left, center, right
        Vertical alignments: top, middle, bottom
        Provide comma-separated UUIDs or 'selection' to use current selection.
        """)
    public String alignShapes(
        @ToolParam(description = "Shape IDs (comma-separated) OR 'selection'") String shapeIds,
        @ToolParam(description = "Alignment: left, center, right, top, middle, bottom") String alignment
    ) {
        if (!VALID_ALIGNMENTS.contains(alignment.toLowerCase()))
            return ToolResponseBuilder.error("Invalid alignment: " + alignment);

        log.info("Tool called: alignShapes (ids='{}', alignment='{}')", shapeIds, alignment);
        List<String> ids = resolveIds(shapeIds);
        String code = PenpotJsSnippets.collectShapesOrFallback(ids, "Align")
            + "if (shapes.length < 2) throw new Error('Need at least 2 shapes to align. Current: ' + shapes.length);\n"
            + buildAlignmentLogic(alignment)
            + "return { aligned: shapes.length, ids: shapes.map(s => s.id) };\n";

        return toolExecutor.executeMultiShape(code, "aligned");
    }

    @Tool(description = """
        Distribute shapes evenly along an axis (requires at least 3 shapes).
        Modes: horizontal, vertical
        """)
    public String distributeShapes(
        @ToolParam(description = "Shape IDs (comma-separated) OR 'selection'") String shapeIds,
        @ToolParam(description = "Axis: horizontal or vertical") String axis
    ) {
        if (!axis.equalsIgnoreCase("horizontal") && !axis.equalsIgnoreCase("vertical"))
            return ToolResponseBuilder.error("Invalid axis: " + axis);

        log.info("Tool called: distributeShapes (ids='{}', axis='{}')", shapeIds, axis);
        List<String> ids = resolveIds(shapeIds);
        String code = PenpotJsSnippets.collectShapesOrFallback(ids, "Distribute")
            + "if (shapes.length < 3) throw new Error('Need at least 3 shapes to distribute. Current: ' + shapes.length);\n"
            + buildDistributionLogic(axis)
            + "return { distributed: shapes.length, ids: shapes.map(s => s.id) };\n";

        return toolExecutor.executeMultiShape(code, "distributed");
    }

    @Tool(description = "Group shapes together. Returns the ID of the created group.")
    public String groupShapes(
        @ToolParam(description = "Shape IDs (comma-separated) OR 'selection'") String shapeIds,
        @ToolParam(description = "Group name", required = false) String groupName
    ) {
        log.info("Tool called: groupShapes (ids='{}', name='{}')", shapeIds, groupName);
        List<String> ids = resolveIds(shapeIds);
        StringBuilder code = new StringBuilder(PenpotJsSnippets.collectShapesOrFallback(ids, "Group"));
        code.append("if (shapes.length < 2) throw new Error('Need at least 2 shapes to group. Current: ' + shapes.length);\n");
        code.append("const group = penpot.group(shapes);\n");
        if (groupName != null && !groupName.isBlank())
            code.append(String.format("group.name = '%s';\n", PenpotJsSnippets.escapeJsString(groupName)));
        code.append("return { groupId: group.id, shapeCount: shapes.length };\n");

        return toolExecutor.createGroup(code.toString());
    }

    @Tool(description = "Ungroup one or more groups. Only groups will be ungrouped (other shapes are ignored).")
    public String ungroupShapes(
        @ToolParam(description = "Group IDs (comma-separated) OR 'selection'") String groupIds
    ) {
        log.info("Tool called: ungroupShapes (ids='{}')", groupIds);
        List<String> ids = resolveIds(groupIds);
        String code = PenpotJsSnippets.collectShapesOrFallback(ids, "Ungroup")
            + """
              if (!shapes || shapes.length === 0)
                  throw new Error('No shapes provided to ungroup.');
              const groups = shapes.filter(s => {
                  const t = (s && s.type) ? String(s.type).toLowerCase() : '';
                  return t === 'group' || s.isGroup === true;
              });
              if (groups.length === 0) throw new Error('No groups found in selection/IDs.');
              if (typeof penpot.ungroup !== 'function') throw new Error('penpot.ungroup() is not available.');
              penpot.ungroup(groups);
              return { ids: groups.map(g => g.id) };
              """;

        return toolExecutor.executeMultiShape(code, "ungrouped");
    }

    // ==================== Z-ORDER ====================

    @Tool(description = "Send shapes one step backward in the layer order (z-index).")
    public String sendShapeBackward(
        @ToolParam(description = "Shape IDs (comma-separated) OR 'selection'") String shapeIds
    ) {
        log.info("Tool called: sendShapeBackward");
        return toolExecutor.executeMultiShape(buildZOrderCode(resolveIds(shapeIds), "sendBackward"), "sent backward");
    }

    @Tool(description = "Bring shapes one step forward in the layer order (z-index).")
    public String sendShapeFrontward(
        @ToolParam(description = "Shape IDs (comma-separated) OR 'selection'") String shapeIds
    ) {
        log.info("Tool called: sendShapeFrontward");
        return toolExecutor.executeMultiShape(buildZOrderCode(resolveIds(shapeIds), "bringForward"), "brought forward");
    }

    @Tool(description = "Send shapes to the very back (bottom) of their siblings list.")
    public String sendShapeToTheBack(
        @ToolParam(description = "Shape IDs (comma-separated) OR 'selection'") String shapeIds
    ) {
        log.info("Tool called: sendShapeToTheBack");
        return toolExecutor.executeMultiShape(buildZOrderCode(resolveIds(shapeIds), "sendToBack"), "sent to back");
    }

    @Tool(description = "Bring shapes to the very front (top) of their siblings list.")
    public String sendShapeToTheFront(
        @ToolParam(description = "Shape IDs (comma-separated) OR 'selection'") String shapeIds
    ) {
        log.info("Tool called: sendShapeToTheFront");
        return toolExecutor.executeMultiShape(buildZOrderCode(resolveIds(shapeIds), "bringToFront"), "brought to front");
    }

    // ==================== HIERARCHY ====================

    @Tool(description = """
        Move one or more shapes into a specific board.
        Shapes will be re-parented to the target board, preserving their position.
        """)
    public String addShapeToBoard(
        @ToolParam(description = "Shape IDs (comma-separated) OR 'selection'") String shapeIds,
        @ToolParam(description = "Target board ID") String boardId
    ) {
        log.info("Tool called: addShapeToBoard (shapeIds='{}', boardId='{}')", shapeIds, boardId);
        return toolExecutor.executeMultiShape(buildMoveToBoardCode(resolveIds(shapeIds), boardId), "moved to board");
    }

    @Tool(description = "Remove one or more shapes from their parent (delete from the document tree).")
    public String removeShapeFromParent(
        @ToolParam(description = "Shape IDs (comma-separated) OR 'selection'") String shapeIds
    ) {
        log.info("Tool called: removeShapeFromParent (ids='{}')", shapeIds);
        String code = PenpotJsSnippets.collectShapesOrFallback(resolveIds(shapeIds), "Remove")
            + """
              if (!shapes || shapes.length === 0)
                  throw new Error('No shapes to remove. Provide valid IDs or select shapes in Penpot.');
              const ids = shapes.map(s => s.id);
              shapes.forEach(s => { if (s && typeof s.remove === 'function') s.remove(); });
              return { ids };
              """;

        return toolExecutor.executeMultiShape(code, "removed");
    }

    @Tool(description = """
        Clone a shape (duplicate it). Returns the new cloned shape ID.
        You can optionally offset the clone position.
        """)
    public String cloneShape(
        @ToolParam(description = "Shape ID OR 'selection'") String shapeId,
        @ToolParam(description = "Offset X for the clone (default 20)", required = false) Integer offsetX,
        @ToolParam(description = "Offset Y for the clone (default 20)", required = false) Integer offsetY
    ) {
        log.info("Tool called: cloneShape (id='{}', dx={}, dy={})", shapeId, offsetX, offsetY);
        int dx = (offsetX != null) ? offsetX : 20;
        int dy = (offsetY != null) ? offsetY : 20;
        List<String> ids = resolveIds(shapeId);

        String code = PenpotJsSnippets.findFirstShapeOrFallback(ids)
            + String.format("""
              if (typeof shape.clone !== 'function') throw new Error('Selected shape cannot be cloned.');
              const clone = shape.clone();
              if (!clone) throw new Error('Clone failed (clone returned null).');
              if (typeof clone.boardX === 'number' && typeof shape.boardX === 'number') {
                  clone.boardX = shape.boardX + %d;
                  clone.boardY = shape.boardY + %d;
              } else {
                  clone.x = shape.x + %d;
                  clone.y = shape.y + %d;
              }
              return { cloneId: clone.id };
              """, dx, dy, dx, dy);

        return toolExecutor.cloneShape(code);
    }

    // ==================== CODE GENERATION HELPERS ====================

    /** Génère la logique JS d'alignement pour un type donné. Extrait du switch pour lisibilité (SRP). */
    private String buildAlignmentLogic(String alignment) {
        return switch (alignment.toLowerCase()) {
            case "left"   -> "const minX = Math.min(...shapes.map(s => s.x));\n"
                           + "shapes.forEach(s => s.x = minX);\n";
            case "center" -> "const avgCX = shapes.reduce((sum, s) => sum + s.x + s.width/2, 0) / shapes.length;\n"
                           + "shapes.forEach(s => s.x = avgCX - s.width/2);\n";
            case "right"  -> "const maxX = Math.max(...shapes.map(s => s.x + s.width));\n"
                           + "shapes.forEach(s => s.x = maxX - s.width);\n";
            case "top"    -> "const minY = Math.min(...shapes.map(s => s.y));\n"
                           + "shapes.forEach(s => s.y = minY);\n";
            case "middle" -> "const avgCY = shapes.reduce((sum, s) => sum + s.y + s.height/2, 0) / shapes.length;\n"
                           + "shapes.forEach(s => s.y = avgCY - s.height/2);\n";
            case "bottom" -> "const maxY = Math.max(...shapes.map(s => s.y + s.height));\n"
                           + "shapes.forEach(s => s.y = maxY - s.height);\n";
            default       -> throw new IllegalArgumentException("Unknown alignment: " + alignment);
        };
    }

    /** Génère la logique JS de distribution pour un axe donné. */
    private String buildDistributionLogic(String axis) {
        if (axis.equalsIgnoreCase("horizontal")) {
            return """
                shapes.sort((a, b) => a.x - b.x);
                const hFirst = shapes[0], hLast = shapes[shapes.length - 1];
                const hTotal = (hLast.x + hLast.width) - hFirst.x;
                const hTotalW = shapes.reduce((sum, s) => sum + s.width, 0);
                const hGap = (hTotal - hTotalW) / (shapes.length - 1);
                let hCurX = hFirst.x;
                shapes.forEach(s => { s.x = hCurX; hCurX += s.width + hGap; });
                """;
        } else {
            return """
                shapes.sort((a, b) => a.y - b.y);
                const vFirst = shapes[0], vLast = shapes[shapes.length - 1];
                const vTotal = (vLast.y + vLast.height) - vFirst.y;
                const vTotalH = shapes.reduce((sum, s) => sum + s.height, 0);
                const vGap = (vTotal - vTotalH) / (shapes.length - 1);
                let vCurY = vFirst.y;
                shapes.forEach(s => { s.y = vCurY; vCurY += s.height + vGap; });
                """;
        }
    }

    private String buildZOrderCode(List<String> ids, String methodName) {
        return PenpotJsSnippets.collectShapesOrFallback(ids, "ZOrder")
            + "if (!shapes || shapes.length === 0) throw new Error('No shapes to reorder.');\n"
            + String.format("""
              shapes.forEach(s => {
                  if (s && typeof s.%s === 'function') s.%s();
              });
              return { ids: shapes.map(s => s.id) };
              """, methodName, methodName);
    }

    private String buildMoveToBoardCode(List<String> ids, String boardId) {
        return String.format("""
            const board = penpot.currentPage.getShapeById('%s');
            if (!board) throw new Error('Board not found: %s');
            """, boardId, boardId)
            + PenpotJsSnippets.collectShapesOrFallback(ids, "MoveToBoard")
            + """
              if (!shapes || shapes.length === 0) throw new Error('No shapes to move.');
              shapes.forEach(s => {
                  const bx = (typeof s.boardX === 'number') ? s.boardX : s.x;
                  const by = (typeof s.boardY === 'number') ? s.boardY : s.y;
                  if (typeof board.appendChild === 'function') board.appendChild(s);
                  else if (typeof s.setParent === 'function') s.setParent(board);
                  if (typeof s.boardX === 'number') { s.boardX = bx; s.boardY = by; }
                  else { s.x = bx; s.y = by; }
              });
              return { ids: shapes.map(s => s.id), boardId: board.id };
              """;
    }

    // ==================== INPUT PARSING ====================

    /** Parse une chaîne CSV d'IDs. Retourne une liste vide si c'est "selection". */
    private List<String> resolveIds(String rawInput) {
        if (rawInput == null || rawInput.isBlank() || rawInput.trim().equalsIgnoreCase("selection")) {
            return List.of();
        }
        return Arrays.stream(rawInput.split(","))
            .map(String::trim)
            .filter(s -> !s.isEmpty())
            .toList();
    }
}