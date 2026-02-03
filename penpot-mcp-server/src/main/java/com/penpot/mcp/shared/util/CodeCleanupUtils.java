package com.penpot.mcp.shared.util;

import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import java.util.regex.*;

/**
 * Utilitaires pour nettoyer et corriger le code JavaScript généré par l'IA.
 * 
 * Résout les problèmes courants :
 * - Format numérique français (virgules) → format JavaScript (points)
 * - Markdown artifacts (```javascript```)
 * - Commentaires superflus
 * - Espaces inutiles
 */
@Slf4j
@UtilityClass
public class CodeCleanupUtils {

    /**
     * Pattern pour détecter les nombres avec virgules comme séparateurs décimaux.
     * Exemples : 0,0 | 100,5 | 0,000000 | 123,456789
     * 
     * Regex expliquée :
     * \b      - Word boundary (début du nombre)
     * \d+     - Un ou plusieurs chiffres avant la virgule
     * ,       - Virgule littérale
     * \d+     - Un ou plusieurs chiffres après la virgule
     * \b      - Word boundary (fin du nombre)
     */
    private static final Pattern FRENCH_NUMBER_PATTERN = Pattern.compile("\\b(\\d+),(\\d+)\\b");

    /**
     * Nettoie et corrige le code JavaScript généré par l'IA.
     * 
     * Applique les corrections suivantes dans l'ordre :
     * 1. Supprime les blocs markdown (```javascript```, ```)
     * 2. Convertit les nombres français (,) en format JavaScript (.)
     * 3. Supprime les commentaires de ligne (//)
     * 4. Supprime les commentaires de bloc (/* *‍/)
     * 5. Normalise les espaces (supprime lignes vides multiples)
     * 6. Trim final
     * 
     * @param code code JavaScript brut généré par l'IA
     * @return code nettoyé et prêt à l'exécution
     */
    public static String cleanGeneratedCode(String code) {
        if (code == null || code.isBlank()) return "";
        String cleaned = code;

        cleaned = cleaned.replaceAll("^```(?:javascript|js)?\\s*", "");
        cleaned = cleaned.replaceAll("```\\s*$", "");
        cleaned = convertFrenchNumbersToJavaScript(cleaned);
        cleaned = cleaned.replaceAll("//[^\n]*", "");
        cleaned = cleaned.replaceAll("/\\*.*?\\*/", "");
        cleaned = cleaned.replaceAll("\\n\\s*\\n", "\n");
        cleaned = cleaned.trim();

        if (!code.equals(cleaned)) {
            log.debug("Code cleaned: {} chars → {} chars", code.length(), cleaned.length());
            if (FRENCH_NUMBER_PATTERN.matcher(code).find()) {
                log.warn("Detected and corrected French number format (commas) in generated code");
            }
        }

        return cleaned;
    }

    /**
     * Convertit les nombres au format français (virgule) en format JavaScript (point).
     * 
     * Exemples de conversions :
     * - rect.x = 0,0;          → rect.x = 0.0;
     * - resize(100,5, 50,25);  → resize(100.5, 50.25);
     * - value = 123,456789;    → value = 123.456789;
     * 
     * Utilise un Matcher pour trouver tous les nombres et les remplacer.
     * 
     * @param code code contenant potentiellement des nombres français
     * @return code avec nombres convertis au format JavaScript
     */
    private static String convertFrenchNumbersToJavaScript(String code) {
        Matcher matcher = FRENCH_NUMBER_PATTERN.matcher(code);
        StringBuffer result = new StringBuffer();

        int replacementCount = 0;
        while (matcher.find()) {
            String integerPart = matcher.group(1);
            String decimalPart = matcher.group(2);

            String replacement = integerPart + "." + decimalPart;
            matcher.appendReplacement(result, replacement);

            replacementCount++;
            log.debug("Converted French number: {},{} → {}", 
                integerPart, decimalPart, replacement);
        }
        matcher.appendTail(result);

        if (replacementCount > 0) {
            log.info("Converted {} French-formatted numbers to JavaScript format", 
                replacementCount);
        }

        return result.toString();
    }

    /**
     * Valide que le code ne contient plus de nombres au format français.
     * Utile pour les tests et la validation post-nettoyage.
     * 
     * @param code code à valider
     * @return true si le code contient encore des nombres français
     */
    public static boolean containsFrenchNumbers(String code) {
        if (code == null || code.isBlank()) return false;
        return FRENCH_NUMBER_PATTERN.matcher(code).find();
    }

    /**
     * Compte le nombre de nombres français dans le code.
     * 
     * @param code code à analyser
     * @return nombre de nombres au format français trouvés
     */
    public static int countFrenchNumbers(String code) {
        if (code == null || code.isBlank()) return 0;

        Matcher matcher = FRENCH_NUMBER_PATTERN.matcher(code);
        int count = 0;
        while (matcher.find()) count++;
        return count;
    }
}