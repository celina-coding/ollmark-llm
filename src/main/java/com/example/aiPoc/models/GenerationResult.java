package com.example.aiPoc.models;

import java.util.ArrayList;
import java.util.List;

/**
 * Représente le résultat d'une génération de code réalisée par un modèle d'IA.
 *
 * <p>Cette classe stocke le code généré, sa version brute, l'état de validité ainsi que les erreurs
 * éventuelles détectées lors d'une phase de validation. Elle inclut également des informations
 * de performance comme le temps de génération et le nombre de tokens utilisés.</p>
 */
public class GenerationResult {

    /** Code final généré, éventuellement formaté ou nettoyé. */
    private String code;

    /** Code brut généré avant post-traitement (formatage, nettoyage ou validation). */
    private String rawCode;

    /** Indique si le code généré a été jugé valide après la validation. */
    private boolean isValid;

    /** Liste des erreurs détectées lors de la validation du code généré. */
    private List<ValidationError> errors = new ArrayList<>();

    /** Temps de génération en millisecondes. */
    private long generationTimeMs;

    /** Nombre de tokens utilisés pour générer ce résultat. */
    private int tokenCount;

    /** Stratégie de prompt utilisée pour générer ce code. */
    private PromptStrategy strategy;

    /**
     * Constructeur par défaut.
     * Initialise un résultat vide, non évalué.
     */
    public GenerationResult() {
    }

    /**
     * Constructeur permettant d'initialiser un résultat valide avec un code généré.
     *
     * @param code code généré
     */
    public GenerationResult(String code) {
        this.code = code;
        this.isValid = true;
    }

    /**
     * Retourne le code généré final.
     *
     * @return code généré
     */
    public String getCode() {
        return code;
    }

    /**
     * Définit le code généré final.
     *
     * @param code code généré
     */
    public void setCode(String code) {
        this.code = code;
    }

    /**
     * Retourne le code brut généré par le modèle avant post-traitement.
     *
     * @return code brut généré
     */
    public String getRawCode() {
        return rawCode;
    }

    /**
     * Définit le code brut généré par le modèle avant post-traitement.
     *
     * @param rawCode code brut généré
     */
    public void setRawCode(String rawCode) {
        this.rawCode = rawCode;
    }

    /**
     * Indique si le code généré a été jugé valide.
     *
     * @return {@code true} si le code est valide, sinon {@code false}
     */
    public boolean isValid() {
        return isValid;
    }

    /**
     * Définit la validité du code généré.
     *
     * @param valid {@code true} si le code est valide, sinon {@code false}
     */
    public void setValid(boolean valid) {
        isValid = valid;
    }

    /**
     * Retourne la liste des erreurs de validation détectées.
     *
     * @return liste des erreurs de validation
     */
    public List<ValidationError> getErrors() {
        return errors;
    }

    /**
     * Définit la liste des erreurs de validation et met à jour l'état de validité.
     *
     * @param errors liste des erreurs de validation
     */
    public void setErrors(List<ValidationError> errors) {
        this.errors = errors;
        this.isValid = errors == null || errors.isEmpty();
    }

    /**
     * Ajoute une erreur à la liste et marque le résultat comme invalide.
     *
     * @param error erreur de validation détectée
     */
    public void addError(ValidationError error) {
        this.errors.add(error);
        this.isValid = false;
    }

    /**
     * Retourne le temps nécessaire à la génération du code par l'IA.
     *
     * @return temps de génération en millisecondes
     */
    public long getGenerationTimeMs() {
        return generationTimeMs;
    }

    /**
     * Définit le temps nécessaire à la génération du code.
     *
     * @param generationTimeMs temps de génération en millisecondes
     */
    public void setGenerationTimeMs(long generationTimeMs) {
        this.generationTimeMs = generationTimeMs;
    }

    /**
     * Retourne le nombre de tokens utilisés par l'IA lors de la génération.
     *
     * @return nombre de tokens utilisés
     */
    public int getTokenCount() {
        return tokenCount;
    }

    /**
     * Définit le nombre de tokens utilisés pour la génération.
     *
     * @param tokenCount nombre de tokens utilisés
     */
    public void setTokenCount(int tokenCount) {
        this.tokenCount = tokenCount;
    }

    /**
     * Retourne la stratégie de prompt utilisée pour générer le code.
     *
     * @return stratégie utilisée
     */
    public PromptStrategy getStrategy() {
        return strategy;
    }

    /**
     * Définit la stratégie de prompt utilisée pour la génération.
     *
     * @param strategy stratégie utilisée
     */
    public void setStrategy(PromptStrategy strategy) {
        this.strategy = strategy;
    }

    /**
     * Indique si des erreurs de validation sont présentes.
     *
     * @return {@code true} s'il existe au moins une erreur, sinon {@code false}
     */
    public boolean hasErrors() {
        return errors != null && !errors.isEmpty();
    }

    /**
     * Retourne le nombre d'erreurs de validation détectées.
     *
     * @return nombre d'erreurs, ou 0 si aucune
     */
    public int getErrorCount() {
        return errors != null ? errors.size() : 0;
    }
}