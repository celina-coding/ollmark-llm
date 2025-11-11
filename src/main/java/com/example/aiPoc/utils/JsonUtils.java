package com.example.aiPoc.utils;

import java.io.IOException;
import java.io.InputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

/**
 * Classe utilitaire pour la manipulation et la transformation de données JSON.
 *
 * <p>
 * Elle repose sur la bibliothèque {@link ObjectMapper} de Jackson et fournit
 * des méthodes pratiques pour :
 * <ul>
 *   <li>Convertir des objets Java en JSON et inversement</li>
 *   <li>Lire des fichiers JSON depuis le classpath</li>
 *   <li>Valider, formater et minifier des chaînes JSON</li>
 *   <li>Extraire des valeurs simples depuis un document JSON</li>
 * </ul>
 * </p>
 *
 * Toutes les erreurs de traitement JSON sont journalisées via SLF4J.
 */
public class JsonUtils {

    /** Logger pour le suivi et la journalisation des erreurs JSON. */
    private static final Logger logger = LoggerFactory.getLogger(JsonUtils.class);

    /** Instance partagée de {@link ObjectMapper} configurée pour l’indentation. */
    private static final ObjectMapper objectMapper = new ObjectMapper()
            .enable(SerializationFeature.INDENT_OUTPUT);

    /**
     * Sérialise un objet Java en chaîne JSON formatée.
     *
     * @param object l’objet à convertir en JSON (peut être {@code null}).
     * @return la représentation JSON de l’objet, ou une chaîne vide `{}` en cas d’erreur.
     */
    public static String toJson(Object object) {
        try {
            return objectMapper.writeValueAsString(object);
        } catch (JsonProcessingException e) {
            logger.error("Erreur lors de la conversion en JSON", e);
            return "{}";
        }
    }

    /**
     * Désérialise une chaîne JSON en une instance d’un type donné.
     *
     * @param <T>   le type de l’objet à créer.
     * @param json  la chaîne JSON à désérialiser.
     * @param clazz la classe cible du type de retour.
     * @return une instance de type {@code T} représentant le JSON,
     *         ou {@code null} si une erreur se produit.
     */
    public static <T> T fromJson(String json, Class<T> clazz) {
        try {
            return objectMapper.readValue(json, clazz);
        } catch (JsonProcessingException e) {
            logger.error("Erreur lors de la lecture du JSON", e);
            return null;
        }
    }

    /**
     * Lit et désérialise un fichier JSON situé dans le classpath.
     *
     * <p>Cette méthode recherche le fichier dans les ressources du projet et tente
     * de le convertir en une instance du type spécifié.</p>
     *
     * @param <T>   le type de l’objet cible.
     * @param path  le chemin du fichier JSON relatif au classpath (ex: {@code "data/config.json"}).
     * @param clazz la classe cible pour la désérialisation.
     * @return une instance de {@code T} représentant le contenu du fichier,
     *         ou {@code null} si le fichier est introuvable ou invalide.
     */
    public static <T> T readFromClasspath(String path, Class<T> clazz) {
        try (InputStream is = JsonUtils.class.getClassLoader().getResourceAsStream(path)) {
            if (is == null) {
                logger.warn("Fichier non trouvé: {}", path);
                return null;
            }
            return objectMapper.readValue(is, clazz);
        } catch (IOException e) {
            logger.error("Erreur lors de la lecture du fichier: {}", path, e);
            return null;
        }
    }

    /**
     * Vérifie si une chaîne de caractères correspond à un JSON valide.
     *
     * @param json la chaîne à valider.
     * @return {@code true} si la chaîne est un JSON valide, {@code false} sinon.
     */
    public static boolean isValidJson(String json) {
        if (json == null || json.trim().isEmpty()) return false;

        try {
            objectMapper.readTree(json);
            return true;
        } catch (JsonProcessingException e) {
            return false;
        }
    }

    /**
     * Formate joliment une chaîne JSON pour la rendre plus lisible (indentation, sauts de ligne).
     *
     * @param json la chaîne JSON brute à formater.
     * @return une version indentée et lisible du JSON, ou le texte original en cas d’erreur.
     */
    public static String prettyPrint(String json) {
        try {
            Object obj = objectMapper.readValue(json, Object.class);
            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            logger.error("Erreur lors du formatage JSON", e);
            return json;
        }
    }

    /**
     * Minifie une chaîne JSON en supprimant les espaces, sauts de ligne et indentations inutiles.
     *
     * @param json la chaîne JSON à minifier.
     * @return une version compacte du JSON, ou le texte original si une erreur se produit.
     */
    public static String minify(String json) {
        try {
            Object obj = objectMapper.readValue(json, Object.class);
            return new ObjectMapper().writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            logger.error("Erreur lors de la minification JSON", e);
            return json;
        }
    }

    /**
     * Extrait une valeur simple depuis un document JSON à partir d’une clé donnée.
     *
     * <p>Cette méthode ne supporte pas les chemins JSON complexes (comme {@code user.name});
     * elle accède uniquement aux clés de premier niveau.</p>
     *
     * @param json la chaîne JSON source.
     * @param key  la clé de l’attribut à extraire.
     * @return la valeur associée à la clé, ou {@code null} si la clé est absente ou invalide.
     */
    public static String extractValue(String json, String key) {
        try {
            var node = objectMapper.readTree(json);
            var valueNode = node.get(key);
            return valueNode != null ? valueNode.asText() : null;
        } catch (JsonProcessingException e) {
            logger.error("Erreur lors de l'extraction de la clé: {}", key, e);
            return null;
        }
    }
}