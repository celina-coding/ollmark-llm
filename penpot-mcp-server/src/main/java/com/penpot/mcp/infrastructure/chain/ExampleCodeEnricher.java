package com.penpot.mcp.infrastructure.chain;

import com.penpot.mcp.core.domain.AiContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Enrichisseur qui ajoute des exemples de code pertinents.
 */
@Slf4j
@Component
public class ExampleCodeEnricher extends ContextEnricher {

    private static final Map<String, String> CODE_EXAMPLES = Map.of(
        "rectangle", """
            const rect = penpot.createRectangle();
            rect.resize(100, 50);
            rect.fills = [{ fillColor: '#FF0000' }];
            """,
        "board", """
            const board = penpot.createBoard();
            board.name = 'My Board';
            board.resize(800, 600);
            """,
        "text", """
            const text = penpot.createText('Hello World');
            text.fontSize = 24;
            text.fills = [{ fillColor: '#000000' }];
            """
    );

    @Override
    protected AiContext doEnrich(AiContext context) {
        log.debug("Enriching context with code examples");
        List<String> relevantExamples = findRelevantExamples(context);
        if (relevantExamples.isEmpty()) return context;

        return context.addExamples(relevantExamples);
    }

    @Override
    protected boolean shouldEnrich(AiContext context) {
        return context.isCodeGeneration() && context.requiresExamples();
    }

    private List<String> findRelevantExamples(AiContext context) {
        String task = context.getTask().toLowerCase();
        return CODE_EXAMPLES.entrySet().stream()
            .filter(entry -> task.contains(entry.getKey()))
            .map(Map.Entry::getValue)
            .collect(Collectors.toList());
    }
}