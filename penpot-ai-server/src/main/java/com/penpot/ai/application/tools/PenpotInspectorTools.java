package com.penpot.ai.application.tools;

import com.penpot.ai.application.tools.support.PenpotToolExecutor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.*;
import org.springframework.stereotype.Component;

/**
 * Tools pour l'inspection des éléments de la page Penpot.
 *
 * <p>L'exécution est déléguée à {@link PenpotToolExecutor#execute} avec un mapper personnalisé.</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PenpotInspectorTools {

    private final PenpotToolExecutor toolExecutor;

    /**
     * Liste tous les éléments présents sur la page Penpot courante.
     *
     * @return JSON contenant la liste des shapes avec type, nom et ID
     */
    @Tool(
        description = """
            List all elements/shapes present on the current Penpot page.
            Returns each shape's type, name, and ID.
            Use this to understand the current page structure before creating or modifying elements.""",
        returnDirect = true
    )
    public String listElements() {
        log.info("Tool called: listElements");
        return toolExecutor.execute(
            """
            const shapes = penpot.currentPage.findShapes({});
            const summary = shapes.map(s => `[${s.type}] "${s.name}" (ID: ${s.id})`);
            return summary.join('\\n');
            """,
            "list elements",
            result -> result.getData().map(Object::toString).orElse("No elements found.")
        );
    }
}