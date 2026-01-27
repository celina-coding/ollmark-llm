package com.penpot.mcp.infrastructure.factory;

import com.penpot.mcp.infrastructure.chain.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Factory pour construire la chaîne d'enrichissement du contexte.
 * Encapsule la logique de construction et d'ordonnancement des enrichisseurs.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ContextEnrichmentChainFactory {

    private final ApiDocumentationEnricher apiDocsEnricher;
    private final ExampleCodeEnricher exampleEnricher;
    private final BestPracticesEnricher bestPracticesEnricher;
    private final ConversationHistoryEnricher historyEnricher;

    /**
     * Crée la chaîne complète d'enrichissement.
     * L'ordre est important : documentation → exemples → best practices → historique.
     * 
     * @return le premier enrichisseur de la chaîne
     */
    public ContextEnricher createChain() {
        log.debug("Building context enrichment chain");

        apiDocsEnricher.setNext(exampleEnricher);
        exampleEnricher.setNext(bestPracticesEnricher);
        bestPracticesEnricher.setNext(historyEnricher);

        log.debug("Context enrichment chain built: " +
            "ApiDocs -> Examples -> BestPractices -> History");

        return apiDocsEnricher;
    }

    /**
     * Crée une chaîne personnalisée pour le chat (sans génération de code).
     * 
     * @return le premier enrichisseur de la chaîne chat
     */
    public ContextEnricher createChatChain() {
        log.debug("Building chat enrichment chain");
        return historyEnricher;
    }

    /**
     * Crée une chaîne minimale pour les cas simples.
     * 
     * @return le premier enrichisseur de la chaîne minimale
     */
    public ContextEnricher createMinimalChain() {
        log.debug("Building minimal enrichment chain");
        apiDocsEnricher.setNext(bestPracticesEnricher);
        return apiDocsEnricher;
    }
}