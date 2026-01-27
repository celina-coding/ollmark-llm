package com.penpot.mcp.application.usecases;

import com.penpot.mcp.application.service.RagTemplateService;
import com.penpot.mcp.core.ports.in.SearchTemplatesUseCase;
import com.penpot.mcp.model.MarketingTemplate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import java.util.List;

/**
 * Implémentation du use case de recherche de templates.
 * Délègue au service RAG pour la recherche sémantique.
 * Suit le Single Responsibility Principle.
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
            throw new IllegalArgumentException("Query cannot be null or empty");
        }
        
        try {
            List<MarketingTemplate> results = ragTemplateService.searchTemplates(query);
            
            log.info("Found {} templates for query: {}", results.size(), query);
            
            return results;
            
        } catch (Exception e) {
            log.error("Template search failed", e);
            throw new RuntimeException("Failed to search templates: " + e.getMessage(), e);
        }
    }
    
    @Override
    public List<MarketingTemplate> searchByType(String type) {
        log.info("Searching templates by type: {}", type);
        
        if (type == null || type.isBlank()) {
            throw new IllegalArgumentException("Category/Type cannot be null or empty");
        }
        
        return ragTemplateService.getTemplatesByType(type);
    }
    
    @Override
    public List<MarketingTemplate> searchByTag(String tag) {
        log.info("Searching templates by tag: {}", tag);
        
        if (tag == null || tag.isBlank()) {
            throw new IllegalArgumentException("Tag cannot be null or empty");
        }
        
        return ragTemplateService.getTemplatesByTag(tag);
    }
    
    @Override
    public List<MarketingTemplate> getAllTemplates() {
        log.info("Retrieving all templates");
        return ragTemplateService.getAllTemplates();
    }
}