package com.example.aiPoc.services;

import com.fasterxml.jackson.databind.*;
import org.slf4j.*;
import org.springframework.cache.annotation.*;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Service utilitaire chargé de charger et désérialiser des fichiers JSON depuis le classpath
 * ou un chemin de ressource interne au projet Spring Boot.
 *
 * <p>
 * Ce service centralise les opérations de lecture JSON utilisées dans l’application,
 * notamment pour charger des fichiers de configuration ou de documentation (ex. : résumés d’API).
 * </p>
 */
@Service
@CacheConfig(cacheNames = "jsonFiles")
public class JsonLoaderService {

    /** Logger utilisé pour le suivi et le débogage du service. */
    private static final Logger logger = LoggerFactory.getLogger(JsonLoaderService.class);

    private final ObjectMapper objectMapper;

    private final ConcurrentHashMap<String, Object> memoryCache = new ConcurrentHashMap<>();

    /**
     * Initialise un {@link ObjectMapper} configuré pour accepter les structures JSON incomplètes
     * ou comportant des propriétés inconnues, afin de rendre la désérialisation plus tolérante.
     *
     * <p>Les options activées sont :
     * <ul>
     *   <li>{@link DeserializationFeature#FAIL_ON_UNKNOWN_PROPERTIES} désactivé</li>
     *   <li>{@link DeserializationFeature#ACCEPT_EMPTY_ARRAY_AS_NULL_OBJECT} activé</li>
     * </ul>
     * </p>
     */
    public JsonLoaderService() {
        this.objectMapper = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
            .configure(DeserializationFeature.ACCEPT_EMPTY_ARRAY_AS_NULL_OBJECT, true)
            .configure(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY, true);
    }

    /**
     * Charge un fichier JSON situé dans le classpath de l’application et le convertit
     * en une instance de la classe spécifiée.
     *
     * @param path  chemin relatif vers le fichier JSON dans le classpath
     * @param clazz classe du type de l’objet attendu après désérialisation.
     * @param <T>   type de retour générique, correspondant à la classe cible.
     * @return instance de l’objet désérialisé, ou {@code null} si une erreur survient
     *         (fichier introuvable, I/O ou JSON invalide).
     *
     * @throws IllegalArgumentException si le chemin est nul ou vide.
     */
    @Cacheable(key = "#path + '_' + #clazz.name")
    public <T> T loadJson(String path, Class<T> clazz) {
        if (path == null || path.trim().isEmpty()) {
            logger.error("Chemin du fichier JSON null ou vide");
            throw new IllegalArgumentException("Le chemin du fichier JSON ne peut pas être vide");
        }

        // Vérification du cache mémoire
        String cacheKey = path + "_" + clazz.getName();
        @SuppressWarnings("unchecked")
        T cached = (T) memoryCache.get(cacheKey);
        if (cached != null) {
            logger.debug("Fichier JSON chargé depuis le cache mémoire: {}", path);
            return cached;
        }

        String cleanPath = path.startsWith("classpath:") 
            ? path.substring("classpath:".length()) 
            : path;

        logger.debug("Chargement du fichier JSON: {}", cleanPath);

        try {
            ClassPathResource resource = new ClassPathResource(cleanPath);
            
            if (!resource.exists()) {
                logger.error("Fichier JSON introuvable: {}", cleanPath);
                throw new FileNotFoundException("Fichier JSON introuvable: " + cleanPath);
            }

            try (InputStream inputStream = resource.getInputStream()) {
                T result = objectMapper.readValue(inputStream, clazz);
                
                // Mise en cache mémoire
                memoryCache.put(cacheKey, result);
                
                logger.info("Fichier JSON chargé avec succès: {} ({} octets)", 
                    cleanPath, resource.contentLength());
                return result;
            }
            
        } catch (FileNotFoundException e) {
            logger.error("Fichier JSON introuvable: {}", cleanPath);
            throw new RuntimeException("Fichier JSON introuvable: " + cleanPath, e);
            
        } catch (IOException e) {
            logger.error("Erreur de lecture du fichier JSON: {} - {}", cleanPath, e.getMessage());
            throw new RuntimeException("Erreur de lecture du fichier JSON: " + cleanPath, e);
        }
    }

    /**
     * Vide le cache pour un chemin spécifique.
     * 
     * @param path chemin du fichier à retirer du cache
     */
    @CacheEvict(key = "#path + '_*'")
    public void evictCache(String path) {
        memoryCache.entrySet().removeIf(entry -> entry.getKey().startsWith(path + "_"));
        logger.info("Cache vidé pour: {}", path);
    }

    /**
     * Vide complètement le cache.
     */
    @CacheEvict(allEntries = true)
    public void clearCache() {
        memoryCache.clear();
        logger.info("Cache JSON complètement vidé");
    }

    /**
     * Retourne la taille actuelle du cache mémoire.
     */
    public int getCacheSize() {
        return memoryCache.size();
    }
}