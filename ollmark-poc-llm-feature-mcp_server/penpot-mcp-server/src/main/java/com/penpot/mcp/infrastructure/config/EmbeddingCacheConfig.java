package com.penpot.mcp.infrastructure.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.*;
import java.util.concurrent.TimeUnit;

/**
 * Configuration du cache pour les embeddings de requêtes.
 * 
 * Principe ABSOLU :
 * - Même requête = même embedding (jamais recalculé)
 * - Cache permanent en mémoire (pas d'expiration)
 * - Hash de la query → embedding
 * 
 * Optimisation critique :
 * - Les embeddings sont coûteux à calculer (appel réseau Ollama)
 * - Une query typique revient souvent (ex: "social media post")
 * - Cache = gain de 100x en performance
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
     * Caffeine est utilisé pour :
     * - Performance : cache en mémoire ultra-rapide
     * - Statistiques : monitoring des hits/miss
     * - Pas d'expiration : cache permanent (ABSOLU)
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
     * 
     * Configuration ABSOLUE :
     * - Pas d'expiration (pas de expireAfterWrite)
     * - Taille maximale : 10,000 entrées (ajustable)
     * - Statistiques activées pour monitoring
     * - Weak keys pour GC si besoin
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

    /**
     * Bean pour exposer les statistiques du cache.
     * Utile pour monitoring et debugging.
     * 
     * @param cacheManager le cache manager
     * @return le bean de statistiques
     */
    @Bean
    public EmbeddingCacheStats embeddingCacheStats(CacheManager cacheManager) {
        return new EmbeddingCacheStats(cacheManager);
    }

    /**
     * Classe utilitaire pour obtenir les statistiques du cache.
     */
    public static class EmbeddingCacheStats {
        private final CacheManager cacheManager;

        public EmbeddingCacheStats(CacheManager cacheManager) {
            this.cacheManager = cacheManager;
        }

        /**
         * Obtient les statistiques du cache des embeddings de requêtes.
         * 
         * @return stats du cache ou null si pas disponible
         */
        public com.github.benmanes.caffeine.cache.stats.CacheStats getQueryEmbeddingsStats() {
            var cache = cacheManager.getCache(QUERY_EMBEDDINGS_CACHE);
            if (cache instanceof org.springframework.cache.caffeine.CaffeineCache) {
                org.springframework.cache.caffeine.CaffeineCache caffeineCache = 
                    (org.springframework.cache.caffeine.CaffeineCache) cache;
                return caffeineCache.getNativeCache().stats();
            }
            return null;
        }

        /**
         * Log les statistiques du cache.
         */
        public void logStats() {
            var stats = getQueryEmbeddingsStats();
            if (stats != null) {
                log.info("Query Embeddings Cache Stats:");
                log.info("  - Hit rate: {}", stats.hitRate());
                log.info("  - Hit count: {}", stats.hitCount());
                log.info("  - Miss count: {}", stats.missCount());
                log.info("  - Load count: {}", stats.loadCount());
                log.info("  - Eviction count: {}", stats.evictionCount());
            }
        }
    }
}