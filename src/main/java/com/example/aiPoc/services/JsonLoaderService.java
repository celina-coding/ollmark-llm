package com.example.aiPoc.services;

import com.fasterxml.jackson.databind.*;
import org.slf4j.*;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.*;

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
public class JsonLoaderService {

    /** Logger utilisé pour le suivi et le débogage du service. */
    private static final Logger logger = LoggerFactory.getLogger(JsonLoaderService.class);

    private final ObjectMapper objectMapper;

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
            .configure(DeserializationFeature.ACCEPT_EMPTY_ARRAY_AS_NULL_OBJECT, true);
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
    public <T> T loadJson(String path, Class<T> clazz) {
        if (path == null || path.trim().isEmpty()) {
            logger.error("Chemin du fichier JSON null ou vide");
            return null;
        }

        String cleanPath = path.startsWith("classpath:") 
            ? path.substring("classpath:".length()) 
            : path;

        logger.debug("Tentative de chargement du fichier JSON: {}", cleanPath);

        try {
            ClassPathResource resource = new ClassPathResource(cleanPath);
            if (!resource.exists()) {
                logger.warn("Fichier JSON introuvable dans le classpath: {} (chemin complet: {})", 
                           cleanPath, resource.getPath());
                return null;
            }

            try (InputStream inputStream = resource.getInputStream()) {
                T result = objectMapper.readValue(inputStream, clazz);
                logger.info("Fichier JSON chargé avec succès : {}", path);
                return result;
            }
        } catch (IOException e) {
            logger.error("Erreur lors du chargement du fichier JSON: {} - {}", 
                        cleanPath, e.getMessage(), e);
            return null;
        }
    }
}