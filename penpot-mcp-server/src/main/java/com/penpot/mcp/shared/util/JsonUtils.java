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

    private static final ObjectMapper MAPPER = new ObjectMapper();

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