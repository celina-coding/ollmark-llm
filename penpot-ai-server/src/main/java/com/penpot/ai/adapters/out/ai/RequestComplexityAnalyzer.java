package com.penpot.ai.adapters.out.ai;

import com.penpot.ai.core.domain.TaskComplexity;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.regex.Pattern;

/**
 * Analyse la complexité d'un message utilisateur pour sélectionner
 * le profil d'options Ollama le plus adapté.
 *
 * <h2>Heuristiques de classification</h2>
 * <ul>
 *     <li><b>COMPLEX</b> : mots-clés indiquant une orchestration multi-étapes,
 *         une création complète, ou une planification globale.</li>
 *     <li><b>CREATIVE</b> : termes liés à la suggestion, la recommandation,
 *         l'inspiration ou le choix esthétique.</li>
 *     <li><b>SIMPLE</b> : actions atomiques sur un seul élément (couleur, position,
 *         taille, nom).</li>
 * </ul>
 *
 * <p>La classification se base sur :
 * <ol>
 *     <li>Longueur du message (seuil : 150 caractères)</li>
 *     <li>Présence de conjonctions de coordination indiquant une séquence</li>
 *     <li>Dictionnaire de mots-clés par niveau</li>
 * </ol>
 */
@Slf4j
@Component
public class RequestComplexityAnalyzer {

    // ==================== SEUILS ====================

    /** Longueur minimale pour suspecter une tâche complexe. */
    private static final int COMPLEX_LENGTH_THRESHOLD = 150;

    /** Longueur minimale pour suspecter une tâche créative. */
    private static final int CREATIVE_LENGTH_THRESHOLD = 60;

    // ==================== MOTS-CLÉS COMPLEXITÉ ====================

    /** Indications de tâche complexe et multi-étapes. */
    private static final List<String> COMPLEX_KEYWORDS = List.of(
        // Orchestration complète
        "crée", "créé", "create", "génère", "generate",
        "design complet", "complete design", "landing page",
        "page entière", "full page", "tout le design",
        "marketing design", "template complet",
        // Multi-étapes explicites
        "puis", "ensuite", "après", "then", "after",
        "step by step", "étape par étape",
        "d'abord", "first", "finally", "finalement",
        // Planification
        "planifie", "plan", "organise", "structure",
        "architecture", "compose",
        // Post Instagram / Social media complet
        "post instagram", "post facebook", "social media post",
        "email complet", "complete email", "newsletter"
    );

    /** Pattern pour détecter plusieurs conjonctions de séquence dans un message. */
    private static final Pattern SEQUENCE_PATTERN = Pattern.compile(
        "(?i)(puis|ensuite|après|then|after|and also|et aussi|également)",
        Pattern.CASE_INSENSITIVE
    );

    // ==================== MOTS-CLÉS CRÉATIF ====================

    /** Indications de tâche créative nécessitant diversité. */
    private static final List<String> CREATIVE_KEYWORDS = List.of(
        // Suggestions et recommandations
        "suggère", "suggère-moi", "suggest", "recommend", "recommande",
        "propose", "idée", "idea", "inspiration",
        // Choix esthétiques
        "moderne", "modern", "minimaliste", "minimal",
        "coloré", "colorful", "professionnel", "professional",
        "style", "thème", "theme", "ambiance", "mood",
        // Layout et composition
        "layout", "disposition", "arrangement", "composition",
        "palette", "couleurs", "colors", "typographie", "typography",
        // Questions ouvertes
        "comment", "how", "quel", "what", "quoi",
        "mieux", "better", "améliore", "improve"
    );

    // ==================== MOTS-CLÉS SIMPLE ====================

    /** Indications de tâche atomique simple. */
    private static final List<String> SIMPLE_KEYWORDS = List.of(
        // Opérations atomiques sur la couleur
        "change color", "change la couleur", "couleur",
        "fill", "remplis", "background color",
        // Position et taille
        "déplace", "move", "resize", "redimensionne",
        "x=", "y=", "width=", "height=",
        "rotate", "rotation", "tourne",
        // Suppression simple
        "delete", "supprime", "remove",
        // Renommage
        "rename", "renomme", "name",
        // Opacité
        "opacity", "opacité", "transparent"
    );

    // ==================== API PUBLIQUE ====================

    /**
     * Analyse le message utilisateur et retourne le niveau de complexité détecté.
     *
     * @param message le message de l'utilisateur
     * @return le niveau de complexité ({@link TaskComplexity})
     */
    public TaskComplexity analyze(String message) {
        if (message == null || message.isBlank()) {
            log.debug("Empty message → defaulting to SIMPLE");
            return TaskComplexity.SIMPLE;
        }

        String normalized = message.toLowerCase().trim();

        // 1. Vérification longueur + séquences → COMPLEX en priorité
        if (isComplex(normalized)) {
            log.debug("Complexity detected: COMPLEX (message='{}...')",
                message.substring(0, Math.min(50, message.length())));
            return TaskComplexity.COMPLEX;
        }

        // 2. Mots-clés créatifs
        if (isCreative(normalized)) {
            log.debug("Complexity detected: CREATIVE (message='{}...')",
                message.substring(0, Math.min(50, message.length())));
            return TaskComplexity.CREATIVE;
        }

        // 3. Par défaut → SIMPLE
        log.debug("Complexity detected: SIMPLE (message='{}...')",
            message.substring(0, Math.min(50, message.length())));
        return TaskComplexity.SIMPLE;
    }

    // ==================== MÉTHODES PRIVÉES ====================

    /**
     * Détermine si le message indique une tâche complexe.
     * Critères (au moins l'un doit être vrai) :
     * <ol>
     *     <li>Longueur > {@value #COMPLEX_LENGTH_THRESHOLD} caractères</li>
     *     <li>Présence de 2+ conjonctions de séquence</li>
     *     <li>Présence d'un mot-clé de complexité</li>
     * </ol>
     */
    private boolean isComplex(String normalized) {
        // Critère 1 : longueur
        if (normalized.length() > COMPLEX_LENGTH_THRESHOLD) return true;

        // Critère 2 : plusieurs conjonctions de séquence
        long sequenceCount = SEQUENCE_PATTERN.matcher(normalized).results().count();
        if (sequenceCount >= 2) return true;

        // Critère 3 : mot-clé complexe
        return COMPLEX_KEYWORDS.stream().anyMatch(normalized::contains);
    }

    /**
     * Détermine si le message indique une tâche créative.
     */
    private boolean isCreative(String normalized) {
        boolean intermediateLength = normalized.length() > CREATIVE_LENGTH_THRESHOLD;
        boolean hasSimpleKeyword = SIMPLE_KEYWORDS.stream().anyMatch(normalized::contains);

        if (intermediateLength && !hasSimpleKeyword) return true;
        return CREATIVE_KEYWORDS.stream().anyMatch(normalized::contains);
    }
}