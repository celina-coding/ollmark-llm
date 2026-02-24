package com.penpot.ai.shared.util;

import com.penpot.ai.shared.exception.ValidationException;
import lombok.experimental.UtilityClass;

/**
 * Utilitaires de validation centralisés.
 * Évite la duplication de code de validation dans les use cases.
 */
@UtilityClass
public class ValidationUtils {

    /**
     * Valide qu'une chaîne n'est pas null ou vide.
     * 
     * @param value la valeur à valider
     * @param fieldName le nom du champ (pour le message d'erreur)
     * @throws ValidationException si la validation échoue
     */
    public static void requireNonBlank(String value, String fieldName) {
        if (value == null || value.isBlank()) throw new ValidationException(fieldName + " cannot be null or empty");
    }

    /**
     * Valide une chaîne avec les contraintes standard.
     * 
     * @param value la valeur
     * @param fieldName le nom du champ
     * @param maxLength longueur max (0 = pas de limite)
     */
    public static void validateString(String value, String fieldName, int maxLength) {
        requireNonBlank(value, fieldName);
        if (maxLength > 0) requireMaxLength(value, maxLength, fieldName);
    }

    /**
     * Valide qu'une chaîne respecte une longueur maximale.
     * 
     * @param value la valeur à valider
     * @param maxLength longueur maximale
     * @param fieldName le nom du champ
     * @throws ValidationException si la validation échoue
     */
    private static void requireMaxLength(String value, int maxLength, String fieldName) {
        if (value != null && value.length() > maxLength) {
            throw new ValidationException(
                String.format("%s too long: %d characters (max %d)", 
                    fieldName, value.length(), maxLength)
            );
        }
    }
}