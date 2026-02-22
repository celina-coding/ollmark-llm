package com.penpot.ai.application.tools;

import com.penpot.ai.application.tools.support.PenpotToolExecutor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.*;
import org.springframework.stereotype.Component;

/**
 * Tools pour la suppression d'éléments dans Penpot.
 *
 * <p>L'exécution et le formatage sont délégués à {@link PenpotToolExecutor#executeDelete}.</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PenpotDeleteTools {

    private final PenpotToolExecutor toolExecutor;

    @Tool(description = """
        Delete all currently selected shapes or elements in Penpot.
        Use this when the user says "delete this", "remove selection", or "clear selected items".
        """)
    public String deleteSelection() {
        log.info("Tool called: deleteSelection");
        return toolExecutor.executeDelete("""
            const selection = penpot.selection;
            if (selection.length === 0) return "No items selected";
            const count = selection.length;
            selection.forEach(shape => shape.remove());
            return "Deleted " + count + " items";
            """, "delete selection");
    }

    @Tool(description = """
        Delete a specific shape by its ID.
        Use this when you have the ID of a shape and the user wants to remove it specifically.
        """)
    public String deleteShapeById(
        @ToolParam(description = "The UUID of the shape to delete") String shapeId
    ) {
        log.info("Tool called: deleteShapeById (id={})", shapeId);
        return toolExecutor.executeDelete(
            String.format("""
                const shape = penpot.currentPage.getShapeById('%s');
                if (shape) { shape.remove(); return "Shape deleted"; }
                return "Shape not found";
                """, shapeId),
            "delete shape " + shapeId
        );
    }

    @Tool(description = """
        Delete only the selected shapes that are on the board and were created manually.
        Excludes component instances. Preserves components while removing manually drawn shapes.
        """)
    public String deleteManualShapesOnBoard() {
        log.info("Tool called: deleteManualShapesOnBoard");
        return toolExecutor.executeDelete("""
            const selection = penpot.selection;
            if (selection.length === 0) return "No items selected";
            let deletedCount = 0;
            selection.forEach(shape => {
                const isManual = !shape.componentId && !shape.componentFile && !shape.mainInstance;
                const isOnBoard = shape.parent && (shape.parent.type === 'page' || shape.parent.type === 'frame');
                if (isManual && isOnBoard) { shape.remove(); deletedCount++; }
            });
            return deletedCount === 0 ? "No manual shapes on board found" : "Deleted " + deletedCount + " manual shape(s) from board";
            """, "delete manual shapes on board");
    }

    @Tool(description = """
        Delete ALL manually created shapes from the current board/artboard.
        Use with caution — removes all manually drawn elements regardless of selection.
        """)
    public String deleteAllManualShapesFromBoard() {
        log.info("Tool called: deleteAllManualShapesFromBoard");
        return toolExecutor.executeDelete("""
            const currentPage = penpot.currentPage;
            if (!currentPage) return "No active page found";
            let deletedCount = 0;
            function deleteManualShapes(element) {
                if (!element.children) return;
                [...element.children].forEach(child => {
                    const isManual = !child.componentId && !child.componentFile && !child.mainInstance;
                    if (isManual && child.type !== 'page') { child.remove(); deletedCount++; }
                    else deleteManualShapes(child);
                });
            }
            deleteManualShapes(currentPage);
            return deletedCount === 0 ? "No manual shapes found" : "Deleted " + deletedCount + " manual shape(s)";
            """, "delete all manual shapes");
    }
}