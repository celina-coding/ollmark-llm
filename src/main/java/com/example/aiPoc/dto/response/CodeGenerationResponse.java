package com.example.aiPoc.dto.response;

import java.util.ArrayList;
import java.util.List;

import com.example.aiPoc.models.ValidationError;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Représente la réponse retournée par le service de génération de code Penpot.
 *
 * <p>Ce DTO contient :
 * <ul>
 *     <li>Le code généré et ses métadonnées</li>
 *     <li>Les informations sur la stratégie utilisée</li>
 *     <li>Le prompt utilisateur et la version enrichie éventuellement utilisée pour la génération</li>
 *     <li>Le résultat de la validation du code (réussite/échec, erreurs éventuelles)</li>
 *     <li>Des statistiques de génération (temps, taille, tokens estimés, timestamp)</li>
 * </ul>
 * 
 * <p>Certains champs sont automatiquement mis à jour lors de l'appel aux setters :
 * <ul>
 *     <li>{@code setGeneratedCode()} met à jour {@code codeLength}</li>
 *     <li>{@code setEnrichedPrompt()} met à jour {@code promptTokensEstimate}</li>
 *     <li>{@code setValidationErrors()} met automatiquement à jour {@code isValid}</li>
 * </ul>
 * </p>
 */
public class CodeGenerationResponse {

    /** Code généré par le moteur IA, éventuellement nettoyé ou enrichi selon les options. */
    @JsonProperty("generatedCode")
    private String generatedCode;

    /** Stratégie utilisée pour générer le code (ex. : {@code basic}, {@code detailed}, {@code structured}). */
    @JsonProperty("strategy")
    private String strategy;

    /** Prompt original fourni par l'utilisateur. */
    @JsonProperty("userPrompt")
    private String userPrompt;

    /**
     * Version enrichie du prompt, éventuellement augmentée par le système
     * afin d'améliorer la qualité de génération (ajout de contexte, structure…).
     */
    @JsonProperty("enrichedPrompt")
    private String enrichedPrompt;

    /** Réponse brute retournée par le modèle IA avant tout traitement, parsing ou nettoyage. */
    @JsonProperty("rawResponse")
    private String rawResponse;

    /** État de validité du code généré. {@code true} si aucune erreur de validation n’a été détectée. */
    @JsonProperty("isValid")
    private boolean isValid = true;

    /**
     * Liste des erreurs de validation détectées sur le code généré
     * (syntaxe, structure, règles métier…).
     */
    @JsonProperty("validationErrors")
    private List<ValidationError> validationErrors = new ArrayList<>();

    /**
     * Longueur du code généré (en nombre de caractères).
     * Calculé automatiquement lors de l’appel de {@link #setGeneratedCode(String)}.
     */
    @JsonProperty("codeLength")
    private int codeLength;

    /**
     * Estimation du nombre de tokens utilisés par le prompt enrichi,
     * basée sur une approximation de 4 caractères par token.
     * Calculé lors de l’appel de {@link #setEnrichedPrompt(String)}.
     */
    @JsonProperty("promptTokensEstimate")
    private int promptTokensEstimate;

    /** Durée totale de génération du code, exprimée en millisecondes. */
    @JsonProperty("generationTimeMs")
    private long generationTimeMs;

    /** Timestamp Unix (en millisecondes) indiquant la date de génération de la réponse. */
    @JsonProperty("timestamp")
    private Long timestamp;

    /**
     * Constructeur par défaut.
     * Initialise automatiquement le timestamp de création de la réponse.
     */
    public CodeGenerationResponse() {
        this.timestamp = System.currentTimeMillis();
    }

    /**
     * Retourne le code généré.
     *
     * @return le code généré
     */
    public String getGeneratedCode() {
        return generatedCode;
    }

    /**
     * Définit le code généré et met automatiquement à jour sa longueur.
     *
     * @param generatedCode le code généré par le moteur IA
     */
    public void setGeneratedCode(String generatedCode) {
        this.generatedCode = generatedCode;
        this.codeLength = generatedCode != null ? generatedCode.length() : 0;
    }

    /**
     * Retourne la stratégie utilisée pour générer le code.
     *
     * @return la stratégie appliquée
     */
    public String getStrategy() {
        return strategy;
    }

    /**
     * Définit la stratégie utilisée pour générer le code.
     *
     * @param strategy nom de la stratégie choisie
     */
    public void setStrategy(String strategy) {
        this.strategy = strategy;
    }

