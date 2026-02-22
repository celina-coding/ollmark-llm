package com.penpot.ai.core.domain;

/**
 * Énumération des niveaux de complexité d'une requête utilisateur.
 *
 * <p>Détermine le profil d'options Ollama à utiliser pour l'appel IA :
 * <ul>
 *     <li>{@link #SIMPLE}  — température basse, déterministe (ex : changer une couleur)</li>
 *     <li>{@link #CREATIVE} — température élevée, diversité (ex : suggérer un layout)</li>
 *     <li>{@link #COMPLEX}  — thinking activé, raisonnement profond (ex : orchestrer un design complet)</li>
 * </ul>
 *
 * @see com.penpot.ai.infrastructure.config.OllamaConfig
 * @see com.penpot.ai.adapters.out.ai.RequestComplexityAnalyzer
 */
public enum TaskComplexity {

    /**
     * Tâche simple et déterministe.
     * Exemples : changer une couleur, déplacer un élément, renommer un layer.
     * Options : temperature=0.1, topK=10
     */
    SIMPLE,

    /**
     * Tâche créative nécessitant diversité dans les réponses.
     * Exemples : suggérer un layout, proposer une palette, recommander un style.
     * Options : temperature=0.8, topK=40, topP=0.9
     */
    CREATIVE,

    /**
     * Tâche complexe nécessitant un raisonnement profond et séquentiel.
     * Exemples : orchestrer un design complet, créer une landing page, planifier une séquence multi-étapes.
     * Options : thinking activé, temperature=0.6
     */
    COMPLEX
}