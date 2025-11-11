package com.example.aiPoc.services.penpot;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.example.aiPoc.utils.CodeUtils;

/**
 * Service responsable du nettoyage et de la normalisation du code généré par l'IA.
 * <p>
 * Ce service s'assure que le code produit par les modèles est prêt à être utilisé,
 * lisible et exempt d'artefacts de génération tels que les balises de réflexion
 * ou les marqueurs Markdown. Il constitue une étape clé de la post-traitement
 * avant validation et exécution du code.
 * </p>
 *
 * <h3>Responsabilités principales :</h3>
 * <ul>
 *   <li>Suppression des artefacts de génération (balises <think>, Markdown, etc.)</li>
 *   <li>Extraction du code JavaScript pur à partir de réponses mixtes</li>
 *   <li>Normalisation du formatage (espaces, sauts de ligne, indentation)</li>
 *   <li>Fourniture de statistiques sur le nettoyage effectué</li>
 * </ul>
 *
 * <p>
 * Ce service repose sur la classe utilitaire {@link CodeUtils} pour effectuer la
 * majorité des opérations de nettoyage.
 * </p>
 */
@Service
public class CodeCleanerService {

    private static final Logger logger = LoggerFactory.getLogger(CodeCleanerService.class);

    /**
     * Nettoie en profondeur un code généré par l'IA afin de le rendre exploitable.
     * <p>
     * Cette méthode supprime les balises de réflexion <think>, les balises Markdown,
     * extrait le code JavaScript et normalise les espaces. Elle peut également
     * (optionnellement) supprimer les commentaires superflus.
     * </p>
     *
     * @param rawCode le code brut généré par l'IA (potentiellement bruité)
     * @return le code nettoyé, formaté et prêt à être validé
     */
    public String clean(String rawCode) {
        if (rawCode == null || rawCode.isEmpty()) {
            logger.warn("Code vide ou null reçu pour nettoyage");
            return "";
        }

        logger.debug("Nettoyage du code: {} caractères", rawCode.length());

        String cleanedCode = rawCode;

        // 1. Suppression des balises <think>
        if (CodeUtils.containsThinkTags(cleanedCode)) {
            logger.debug("Suppression des balises <think>");
            cleanedCode = CodeUtils.removeThinkTags(cleanedCode);
        }

        // 2. Suppression des balises markdown
        if (CodeUtils.containsMarkdown(cleanedCode)) {
            logger.debug("Suppression des balises markdown");
            cleanedCode = CodeUtils.removeMarkdownFences(cleanedCode);
        }

        // 3. Extraction du code JavaScript
        cleanedCode = CodeUtils.extractJavaScriptCode(cleanedCode);

        // 4. Normalisation des espaces
        cleanedCode = CodeUtils.normalizeWhitespace(cleanedCode);

        // 5. Suppression des commentaires excessifs
        cleanedCode = CodeUtils.removeExcessiveComments(cleanedCode);

        logger.debug("Code nettoyé: {} caractères", cleanedCode.length());

        return cleanedCode;
    }
}