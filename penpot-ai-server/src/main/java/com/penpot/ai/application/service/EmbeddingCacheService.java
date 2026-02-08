package com.penpot.ai.application.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.embedding.*;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import static com.penpot.ai.infrastructure.config.EmbeddingCacheConfig.QUERY_EMBEDDINGS_CACHE;

/**
 * Service d'embedding avec cache ABSOLU.
 * 
 * Principe :
 * - Même requête = même embedding (jamais recalculé)
 * - Hash de la query → embedding
 * - Cache permanent (pas d'expiration)
 * 
 * Gain de performance :
 * - Sans cache : ~100-200ms par requête (appel réseau Ollama)
 * - Avec cache : <1ms (lookup en mémoire)
 * - Gain : 100x-200x
 * 
 * Exemples :
 * - "social media post" appelé 10 fois → 1 seul calcul
 * - "instagram post" appelé 5 fois → 1 seul calcul
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EmbeddingCacheService {

    private final EmbeddingModel embeddingModel;

    /**
     * Génère l'embedding d'une requête avec cache ABSOLU.
     * 
     * Le cache utilise la query comme clé :
     * - Clé : hash de la query string
     * - Valeur : tableau de floats (embedding vector)
     * 
     * Spring Cache gère automatiquement :
     * - Le hashing de la clé
     * - Le stockage en mémoire
     * - La récupération depuis le cache
     * 
     * @param query la requête à embedder
     * @return le vecteur d'embedding (typiquement 1024 dimensions pour mxbai-embed-large)
     */
    @Cacheable(value = QUERY_EMBEDDINGS_CACHE, key = "#query")
    public float[] embedQuery(String query) {
        log.debug("Computing embedding for query: {} (CACHE MISS)", truncate(query, 50));
        long startTime = System.currentTimeMillis();

        EmbeddingResponse response = embeddingModel.embedForResponse(java.util.List.of(query));
        float[] embedding = response.getResult().getOutput();

        long duration = System.currentTimeMillis() - startTime;

        log.info("Embedding computed in {}ms (dimensions: {}) - CACHED for query: {}", 
            duration, 
            embedding.length, 
            truncate(query, 50));

        return embedding;
    }

    /**
     * Génère l'embedding d'un document avec cache.
     * Utilisé pour les templates lors de l'indexation.
     * 
     * @param content le contenu du document
     * @return le vecteur d'embedding
     */
    @Cacheable(value = QUERY_EMBEDDINGS_CACHE, key = "#content.hashCode()")
    public float[] embedDocument(String content) {
        log.debug("Computing embedding for document (length: {} chars)", content.length());
        EmbeddingResponse response = embeddingModel.embedForResponse(java.util.List.of(content));
        float[] embedding = response.getResult().getOutput();
        log.debug("Document embedding computed (dimensions: {})", embedding.length);
        return embedding;
    }

    /**
     * Tronque une string pour les logs.
     */
    private String truncate(String str, int maxLength) {
        if (str == null) return "null";
        if (str.length() <= maxLength) return str;
        return str.substring(0, maxLength) + "...";
    }
}