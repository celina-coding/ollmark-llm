package com.penpot.mcp.application.usecases;

import com.penpot.mcp.application.service.RagTemplateService;
import com.penpot.mcp.core.ports.in.SearchTemplatesUseCase;
import com.penpot.mcp.model.MarketingTemplate;
import com.penpot.mcp.shared.exception.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import java.util.List;

/**
 * Implémentation du use case de recherche de templates.
 * Délègue au service RAG pour la recherche sémantique.
 * 
 * <h2>Responsabilités</h2>
 * <ul>
 *     <li>Validation des paramètres de recherche</li>
 *     <li>Délégation au service RAG</li>
 *     <li>Gestion cohérente des erreurs</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SearchTemplatesUseCaseImpl implements SearchTemplatesUseCase {

    private final RagTemplateService ragTemplateService;

    @Override
    public List<MarketingTemplate> searchByQuery(String query) {
        log.info("Searching templates with query: {}", query);

        if (query == null || query.isBlank()) {
            throw new ValidationException("Query cannot be null or empty");
        }

        try {
            List<MarketingTemplate> results = ragTemplateService.searchTemplates(query);

            log.info("Found {} templates for query: {}", results.size(), query);
            return results;
        } catch (Exception e) {
            log.error("Template search failed for query: {}", query, e);
            throw new ToolExecutionException(
                "Failed to search templates: " + e.getMessage(), 
                e
            );
        }
    }

    @Override
    public List<MarketingTemplate> searchByType(String type) {
        log.info("Searching templates by type: {}", type);

        if (type == null || type.isBlank()) {
            throw new ValidationException("Type cannot be null or empty");
        }

        try {
            List<MarketingTemplate> results = ragTemplateService.getTemplatesByType(type);

            log.info("Found {} templates for type: {}", results.size(), type);
            return results;
        } catch (Exception e) {
            log.error("Template search by type failed for: {}", type, e);
            throw new ToolExecutionException(
                "Failed to search templates by type: " + e.getMessage(), 
                e
            );
        }
    }

    @Override
    public List<MarketingTemplate> searchByTag(String tag) {
        log.info("Searching templates by tag: {}", tag);

        if (tag == null || tag.isBlank()) {
            throw new ValidationException("Tag cannot be null or empty");
        }

        try {
            List<MarketingTemplate> results = ragTemplateService.getTemplatesByTag(tag);

            log.info("Found {} templates for tag: {}", results.size(), tag);
            return results;
        } catch (Exception e) {
            log.error("Template search by tag failed for: {}", tag, e);
            throw new ToolExecutionException(
                "Failed to search templates by tag: " + e.getMessage(), 
                e
            );
        }
    }

    @Override
    public List<MarketingTemplate> getAllTemplates() {
        log.info("Retrieving all templates");

        try {
            List<MarketingTemplate> results = ragTemplateService.getAllTemplates();
            log.info("Retrieved {} templates in total", results.size());
            return results;
        } catch (Exception e) {
            log.error("Failed to retrieve all templates", e);
            throw new ToolExecutionException(
                "Failed to retrieve templates: " + e.getMessage(), 
                e
            );
        }
    }
}