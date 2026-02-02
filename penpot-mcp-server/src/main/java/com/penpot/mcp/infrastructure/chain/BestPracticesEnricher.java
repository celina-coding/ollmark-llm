package com.penpot.mcp.infrastructure.chain;

import com.penpot.mcp.core.domain.AiContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import java.util.*;

/**
 * Enrichisseur qui ajoute les best practices et contraintes.
 */
@Slf4j
@Component
public class BestPracticesEnricher extends ContextEnricher {

    private static final List<String> BEST_PRACTICES = List.of(
        "Always check if shapes exist before accessing properties",
        "Use penpotUtils.findShape() for searching by criteria",
        "Prefer createRectangle() over createShape() for rectangles",
        "Set fills as array: shape.fills = [{ fillColor: '#RRGGBB' }]",
        "Use try-catch for error handling",
        "Return the created shape or meaningful result"
    );

    private static final List<String> CONSTRAINTS = List.of(
        "NO require() or import statements",
        "NO localStorage or sessionStorage",
        "NO markdown backticks in output",
        "NO comments unless specifically requested"
    );

    @Override
    protected AiContext doEnrich(AiContext context) {
        log.debug("Enriching context with best practices");
        return context
            .addBestPractices(BEST_PRACTICES)
            .addConstraints(CONSTRAINTS);
    }

    @Override
    protected boolean shouldEnrich(AiContext context) {
        return context.isCodeGeneration();
    }
}