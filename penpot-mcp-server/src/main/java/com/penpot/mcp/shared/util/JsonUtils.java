package com.penpot.mcp.shared.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

/**
 * Utilitaires pour la manipulation JSON.
 * Classe utilitaire statique pour les opérations JSON courantes.
 */
@Slf4j
@UtilityClass
public class JsonUtils {

    /**
     * ObjectMapper thread-safe partagé.
     * Réutilisé pour toutes les opérations JSON.
     */
    private static final ObjectMapper MAPPER = new ObjectMapper();

    /**
     * Sérialise un objet en JSON.
     * 
     * @param object l'objet à sérialiser
     * @return la chaîne JSON ou null en cas d'erreur
     */
    public static String toJson(Object object) {
        if (object == null) return "null";

        try {
            return MAPPER.writeValueAsString(object);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize object to JSON", e);
            return null;
        }
    }

    /**
     * Sérialise un objet en JSON avec formatage pretty.
     * 
     * @param object l'objet à sérialiser
     * @return la chaîne JSON formatée ou null en cas d'erreur
     */
    public static String toPrettyJson(Object object) {
        if (object == null) return "null";

        try {
            return MAPPER.writerWithDefaultPrettyPrinter()
                .writeValueAsString(object);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize object to pretty JSON", e);
            return null;
        }
    }

    /**
     * Désérialise une chaîne JSON en objet.
     * 
     * @param json la chaîne JSON
     * @param clazz la classe cible
     * @param <T> le type de retour
     * @return l'objet désérialisé ou null en cas d'erreur
     */
    public static <T> T fromJson(String json, Class<T> clazz) {
        if (json == null || json.isBlank()) return null;

        try {
            return MAPPER.readValue(json, clazz);
        } catch (JsonProcessingException e) {
            log.error("Failed to deserialize JSON to {}", clazz.getSimpleName(), e);
            return null;
        }
    }

    /**
     * Échappe une chaîne pour inclusion dans un JSON.
     * Gère les caractères spéciaux : \, ", newline, carriage return, tab.
     * 
     * @param str la chaîne à échapper
     * @return la chaîne échappée entre guillemets, ou "null" si str est null
     */
    public static String escapeJson(String str) {
        if (str == null) return "null";

        return "\"" + str
            .replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
            .replace("\t", "\\t")
            + "\"";
    }

    /**
     * Vérifie si une chaîne est un JSON valide.
     * 
     * @param json la chaîne à vérifier
     * @return true si JSON valide
     */
    public static boolean isValidJson(String json) {
        if (json == null || json.isBlank()) return false;

        try {
            MAPPER.readTree(json);
            return true;
        } catch (JsonProcessingException e) {
            return false;
        }
    }

    /**
     * Tronque une chaîne JSON pour les logs.
     * 
     * @param json la chaîne JSON
     * @param maxLength longueur maximale
     * @return la chaîne tronquée si nécessaire
     */
    public static String truncateForLog(String json, int maxLength) {
        if (json == null) return "null";
        if (json.length() <= maxLength) return json;
        return json.substring(0, maxLength) + "... (truncated)";
    }
}