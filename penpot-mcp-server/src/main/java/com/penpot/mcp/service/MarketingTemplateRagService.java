package com.penpot.mcp.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.penpot.mcp.model.MarketingTemplate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

/**
 * Service RAG pour la gestion des templates marketing.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MarketingTemplateRagService {
    
    private final VectorStore vectorStore;
    private final ObjectMapper objectMapper;
    private final AtomicBoolean isReady = new AtomicBoolean(false);
    
    @Value("${penpot.mcp.rag.similarity-threshold:0.6}")
    private double similarityThreshold;
    
    @Value("${penpot.mcp.rag.top-k:3}")
    private int defaultTopK;
    
    /**
     * Charge les templates marketing au démarrage.
     */
    @Async
    @EventListener(ApplicationReadyEvent.class)
    public void loadTemplates() {
        try {
            log.info("Chargement des templates marketing...");
            PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
            Resource[] resources = resolver.getResources("classpath:data/rag/templates/*.json");

            List<Document> documents = new ArrayList<>();

            for (Resource resource : resources) {
                try {
                    MarketingTemplate template = objectMapper.readValue(
                        resource.getInputStream(), 
                        MarketingTemplate.class
                    );

                    String content = buildTemplateContent(template);
                    Map<String, Object> metadata = buildTemplateMetadata(template);

                    Document doc = new Document(content, metadata);
                    documents.add(doc);

                    log.debug("Template chargé: {}", template.getId());
                } catch (Exception e) {
                    log.error("Erreur lors du chargement du template: {}", resource.getFilename(), e);
                }
            }

            if (!documents.isEmpty()) {
                try {
                    log.info("Indexation de {} templates dans le VectorStore...", documents.size());

                    // Traiter par lots de 10 pour éviter les timeouts
                    int batchSize = 10;
                    int totalBatches = (int) Math.ceil((double) documents.size() / batchSize);

                    for (int i = 0; i < documents.size(); i += batchSize) {
                        int end = Math.min(i + batchSize, documents.size());
                        List<Document> batch = documents.subList(i, end);
                        int currentBatch = (i / batchSize) + 1;

                        log.info("Indexation du lot {}/{} ({} templates)...", 
                            currentBatch, totalBatches, batch.size());

                        vectorStore.add(batch);

                        log.info("✅ Lot {}/{} indexé avec succès", currentBatch, totalBatches);

                        // Petite pause entre les lots pour éviter de surcharger Ollama
                        if (i + batchSize < documents.size()) {
                            Thread.sleep(500);
                        }
                    }

                    log.info("✅ {} templates marketing indexés avec succès au total", documents.size());
                    isReady.set(true);
                } catch (Exception e) {
                    log.error("❌ ERREUR lors de l'indexation des templates", e);
                    log.error("CAUSE POSSIBLE: Timeout de connexion à Ollama ou modèle d'embedding non disponible");
                    log.error("VÉRIFICATIONS:");
                    log.error("  1) Ollama est accessible: curl http://127.0.0.1:11434/api/tags");
                    log.error("  2) Le modèle existe: ollama list | grep mxbai-embed-large");
                    log.error("  3) Le tunnel SSH est actif (si utilisé)");
                    log.warn("⚠️  Le serveur continuera mais le RAG ne sera PAS disponible.");
                }
            } else {
                log.warn("Aucun template trouvé dans data/rag/templates/");
            }

        } catch (Exception e) {
            log.error("Erreur lors du chargement des templates", e);
        }
    }
    
    /**
     * Recherche des templates similaires.
     */
    public List<MarketingTemplate> searchTemplates(String query, int topK, double threshold) {
        if (!isReady.get()) {
            log.warn("⏳ Templates pas encore chargés");
            return Collections.emptyList();
        }

        SearchRequest request = SearchRequest.builder()
            .query(query)
            .topK(topK)
            .similarityThreshold(threshold)
            .build();
        
        List<Document> results = vectorStore.similaritySearch(request);
        
        return results.stream()
            .map(this::reconstructTemplate)
            .filter(Objects::nonNull)
            .collect(Collectors.toList());
    }
    
    /**
     * Trouve le meilleur template.
     */
    public Optional<MarketingTemplate> findBestTemplate(String description) {
        List<MarketingTemplate> templates = searchTemplates(description, 1, similarityThreshold);
        return templates.isEmpty() ? Optional.empty() : Optional.of(templates.get(0));
    }
    
    private String buildTemplateContent(MarketingTemplate template) {
        StringBuilder content = new StringBuilder();
        
        content.append("ID: ").append(template.getId()).append("\n");
        content.append("Type: ").append(template.getType()).append("\n");
        content.append("Tags: ").append(String.join(", ", template.getTags())).append("\n");
        content.append("Description: ").append(template.getDescription()).append("\n");
        
        if (template.getDesignRecipe() != null) {
            MarketingTemplate.DesignRecipe recipe = template.getDesignRecipe();
            content.append("Canvas: ").append(recipe.getCanvasSize()).append("\n");
            content.append("Background: ").append(recipe.getBackgroundMode()).append("\n");
            
            if (recipe.getMainElement() != null) {
                content.append("Element: ")
                    .append(recipe.getMainElement().getType())
                    .append("\n");
            }
        }
        
        return content.toString();
    }
    
    private Map<String, Object> buildTemplateMetadata(MarketingTemplate template) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("template_id", template.getId());
        metadata.put("template_type", template.getType());
        metadata.put("tags", String.join(",", template.getTags()));
        
        try {
            metadata.put("full_template", objectMapper.writeValueAsString(template));
        } catch (Exception e) {
            log.error("Erreur sérialisation template", e);
        }
        
        return metadata;
    }
    
    private MarketingTemplate reconstructTemplate(Document doc) {
        try {
            String fullTemplate = (String) doc.getMetadata().get("full_template");
            if (fullTemplate != null) {
                return objectMapper.readValue(fullTemplate, MarketingTemplate.class);
            }
        } catch (Exception e) {
            log.error("Erreur reconstruction template", e);
        }
        return null;
    }
}