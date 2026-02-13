package com.penpot.ai.application.tools;

import com.penpot.ai.core.ports.in.ExecuteCodeUseCase;
import com.penpot.ai.core.domain.*;
import com.penpot.ai.shared.util.JsonUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.*;
import org.springframework.stereotype.Component;

/**
 * Tools pour l'inspection et la découverte des éléments dans Penpot.
 * 
 * <h2>Responsabilité unique</h2>
 * Permet à l'IA de "voir" ce qui est sur la page :
 * - Lister les formes
 * - Obtenir les détails d'une forme
 * - Rechercher par nom
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PenpotInspectorTools {

    private final ExecuteCodeUseCase executeCodeUseCase;

    /**
     * Liste tous les éléments présents sur la page courante.
     * Utile pour que l'IA puisse connaître le contexte ou retrouver des IDs.
     * 
     * @return Liste formatée des éléments (Type, Nom, ID)
     */
    @Tool(description = """
        List all elements (shapes, boards, text) on the current page.
        
        Use this to:
        - Discover what is on the page.
        - Find IDs of existing shapes to modify them.
        - Check if a specific element exists.
        
        Returns a list with [Type] Name (ID).
        """)
    public String listElements() {
        log.info("Tool called: listElements");

        String code = """
            const shapes = penpot.currentPage.findShapes({});
            const summary = shapes.map(s => {
                return `[${s.type}] "${s.name}" (ID: ${s.id})`;
            });
            return summary.join('\\n');
        """;

        try {
            TaskResult result = executeCodeUseCase.execute(
                ExecuteCodeCommand.of(code)
            );

            if (!result.isSuccess()) {
                return formatError(result.getError().orElse("Unknown error"));
            }

            log.debug("Elements listed");
            return result.getData()
                .map(Object::toString)
                .orElse("No elements found.");

        } catch (Exception e) {
            log.error("Failed to list elements", e);
            return formatError(e.getMessage());
        }
    }

    private String formatError(String errorMessage) {
        return String.format(
            "{\"success\": false, \"error\": %s}",
            JsonUtils.escapeJson(errorMessage)
        );
    }
}