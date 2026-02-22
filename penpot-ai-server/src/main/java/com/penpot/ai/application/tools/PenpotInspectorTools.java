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

    @Tool(description = """
        List all elements (shapes, boards, text) on the current page.
        Use this to discover existing elements or find IDs to modify them.
        Returns a list with [Type] Name (ID).
        """)
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