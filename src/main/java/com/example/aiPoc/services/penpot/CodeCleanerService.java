package com.example.aiPoc.services.penpot;

import org.slf4j.*;
import org.springframework.stereotype.Service;
import com.example.aiPoc.utils.CodeUtils;

/**
 * Service responsable du nettoyage et de la normalisation du code généré par l'IA.
 * <p>
 * Ce service s'assure que le code produit par les modèles est prêt à être utilisé,
 * lisible et exempt d'artefacts de génération tels que les balises de réflexion
 * ou les marqueurs Markdown.
 * </p>
 *
 * @see CodeUtils
 */
@Service
public class CodeCleanerService {

    /** Logger utilisé pour le suivi et le débogage du service. */
    private static final Logger logger = LoggerFactory.getLogger(CodeCleanerService.class);

    /**
     * Nettoie en profondeur un code généré par l'IA afin de le rendre exploitable.
     * <p>
     * Cette méthode supprime les balises de réflexion, les balises Markdown,
     * extrait le code JavaScript et normalise les espaces.
     * </p>
     *
     * @param rawCode le code brut généré par l'IA (potentiellement bruité)
     * @return le code nettoyé, formaté et prêt à être validé
     */
    public String clean(String rawCode) {
        if (rawCode == null || rawCode.trim().isEmpty()) {
            logger.warn("Code vide ou null reçu pour nettoyage");
            return "";
        }

        logger.debug("Nettoyage du code: {} caractères", rawCode.length());

        String cleanedCode = rawCode;
        int initialLength = rawCode.length();

        try {
            // 1. Suppression des balises <think>
            if (CodeUtils.containsThinkTags(cleanedCode)) {
                logger.debug("  → Suppression des balises <think>");
                cleanedCode = CodeUtils.removeThinkTags(cleanedCode);
            }

            // 2. Suppression des phrases explicatives
            if (CodeUtils.containsExplanatoryText(cleanedCode)) {
                logger.debug("  → Suppression des phrases explicatives");
                cleanedCode = CodeUtils.removeExplanatoryText(cleanedCode);
            }

            // 3. Suppression des balises markdown
            if (CodeUtils.containsMarkdown(cleanedCode)) {
                logger.debug("  → Suppression des balises markdown");
                cleanedCode = CodeUtils.removeMarkdownFences(cleanedCode);
            }

            // 4. Extraction du code JavaScript
            logger.debug("  → Extraction du code JavaScript");
            String extractedCode = CodeUtils.extractJavaScriptCode(cleanedCode);
            
            // Si l'extraction retourne du vide mais qu'on avait du contenu, garder le contenu nettoyé
            if ((extractedCode == null || extractedCode.trim().isEmpty()) && 
                !cleanedCode.trim().isEmpty()) {
                logger.warn("    L'extraction JavaScript a échoué, on garde le code nettoyé");
                extractedCode = cleanedCode;
            }
            cleanedCode = extractedCode;

            // 4. Normalisation des espaces
            logger.debug("  → Normalisation des espaces");
            cleanedCode = CodeUtils.normalizeWhitespace(cleanedCode);

            // 5. Suppression des commentaires excessifs (optionnel)
            logger.debug("  → Suppression des commentaires excessifs");
            cleanedCode = CodeUtils.removeExcessiveComments(cleanedCode);

            if (cleanedCode == null || cleanedCode.trim().isEmpty()) {
                logger.error("   Le nettoyage a produit un résultat vide !");
                logger.error("  Code original (premiers 200 chars): {}", 
                            rawCode.substring(0, Math.min(200, rawCode.length())));

                return rawCode.trim();
            }

            int reductionPercent = (int) (((initialLength - cleanedCode.length()) * 100.0) / initialLength);
            logger.info("Code nettoyé: {} → {} caractères (-{}%)", 
                       initialLength, cleanedCode.length(), reductionPercent);

            return cleanedCode;
        } catch (Exception e) {
            logger.error("Erreur lors du nettoyage du code", e);
            logger.error("Code problématique (premiers 200 chars): {}", 
                        rawCode.substring(0, Math.min(200, rawCode.length())));
            return rawCode.trim();
        }
    }
}