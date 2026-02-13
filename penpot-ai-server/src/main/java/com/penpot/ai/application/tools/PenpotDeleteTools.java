package com.penpot.ai.application.tools;

import com.penpot.ai.core.ports.in.ExecuteCodeUseCase;
import com.penpot.ai.core.domain.*;
import com.penpot.ai.shared.util.JsonUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.*;
import org.springframework.stereotype.Component;

/**
 * Tools pour la suppression d'éléments dans Penpot.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PenpotDeleteTools {

    private final ExecuteCodeUseCase executeCodeUseCase;

    /**
     * Supprime les éléments actuellement sélectionnés dans Penpot.
     * * @return JSON confirmant le succès ou l'échec
     */
    @Tool(description = """
        Delete all currently selected shapes or elements in Penpot.
        
        Use this when the user says "delete this", "remove selection", 
        "clear selected items", or "get rid of the current shape".
        """)
    public String deleteSelection() {
        log.info("Tool called: deleteSelection");

        // Code JavaScript Penpot pour supprimer la sélection
        String code = """
            const selection = penpot.selection;
            if (selection.length === 0) {
                return "No items selected";
            }
            const count = selection.length;
            selection.forEach(shape => shape.remove());
            return "Deleted " + count + " items";
            """;

        try {
            TaskResult result = executeCodeUseCase.execute(
                    ExecuteCodeCommand.of(code)
            );

            if (!result.isSuccess()) {
                return formatError(result.getError().orElse("Unknown error during deletion"));
            }

            String message = result.getData()
                    .map(Object::toString)
                    .orElse("Selection cleared");

            return formatSuccess(message);
        } catch (Exception e) {
            log.error("Failed to delete selection", e);
            return formatError(e.getMessage());
        }
    }

    /**
     * Supprime un élément spécifique par son ID.
     * * @param shapeId L'UUID de la forme à supprimer
     * @return JSON confirmant la suppression
     */
    @Tool(description = """
    Delete a specific shape by its ID.
    
    Use this when you have the ID of a shape and the user wants to remove it specifically.
    """)
    public String deleteShapeById(
            @ToolParam(description = "The UUID of the shape to delete") String shapeId
    ) {
        log.info("Tool called: deleteShapeById (id={})", shapeId);

        String code = String.format("""
        const shape = penpot.currentPage.getShapeById('%s');
        if (shape) {
            shape.remove();
            return "Shape deleted";
        }
        return "Shape not found";
        """, shapeId);

        try {
            TaskResult result = executeCodeUseCase.execute(
                    ExecuteCodeCommand.of(code)
            );

            if (!result.isSuccess()) {
                return formatError(result.getError().orElse("Error deleting shape " + shapeId));
            }

            return formatSuccess("Shape " + shapeId + " removed successfully.");
        } catch (Exception e) {
            log.error("Failed to delete shape by ID", e);
            return formatError(e.getMessage());
        }
    }

    /**
     * Supprime uniquement les éléments sélectionnés qui sont sur le bord (board/artboard)
     * et qui ont été créés manuellement (pas des composants ou instances).
     *
     * @return JSON confirmant le nombre d'éléments supprimés
     */
    @Tool(description = """
    Delete only the selected shapes that are on the board/artboard and were created manually.
    
    This excludes component instances and only removes manually created shapes.
    Use this when the user wants to clean up manual shapes from the board while preserving components.
    """)
    public String deleteManualShapesOnBoard() {
        log.info("Tool called: deleteManualShapesOnBoard");

        // Code JavaScript pour filtrer et supprimer uniquement les formes manuelles sur le board
        String code = """
        const selection = penpot.selection;
        if (selection.length === 0) {
            return "No items selected";
        }
        
        let deletedCount = 0;
        
        selection.forEach(shape => {
            // Vérifie si c'est une forme manuelle (pas un composant ou instance)
            const isManual = !shape.componentId && !shape.componentFile && !shape.mainInstance;
            
            // Vérifie si la forme est directement sur le board (parent est la page ou un board)
            const isOnBoard = shape.parent && (shape.parent.type === 'page' || shape.parent.type === 'frame');
            
            if (isManual && isOnBoard) {
                shape.remove();
                deletedCount++;
            }
        });
        
        if (deletedCount === 0) {
            return "No manual shapes on board found in selection";
        }
        
        return "Deleted " + deletedCount + " manual shape(s) from board";
        """;

        try {
            TaskResult result = executeCodeUseCase.execute(
                    ExecuteCodeCommand.of(code)
            );

            if (!result.isSuccess()) {
                return formatError(result.getError().orElse("Unknown error during deletion"));
            }

            String message = result.getData()
                    .map(Object::toString)
                    .orElse("Manual shapes removed from board");

            return formatSuccess(message);
        } catch (Exception e) {
            log.error("Failed to delete manual shapes on board", e);
            return formatError(e.getMessage());
        }
    }

    /**
     * Supprime TOUS les éléments créés manuellement sur le bord actuel,
     * sans tenir compte de la sélection.
     *
     * @return JSON confirmant le nombre d'éléments supprimés
     */
    @Tool(description = """
    Delete ALL manually created shapes from the current board/artboard.
    
    This removes all manual shapes (non-components) from the board, regardless of selection.
    Use with caution - this will delete all manually drawn elements on the current page.
    """)
    public String deleteAllManualShapesFromBoard() {
        log.info("Tool called: deleteAllManualShapesFromBoard");

        String code = """
        const currentPage = penpot.currentPage;
        if (!currentPage) {
            return "No active page found";
        }
        
        let deletedCount = 0;
        
        // Parcourt tous les enfants de la page
        function deleteManualShapes(element) {
            if (!element.children) return;
            
            // Crée une copie du tableau car on va modifier pendant l'itération
            const children = [...element.children];
            
            children.forEach(child => {
                const isManual = !child.componentId && !child.componentFile && !child.mainInstance;
                
                if (isManual && child.type !== 'page') {
                    child.remove();
                    deletedCount++;
                } else {
                    // Récursion pour parcourir les groupes et frames
                    deleteManualShapes(child);
                }
            });
        }
        
        deleteManualShapes(currentPage);
        
        if (deletedCount === 0) {
            return "No manual shapes found on board";
        }
        
        return "Deleted " + deletedCount + " manual shape(s) from board";
        """;

        try {
            TaskResult result = executeCodeUseCase.execute(
                    ExecuteCodeCommand.of(code)
            );

            if (!result.isSuccess()) {
                return formatError(result.getError().orElse("Unknown error during deletion"));
            }

            String message = result.getData()
                    .map(Object::toString)
                    .orElse("All manual shapes removed from board");

            return formatSuccess(message);
        } catch (Exception e) {
            log.error("Failed to delete all manual shapes from board", e);
            return formatError(e.getMessage());
        }
    }

    private String formatSuccess(String message) {
        return String.format(
                "{\"success\": true, \"message\": %s}",
                JsonUtils.escapeJson(message)
        );
    }

    private String formatError(String errorMessage) {
        return String.format(
                "{\"success\": false, \"error\": %s}",
                JsonUtils.escapeJson(errorMessage)
        );
    }
}