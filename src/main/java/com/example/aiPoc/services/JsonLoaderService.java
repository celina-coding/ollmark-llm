package com.example.aiPoc.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

/**
 * Service utilitaire pour charger et désérialiser des fichiers JSON
 * depuis le classpath ou le système de fichiers.
 */
@Service
public class JsonLoaderService {

    private static final Logger logger = LoggerFactory.getLogger(JsonLoaderService.class);
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Charge un fichier JSON du classpath et le désérialise en un objet du type spécifié.
     *
     * @param path  chemin relatif dans le classpath (ex : "templates/penpot-api-summary.json")
     * @param clazz type cible pour la désérialisation
     * @param <T>   type générique de l’objet attendu
     * @return instance de l’objet désérialisé, ou {@code null} si erreur
     */
    public <T> T loadJson(String path, Class<T> clazz) {
        try {
            ClassPathResource resource = new ClassPathResource(path);
            if (!resource.exists()) {
                logger.warn("Fichier JSON introuvable dans le classpath: {}", path);
                return null;
            }

            try (InputStream inputStream = resource.getInputStream()) {
                T result = objectMapper.readValue(inputStream, clazz);
                logger.debug("Fichier JSON chargé avec succès : {}", path);
                return result;
            }

        } catch (IOException e) {
            logger.error("Erreur lors du chargement du fichier JSON : {}", path, e);
            return null;
        }
    }

    /**
     * Charge un fichier JSON depuis le système de fichiers.
     *
     * @param file  fichier à charger
     * @param clazz classe cible pour la désérialisation
     * @return instance de l’objet désérialisé, ou {@code null} si erreur
     */
    public <T> T loadJsonFromFile(File file, Class<T> clazz) {
        if (file == null || !file.exists()) {
            logger.warn("Fichier JSON introuvable : {}", file);
            return null;
        }
        try {
            return objectMapper.readValue(file, clazz);
        } catch (IOException e) {
            logger.error("Erreur de lecture du fichier JSON : {}", file, e);
            return null;
        }
    }

    /**
     * Vérifie si un fichier existe dans le classpath.
     *
     * @param path chemin relatif du fichier
     * @return true si le fichier est présent
     */
    public boolean exists(String path) {
        return new ClassPathResource(path).exists();
    }
}