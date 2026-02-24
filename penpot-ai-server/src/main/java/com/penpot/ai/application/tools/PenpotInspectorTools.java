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

    @Tool(
        description = """
            Describe all shapes on the current page with useful properties (id, name, type, position, size, fills/strokes, text content when applicable).
            Use this to identify a target like "the green rectangle" or "the title at the top".
            Returns JSON.""",
        returnDirect = true
    )
    public String describeElementsDetailed() {
    log.info("Tool called: describeElementsDetailed");
        return toolExecutor.execute(
            """
            const shapes = penpot.currentPage.findShapes({});
            const toHex = (c) => c?.toString?.() ?? c ?? null;

            const out = shapes.map(s => {
            const base = {
                id: s.id,
                name: s.name,
                type: s.type,
                x: s.x, y: s.y,
                width: s.width, height: s.height
            };

            // fills / strokes (defensive)
            try {
                if ('fills' in s && Array.isArray(s.fills) && s.fills.length) {
                base.fills = s.fills.map(f => ({
                    fillColor: toHex(f.fillColor ?? f.color),
                    fillOpacity: f.fillOpacity ?? f.opacity ?? null,
                    fillType: f.fillType ?? f.type ?? null
                }));
                }
            } catch (e) {}

            try {
                if ('strokes' in s && Array.isArray(s.strokes) && s.strokes.length) {
                base.strokes = s.strokes.map(st => ({
                    strokeColor: toHex(st.strokeColor ?? st.color),
                    strokeOpacity: st.strokeOpacity ?? st.opacity ?? null,
                    strokeWidth: st.strokeWidth ?? st.width ?? null
                }));
                }
            } catch (e) {}

            // text content
            try {
                if (s.type === 'text' && 'content' in s) {
                base.text = s.content;
                }
            } catch (e) {}

            return base;
            });

            return JSON.stringify(out, null, 2);
            """,
            "describe elements detailed",
            result -> result.getData().map(Object::toString).orElse("[]")
        );
    }
}