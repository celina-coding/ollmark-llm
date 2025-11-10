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

        // 5. Suppression des commentaires excessifs (optionnel)
        cleanedCode = CodeUtils.removeExcessiveComments(cleanedCode);

        logger.debug("Code nettoyé: {} caractères", cleanedCode.length());

        return cleanedCode;
    }

    /**
     * Effectue un nettoyage léger du code, sans supprimer les commentaires.
     * <p>
     * Cette méthode est utile lorsqu’on souhaite conserver les commentaires
     * ou les métadonnées explicatives générées par le modèle, tout en retirant
     * les balises et artefacts inutiles.
     * </p>
     *
     * @param rawCode le code brut à nettoyer
     * @return le code partiellement nettoyé
     */
    public String cleanLightly(String rawCode) {
        if (rawCode == null || rawCode.isEmpty()) return "";

        String cleanedCode = rawCode;

        cleanedCode = CodeUtils.removeThinkTags(cleanedCode);
        cleanedCode = CodeUtils.removeMarkdownFences(cleanedCode);
        cleanedCode = CodeUtils.normalizeWhitespace(cleanedCode);

        return cleanedCode;
    }

    /**
     * Évalue si un code donné nécessite un nettoyage avant validation ou exécution.
     * <p>
     * La détection repose sur la présence de balises <think>, de blocs Markdown
     * ou de structures textuelles non conformes à du code JavaScript.
     * </p>
     *
     * @param code le code à analyser
     * @return {@code true} si le code nécessite un nettoyage, {@code false} sinon
     */
    public boolean needsCleaning(String code) {
        if (code == null || code.isEmpty()) return false;

        return CodeUtils.containsThinkTags(code) ||
               CodeUtils.containsMarkdown(code) ||
               code.contains("L'IA:") ||
               code.contains("Voici") ||
               !CodeUtils.looksLikeJavaScript(code);
    }

    /**
     * Calcule et retourne les statistiques de nettoyage entre le code brut et le code nettoyé.
     * <p>
     * Ces statistiques permettent de mesurer l’impact du nettoyage sur la taille,
     * la structure et la lisibilité du code.
     * </p>
     *
     * @param rawCode     le code brut avant nettoyage
     * @param cleanedCode le code après nettoyage
     * @return un objet {@link CleaningStats} contenant les mesures comparatives
     */
    public CleaningStats getCleaningStats(String rawCode, String cleanedCode) {
        CleaningStats stats = new CleaningStats();

        stats.setOriginalLength(rawCode != null ? rawCode.length() : 0);
        stats.setCleanedLength(cleanedCode != null ? cleanedCode.length() : 0);
        stats.setOriginalLines(CodeUtils.countLines(rawCode));
        stats.setCleanedLines(CodeUtils.countLines(cleanedCode));
        stats.setHadThinkTags(rawCode != null && CodeUtils.containsThinkTags(rawCode));
        stats.setHadMarkdown(rawCode != null && CodeUtils.containsMarkdown(rawCode));
    
        return stats;
    }

    /**
     * Classe interne représentant les statistiques issues d’une opération de nettoyage.
     * <p>
     * Elle fournit des indicateurs de volume (longueur, nombre de lignes) et
     * d’éléments détectés (balises <think>, Markdown), permettant une analyse
     * fine du gain de qualité obtenu après post-traitement.
     * </p>
     */
    public static class CleaningStats {
        private int originalLength;
        private int cleanedLength;
        private int originalLines;
        private int cleanedLines;
        private boolean hadThinkTags;
        private boolean hadMarkdown;

        /**
         * Retourne la longueur initiale du code avant nettoyage.
         *
         * @return nombre de caractères du code brut
         */
        public int getOriginalLength() {
            return originalLength;
        }

        /**
         * Définit la longueur initiale du code avant nettoyage.
         *
         * @param originalLength nombre de caractères du code brut
         */
        public void setOriginalLength(int originalLength) {
            this.originalLength = originalLength;
        }

        /**
         * Retourne la longueur du code après nettoyage.
         *
         * @return nombre de caractères du code nettoyé
         */
        public int getCleanedLength() {
            return cleanedLength;
        }

        /**
         * Définit la longueur du code après nettoyage.
         *
         * @param cleanedLength nombre de caractères du code nettoyé
         */
        public void setCleanedLength(int cleanedLength) {
            this.cleanedLength = cleanedLength;
        }

        /**
         * Retourne le nombre de lignes avant nettoyage.
         *
         * @return nombre de lignes du code brut
         */
        public int getOriginalLines() {
            return originalLines;
        }

        /**
         * Définit le nombre de lignes avant nettoyage.
         *
         * @param originalLines nombre de lignes du code brut
         */
        public void setOriginalLines(int originalLines) {
            this.originalLines = originalLines;
        }

        /**
         * Retourne le nombre de lignes après nettoyage.
         *
         * @return nombre de lignes du code nettoyé
         */
        public int getCleanedLines() {
            return cleanedLines;
        }

        /**
         * Définit le nombre de lignes après nettoyage.
         *
         * @param cleanedLines nombre de lignes du code nettoyé
         */
        public void setCleanedLines(int cleanedLines) {
            this.cleanedLines = cleanedLines;
        }

        /**
         * Indique si le code initial contenait des balises <think>.
         *
         * @return {@code true} si des balises <think> ont été détectées
         */
        public boolean isHadThinkTags() {
            return hadThinkTags;
        }

        /**
         * Définit la présence de balises <think> dans le code brut.
         *
         * @param hadThinkTags valeur booléenne indiquant la présence de balises
         */
        public void setHadThinkTags(boolean hadThinkTags) {
            this.hadThinkTags = hadThinkTags;
        }

        /**
         * Indique si le code initial contenait des marqueurs Markdown.
         *
         * @return {@code true} si des marqueurs Markdown ont été détectés
         */
        public boolean isHadMarkdown() {
            return hadMarkdown;
        }

        /**
         * Définit la présence de marqueurs Markdown dans le code brut.
         *
         * @param hadMarkdown valeur booléenne indiquant la présence de balises Markdown
         */
        public void setHadMarkdown(boolean hadMarkdown) {
            this.hadMarkdown = hadMarkdown;
        }

        /**
         * Calcule le pourcentage de réduction en taille du code après nettoyage.
         *
         * @return pourcentage de réduction (entre 0 et 100)
         */
        public int getReductionPercent() {
            if (originalLength == 0) return 0;
            return (int) (((originalLength - cleanedLength) / (double) originalLength) * 100);
        }
    }
}