package com.example.aiPoc.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Représente la requête utilisée pour demander la génération de code Penpot.
 *
 * <p>Ce DTO permet au client de spécifier :
 * <ul>
 *   <li>le prompt utilisateur décrivant le besoin</li>
 *   <li>la stratégie de génération de code</li>
 *   <li>si une validation automatique du code doit être effectuée</li>
 *   <li>si le code généré doit être nettoyé avant renvoi</li>
 * </ul>
 *
 * <p>Les valeurs par défaut sont :
 * <ul>
 *   <li>strategy : {@code creation}</li>
 *   <li>includeValidation : {@code true}</li>
 *   <li>cleanCode : {@code true}</li>
 * </ul>
 * </p>
 */
public class CodeGenerationRequest {

    /** Prompt fourni par l'utilisateur servant de base à la génération de code. */
    @JsonProperty("prompt")
    private String prompt;

    /**
     * Stratégie de génération de code utilisée par le moteur.
     * <p>Peut être par exemple : {@code creation}.</p>
     * Valeur par défaut : {@code creation}.
     */
    @JsonProperty("strategy")
    private String strategy = "creation";

    /**
     * Indique si le code généré doit être validé (syntaxe, structure, conformité, etc.).
     * <p>Valeur par défaut : {@code true}.</p>
     */
    @JsonProperty("includeValidation")
    private boolean includeValidation = true;

    /**
     * Indique si le code généré doit être nettoyé (suppression des artefacts, formatage, etc.).
     * <p>Valeur par défaut : {@code true}.</p>
     */
    @JsonProperty("cleanCode")
    private boolean cleanCode = true;

    /**
     * Constructeur par défaut requis pour la désérialisation JSON.
     */
    public CodeGenerationRequest() {
    }

    /**
     * Crée une nouvelle requête de génération de code avec le prompt
     * et la stratégie spécifiés.
     *
     * @param prompt   le prompt de génération fourni par l'utilisateur
     * @param strategy la stratégie à appliquer pour la génération
     */
    public CodeGenerationRequest(String prompt, String strategy) {
        this.prompt = prompt;
        this.strategy = strategy;
    }

    /**
     * Retourne le prompt utilisateur utilisé pour la génération.
     *
     * @return le prompt de génération
     */
    public String getPrompt() {
        return prompt;
    }

    /**
     * Définit le prompt de génération.
     *
     * @param prompt le prompt utilisateur décrivant la demande de génération
     */
    public void setPrompt(String prompt) {
        this.prompt = prompt;
    }

    /**
     * Retourne la stratégie de génération de code.
     *
     * @return la stratégie de génération
     */
    public String getStrategy() {
        return strategy;
    }

    /**
     * Définit la stratégie utilisée pour la génération de code.
     *
     * @param strategy nom de la stratégie (ex. : {@code creation})
     */
    public void setStrategy(String strategy) {
        this.strategy = strategy;
    }

    /**
     * Indique si la validation automatique du code généré est activée.
     *
     * @return {@code true} si la validation est activée, sinon {@code false}
     */
    public boolean isIncludeValidation() {
        return includeValidation;
    }

    /**
     * Active ou désactive la validation du code généré.
     *
     * @param includeValidation {@code true} pour activer la validation, sinon {@code false}
     */
    public void setIncludeValidation(boolean includeValidation) {
        this.includeValidation = includeValidation;
    }

    /**
     * Indique si le nettoyage du code généré est activé.
     *
     * @return {@code true} si le code doit être nettoyé, sinon {@code false}
     */
    public boolean isCleanCode() {
        return cleanCode;
    }

    /**
     * Active ou désactive le nettoyage du code généré.
     *
     * @param cleanCode {@code true} pour activer le nettoyage, sinon {@code false}
     */
    public void setCleanCode(boolean cleanCode) {
        this.cleanCode = cleanCode;
    }

    @Override
    public String toString() {
        return "CodeGenerationRequest{" +
                "prompt='" + (prompt != null ? prompt.substring(0, Math.min(50, prompt.length())) : "null") +
                "', strategy='" + strategy + '\'' +
                ", includeValidation=" + includeValidation +
                ", cleanCode=" + cleanCode +
                '}';
    }
}