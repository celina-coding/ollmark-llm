package com.example.aiPoc.models;

/**
 * Représente les différentes stratégies de construction de prompts utilisées
 * dans le cadre de l'interaction avec le modèle d'IA.
 *
 * <p>
 * Chaque stratégie définit un niveau de contextualisation et de structuration
 * différent, afin d’adapter la formulation des requêtes au modèle selon le cas
 * d’usage.
 * </p>
 *
 * <p>
 * Les stratégies disponibles sont :
 * <ul>
 *   <li>{@link #BASIC} : Prompt minimaliste sans contexte détaillé.</li>
 *   <li>{@link #DETAILED} : Prompt enrichi avec la documentation complète du SDK Penpot.</li>
 *   <li>{@link #WITH_EXAMPLES} : Prompt intégrant des exemples concrets (few-shot learning).</li>
 *   <li>{@link #STRUCTURED} : Prompt structuré avec sections clairement identifiées.</li>
 * </ul>
 * </p>
 */
public enum PromptStrategy {

    /**
     * Stratégie basique : génère un prompt minimal sans contexte spécifique ni structure avancée.
     * <p>
     * Cette approche est adaptée aux requêtes simples ne nécessitant pas de compréhension approfondie.
     * </p>
     */
    BASIC("basic", "Prompt simple sans contexte détaillé"),

    /**
     * Stratégie détaillée : génère un prompt complet intégrant la documentation du SDK Penpot.
     * <p>
     * Cette stratégie est pertinente pour les tâches nécessitant une connaissance du contexte applicatif.
     * </p>
     */
    DETAILED("detailed", "Inclut la documentation complète du SDK Penpot"),

    /**
     * Stratégie avec exemples : intègre des exemples concrets dans le prompt afin de favoriser
     * l’apprentissage contextuel (few-shot learning).
     * <p>
     * Recommandée pour les cas où le modèle doit reproduire un comportement observé dans les exemples fournis.
     * </p>
     */
    WITH_EXAMPLES("with-examples", "Inclut des exemples de code fonctionnels"),

    /**
     * Stratégie structurée : construit un prompt organisé en sections distinctes telles que
     * [SYSTÈME], [API] et [TÂCHE].
     * <p>
     * Cette approche améliore la lisibilité et la cohérence du prompt pour des interactions complexes.
     * </p>
     */
    STRUCTURED("structured", "Format structuré avec balises [SYSTÈME], [API], [TÂCHE]");

    /** Valeur textuelle associée à la stratégie (identifiant interne ou clé de configuration). */
    private final String value;

    /** Description explicative de la stratégie. */
    private final String description;

    /**
     * Constructeur interne de l’énumération.
     *
     * @param value       la valeur textuelle représentant la stratégie
     * @param description une description détaillée de la stratégie
     */
    PromptStrategy(String value, String description) {
        this.value = value;
        this.description = description;
    }

    /**
     * Retourne la valeur textuelle associée à la stratégie.
     *
     * @return la valeur unique identifiant la stratégie
     */
    public String getValue() {
        return value;
    }

    /**
     * Fournit une description détaillée de la stratégie.
     *
     * @return la description associée à la stratégie
     */
    public String getDescription() {
        return description;
    }

    /**
     * Recherche une stratégie à partir de sa valeur textuelle.
     *
     * <p>
     * La recherche est insensible à la casse. Si aucune correspondance n’est trouvée,
     * la stratégie {@link #DETAILED} est retournée par défaut.
     * </p>
     *
     * @param value la valeur textuelle de la stratégie recherchée
     * @return la stratégie correspondante si trouvée, ou {@link #DETAILED} par défaut
     */
    public static PromptStrategy fromValue(String value) {
        for (PromptStrategy strategy : values()) {
            if (strategy.value.equalsIgnoreCase(value)) {
                return strategy;
            }
        }
        return DETAILED;
    }

    @Override
    public String toString() {
        return value;
    }
}