    /**
     * Retourne le prompt original fourni par l'utilisateur.
     *
     * @return le prompt utilisateur
     */
    public String getUserPrompt() {
        return userPrompt;
    }

    /**
     * Définit le prompt original.
     *
     * @param userPrompt prompt fourni par l'utilisateur
     */
    public void setUserPrompt(String userPrompt) {
        this.userPrompt = userPrompt;
    }

    /**
     * Retourne le prompt enrichi utilisé pour la génération.
     *
     * @return le prompt enrichi
     */
    public String getEnrichedPrompt() {
        return enrichedPrompt;
    }

    /**
     * Définit le prompt enrichi et met automatiquement à jour
     * l’estimation des tokens associés.
     *
     * @param enrichedPrompt prompt enrichi
     */
    public void setEnrichedPrompt(String enrichedPrompt) {
        this.enrichedPrompt = enrichedPrompt;
        this.promptTokensEstimate = enrichedPrompt != null ? enrichedPrompt.length() / 4 : 0;
    }

    /**
     * Retourne la réponse brute fournie par le modèle IA.
     *
     * @return la réponse brute
     */
    public String getRawResponse() {
        return rawResponse;
    }

    /**
     * Définit la réponse brute renvoyée par l'IA.
     *
     * @param rawResponse réponse brute retournée par l’IA
     */
    public void setRawResponse(String rawResponse) {
        this.rawResponse = rawResponse;
    }

    /**
     * Indique si le code généré est considéré comme valide.
     *
     * @return {@code true} si le code est valide, sinon {@code false}
     */
    public boolean isValid() {
        return isValid;
    }

    /**
     * Définit si le code est valide ou non.
     *
     * @param valid nouvel état de validité
     */
    public void setValid(boolean valid) {
        isValid = valid;
    }

    /**
     * Retourne la liste des erreurs de validation du code généré.
     *
     * @return liste des erreurs de validation
     */
    public List<ValidationError> getValidationErrors() {
        return validationErrors;
    }

    /**
     * Définit la liste des erreurs de validation et met automatiquement
     * à jour le statut de validité du code.
     *
     * @param validationErrors liste des erreurs détectées
     */
    public void setValidationErrors(List<ValidationError> validationErrors) {
        this.validationErrors = validationErrors;
        this.isValid = validationErrors == null || validationErrors.isEmpty();
    }

    /**
     * Ajoute une erreur de validation et marque automatiquement la réponse comme invalide.
     *
     * @param error erreur de validation détectée
     */
    public void addValidationError(ValidationError error) {
        this.validationErrors.add(error);
        this.isValid = false;
    }

    /**
     * Retourne la longueur du code généré.
     *
     * @return taille du code en caractères
     */
    public int getCodeLength() {
        return codeLength;
    }

    /**
     * Définit la longueur du code généré.
     *
     * @param codeLength longueur du code
     */
    public void setCodeLength(int codeLength) {
        this.codeLength = codeLength;
    }

    /**
     * Retourne l'estimation du nombre de tokens du prompt enrichi.
     *
     * @return estimation du nombre de tokens
     */
    public int getPromptTokensEstimate() {
        return promptTokensEstimate;
    }

    /**
     * Définit l'estimation du nombre de tokens utilisés.
     *
     * @param promptTokensEstimate estimation des tokens
     */
    public void setPromptTokensEstimate(int promptTokensEstimate) {
        this.promptTokensEstimate = promptTokensEstimate;
    }

    /**
     * Retourne le temps de génération de la réponse.
     *
     * @return temps en millisecondes
     */
    public long getGenerationTimeMs() {
        return generationTimeMs;
    }

    /**
     * Définit le temps de génération du code.
     *
     * @param generationTimeMs temps en millisecondes
     */
    public void setGenerationTimeMs(long generationTimeMs) {
        this.generationTimeMs = generationTimeMs;
    }

    /**
     * Retourne le timestamp de création de la réponse.
     *
     * @return timestamp en millisecondes
     */
    public Long getTimestamp() {
        return timestamp;
    }

    /**
     * Définit le timestamp de création de la réponse.
     *
     * @param timestamp timestamp en millisecondes
     */
    public void setTimestamp(Long timestamp) {
        this.timestamp = timestamp;
    }
}