package com.penpot.mcp.infrastructure.chain;

import com.penpot.mcp.core.domain.AiContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Enrichisseur qui ajoute le contexte de l'utilisateur depuis l'historique.
 */
@Slf4j
@Component
public class ConversationHistoryEnricher extends ContextEnricher {

    private static final int MAX_HISTORY_ITEMS = 5;

    @Override
    protected AiContext doEnrich(AiContext context) {
        log.debug("Enriching context with conversation history");
        if (context.getConversationHistory().isEmpty()) return context;

        List<String> recentHistory = context.getConversationHistory()
            .stream()
            .limit(MAX_HISTORY_ITEMS)
            .collect(Collectors.toList());

        return context.withTrimmedHistory(recentHistory);
    }

    @Override
    protected boolean shouldEnrich(AiContext context) {
        return !context.getConversationHistory().isEmpty();
    }
}
