package com.example.aiPoc.models;

/**
 * Représente les différentes stratégies de construction de prompts utilisées
 * dans le cadre de l'interaction avec le modèle d'IA.
 *
 * <p>
 * Chaque stratégie est enrichit avec la documentation complète du SDK Penpot,
 * ainsi que quelques exemples selon le contexte de la stratégie utilisée.
 * </p>
 *
 * <p>
 * Les stratégies disponibles sont :
 * <ul>
 *   <li>{@link #CREATION} : Prompt enrichi avec la documentation complète du SDK Penpot.</li>
 * </ul>
 * </p>
 */
public enum PromptStrategy {

    /**
     * Stratégie détaillée : génère un prompt mettant dans le contexte de la création d'un contenu.
     * <p>
     * Cette stratégie est pertinente pour les tâches de création de composants.
     * </p>
     */
    CREATION("creation", "Inclut la documentation complète du SDK Penpot");

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
     * la stratégie {@link #CREATION} est retournée par défaut.
     * </p>
     *
     * @param value la valeur textuelle de la stratégie recherchée
     * @return la stratégie correspondante si trouvée, ou {@link #CREATION} par défaut
     */
    public static PromptStrategy fromValue(String value) {
        for (PromptStrategy strategy : values()) {
            if (strategy.value.equalsIgnoreCase(value)) {
                return strategy;
            }
        }
        return CREATION;
    }

    @Override
    public String toString() {
        return value;
    }
}