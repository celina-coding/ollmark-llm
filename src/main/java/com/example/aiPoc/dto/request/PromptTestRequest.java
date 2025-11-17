package com.example.aiPoc.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Représente une requête utilisée pour tester différentes stratégies de prompts
 * dans le processus de génération de code Penpot.
 *
 * <p>Ce DTO permet au client de spécifier :
 * <ul>
 *     <li>Le prompt à tester</li>
 *     <li>La stratégie de génération à utiliser</li>
 *     <li>Si des métriques supplémentaires doivent être retournées pour l'analyse</li>
 * </ul>
 * 
 * <p>Il est principalement utilisé par l'endpoint de test des stratégies de génération.</p>
 */
public class PromptTestRequest {

    /** Prompt fourni par l'utilisateur et servant de base au test de génération. */
    @JsonProperty("prompt")
    private String prompt;

    /**
     * Stratégie de génération de code à tester.  
     * Par exemple : {@code creation}.
     */
    @JsonProperty("strategy")
    private String strategy;

    /**
     * Indique si les métriques de génération doivent être incluses dans la réponse
     * (ex: temps de génération, longueur du code, tokens estimés, qualité, etc.).
     * <p>Valeur par défaut : {@code true}.</p>
     */
    @JsonProperty("includeMetrics")
    private boolean includeMetrics = true;

    /**
     * Constructeur par défaut requis pour la désérialisation JSON.
     */
    public PromptTestRequest() {
    }

    /**
     * Crée une nouvelle requête de test avec un prompt et une stratégie spécifiés.
     *
     * @param prompt   le prompt utilisateur servant de base au test
     * @param strategy la stratégie à tester
     */
    public PromptTestRequest(String prompt, String strategy) {
        this.prompt = prompt;
        this.strategy = strategy;
    }

    /**
     * Retourne le prompt utilisateur utilisé pour le test de stratégie.
     *
     * @return le prompt fourni
     */
    public String getPrompt() {
        return prompt;
    }

    /**
     * Définit le prompt à tester.
     *
     * @param prompt le prompt utilisateur décrivant la demande de génération
     */
    public void setPrompt(String prompt) {
        this.prompt = prompt;
    }

    /**
     * Retourne la stratégie de génération à tester.
     *
     * @return la stratégie de génération
     */
    public String getStrategy() {
        return strategy;
    }

    /**
     * Définit la stratégie utilisée pour le test.
     *
     * @param strategy nom de la stratégie à évaluer (ex: {@code creation})
     */
    public void setStrategy(String strategy) {
        this.strategy = strategy;
    }

    /**
     * Indique si les métriques doivent être incluses dans la réponse de test.
     *
     * @return {@code true} si les métriques sont incluses, sinon {@code false}
     */
    public boolean isIncludeMetrics() {
        return includeMetrics;
    }

    /**
     * Active ou désactive l'inclusion des métriques dans la réponse.
     *
     * @param includeMetrics {@code true} pour inclure les métriques, sinon {@code false}
     */
    public void setIncludeMetrics(boolean includeMetrics) {
        this.includeMetrics = includeMetrics;
    }
}