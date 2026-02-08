package com.penpot.ai.application.service;

import com.penpot.ai.model.MarketingTemplate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Service;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service RAG (Retrieval-Augmented Generation) pour les templates marketing.
 * 
 * Architecture RAG avec Cache:
 * 1. Chargement : Lit les templates JSON depuis resources/data/rag/templates/
 * 2. Vectorisation : Convertit chaque template en embedding via Ollama (avec cache)
 * 3. Stockage : Enregistre les embeddings dans VectorStore (en mémoire)
 * 4. Recherche : Trouve les templates similaires à une requête utilisateur (avec cache)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RagTemplateService {

    private final VectorStore vectorStore;
    private final ObjectMapper objectMapper;
    private final EmbeddingCacheService embeddingCache;

    @Value("${penpot.ai.rag.templates-path}")
    private String templatesPath;

    @Value("${penpot.ai.rag.similarity-threshold:0.6}")
    private double similarityThreshold;

    @Value("${penpot.ai.rag.top-k:3}")
    private int topK;

    /** Cache des templates chargés (pour consultation directe) */
    private final Map<String, MarketingTemplate> templatesCache = new HashMap<>();

    /**
     * Initialise le service en chargeant et vectorisant tous les templates.
     * Appelé automatiquement après la construction du bean.
     */
    @PostConstruct
    public void init() {
        try {
            loadAndIndexTemplates();
            log.info("RAG Template Service initialized with {} templates", 
                templatesCache.size());
        } catch (Exception e) {
            log.error("Failed to initialize RAG Template Service", e);
        }
    }

    /**
     * Charge tous les templates JSON et les indexe dans le VectorStore.
     */
    private void loadAndIndexTemplates() throws IOException {
        PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
        Resource[] resources = resolver.getResources(templatesPath);

        log.info("Loading {} template files from {}", resources.length, templatesPath);
        List<Document> documents = new ArrayList<>();

        for (Resource resource : resources) {
            try {
                MarketingTemplate template = objectMapper.readValue(
                    resource.getInputStream(), 
                    MarketingTemplate.class
                );

                templatesCache.put(template.getId(), template);
                Document doc = createDocument(template);
                documents.add(doc);

                log.debug("Loaded template: {} (type: {})", 
                    template.getId(), template.getType());
            } catch (Exception e) {
                log.error("Failed to load template from {}", resource.getFilename(), e);
            }
        }

        if (!documents.isEmpty()) {
            vectorStore.add(documents);
            log.info("Indexed {} templates in VectorStore", documents.size());
        }
    }

    /**
     * Crée un Document pour le VectorStore à partir d'un template.
     * 
     * Note: Le VectorStore gère les embeddings en interne lors de l'ajout des documents.
     * Le cache des embeddings est utilisé au niveau des requêtes de recherche.
     */
    private Document createDocument(MarketingTemplate template) {
        StringBuilder content = new StringBuilder();
        content.append("Template ID: ").append(template.getId()).append("\n");
        content.append("Type: ").append(template.getType()).append("\n");
        content.append("Description: ").append(template.getDescription()).append("\n");

        if (template.getTags() != null && !template.getTags().isEmpty()) {
            content.append("Tags: ").append(String.join(", ", template.getTags())).append("\n");
        }

        if (template.getDesignRecipe() != null && !template.getDesignRecipe().isEmpty()) {
            content.append("Design Recipe: ");
            template.getDesignRecipe().forEach((key, value) -> {
                content.append(key).append("=").append(value).append(" ");
            });
            content.append("\n");
        }

        String contentText = content.toString();
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("id", template.getId());
        metadata.put("type", template.getType());
        metadata.put("tags", template.getTags());

        return new Document(contentText, metadata);
    }

    /**
     * Recherche les templates les plus similaires à une requête utilisateur.
     *
     * @param query la requête utilisateur (ex: "social media post for product launch")
     * @return liste des templates correspondants, triés par pertinence
     */
    public List<MarketingTemplate> searchTemplates(String query) {
        log.info("Searching templates for query: {}", query);

        long startTime = System.currentTimeMillis();

        float[] queryEmbedding = embeddingCache.embedQuery(query);

        long embeddingTime = System.currentTimeMillis() - startTime;
        log.debug("Query embedding completed in {}ms (cache used)", embeddingTime);

        SearchRequest searchRequest = SearchRequest.builder()
            .query(query)
            .similarityThreshold(similarityThreshold)
            .topK(topK)
            .build();

        List<Document> results = vectorStore.similaritySearch(searchRequest);

        long totalTime = System.currentTimeMillis() - startTime;
        log.info("Found {} matching templates in {}ms (embedding: {}ms, search: {}ms)", 
            results.size(), 
            totalTime,
            embeddingTime,
            totalTime - embeddingTime);

        return results.stream()
            .map(doc -> {
                String templateId = (String) doc.getMetadata().get("id");
                return templatesCache.get(templateId);
            })
            .filter(Objects::nonNull)
            .collect(Collectors.toList());
    }

    /**
     * Recherche des templates par type.
     *
     * @param type le type recherché (ex: "social_media_post", "email")
     * @return liste des templates de ce type
     */
    public List<MarketingTemplate> getTemplatesByType(String type) {
        return templatesCache.values().stream()
            .filter(t -> type.equalsIgnoreCase(t.getType()))
            .collect(Collectors.toList());
    }

    /**
     * Recherche des templates par tag.
     *
     * @param tag le tag recherché
     * @return liste des templates ayant ce tag
     */
    public List<MarketingTemplate> getTemplatesByTag(String tag) {
        return templatesCache.values().stream()
            .filter(t -> t.getTags() != null && t.getTags().contains(tag))
            .collect(Collectors.toList());
    }

    /**
     * Récupère un template par son ID.
     *
     * @param templateId l'ID du template
     * @return le template ou Optional.empty()
     */
    public Optional<MarketingTemplate> getTemplateById(String templateId) {
        return Optional.ofNullable(templatesCache.get(templateId));
    }

    /**
     * Retourne tous les templates disponibles.
     *
     * @return liste de tous les templates
     */
    public List<MarketingTemplate> getAllTemplates() {
        return new ArrayList<>(templatesCache.values());
    }

    /**
     * Retourne le nombre de templates chargés.
     *
     * @return le nombre de templates
     */
    public int getTemplateCount() {
        return templatesCache.size();
    }

    /**
     * Retourne tous les types disponibles.
     *
     * @return ensemble des types
     */
    public Set<String> getAvailableTypes() {
        return templatesCache.values().stream()
            .map(MarketingTemplate::getType)
            .collect(Collectors.toSet());
    }

    /**
     * Retourne tous les tags disponibles.
     *
     * @return ensemble des tags
     */
    public Set<String> getAvailableTags() {
        return templatesCache.values().stream()
            .filter(t -> t.getTags() != null)
            .flatMap(t -> t.getTags().stream())
            .collect(Collectors.toSet());
    }
}