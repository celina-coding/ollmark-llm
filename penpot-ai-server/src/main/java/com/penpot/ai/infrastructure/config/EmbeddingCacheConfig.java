package com.penpot.ai.infrastructure.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.*;

/**
 * Configuration du cache pour les embeddings de requêtes.
 */
@Slf4j
@Configuration
@EnableCaching
public class EmbeddingCacheConfig {

    /**
     * Nom du cache pour les embeddings de requêtes.
     */
    public static final String QUERY_EMBEDDINGS_CACHE = "query-embeddings";

    /**
     * Nom du cache pour les embeddings de documents.
     */
    public static final String DOCUMENT_EMBEDDINGS_CACHE = "document-embeddings";

    /**
     * Configure le CacheManager avec Caffeine.
     * 
     * @return le cache manager configuré
     */
    @Bean
    public CacheManager cacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager(
            QUERY_EMBEDDINGS_CACHE,
            DOCUMENT_EMBEDDINGS_CACHE
        );

        cacheManager.setCaffeine(caffeineCacheBuilder());

        log.info("Embedding Cache Manager configured with Caffeine");
        log.info("Cache names: {}", cacheManager.getCacheNames());

        return cacheManager;
    }

    /**
     * Configure le builder Caffeine pour le cache d'embeddings.
     */
    private Caffeine<Object, Object> caffeineCacheBuilder() {
        return Caffeine.newBuilder()
            .maximumSize(10_000)
            .recordStats()
            .weakKeys()
            .evictionListener((key, value, cause) -> {
                log.debug("Cache eviction: key={}, cause={}", key, cause);
            });
    }
}