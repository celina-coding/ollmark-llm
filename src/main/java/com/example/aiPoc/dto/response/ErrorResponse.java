package com.example.aiPoc.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Représente une réponse d'erreur standardisée renvoyée par l'application.
 *
 * <p>Ce DTO est utilisé pour fournir des informations cohérentes et structurées
 * en cas d'erreur côté serveur ou client. Il est généralement retourné
 * par les handlers d'exceptions Spring Boot.</p>
 *
 * <p>Les informations incluent :</p>
 * <ul>
 *     <li>Un code ou type d'erreur</li>
 *     <li>Un message explicatif destiné au client</li>
 *     <li>Un timestamp permettant de tracer l'instant de l'erreur</li>
 *     <li>Le chemin de la requête ayant déclenché l'erreur</li>
 * </ul>
 */
public class ErrorResponse {

    /** Code ou type d'erreur (ex. : {@code BAD_REQUEST}, {@code INTERNAL_SERVER_ERROR}, {@code VALIDATION_ERROR}). */
    @JsonProperty("error")
    private String error;

    /** Message décrivant l'erreur, destiné à apporter une information claire au client. */
    @JsonProperty("message")
    private String message;

    /** Date et heure de l'erreur, exprimée en millisecondes (timestamp Unix). */
    @JsonProperty("timestamp")
    private Long timestamp;

    /** Chemin de la requête HTTP lors de laquelle l'erreur s'est produite. */
    @JsonProperty("path")
    private String path;

    /**
     * Constructeur par défaut.
     * Initialise automatiquement le timestamp au moment de la création de l'objet.
     */
    public ErrorResponse() {
        this.timestamp = System.currentTimeMillis();
    }

    /**
     * Constructeur permettant de définir le type d'erreur et le message.
     * Le timestamp est automatiquement généré.
     *
     * @param error   type ou code de l'erreur
     * @param message message explicatif de l'erreur
     */
    public ErrorResponse(String error, String message) {
        this.error = error;
        this.message = message;
        this.timestamp = System.currentTimeMillis();
    }

    /**
     * Constructeur complet permettant de définir le type, le message et le chemin de l'erreur.
     * Le timestamp est automatiquement généré.
     *
     * @param error   type ou code de l'erreur
     * @param message message explicatif
     * @param path    chemin ayant généré l'erreur
     */
    public ErrorResponse(String error, String message, String path) {
        this.error = error;
        this.message = message;
        this.path = path;
        this.timestamp = System.currentTimeMillis();
    }

    /**
     * Retourne le type ou code d'erreur.
     *
     * @return type d'erreur
     */
    public String getError() {
        return error;
    }

    /**
     * Définit le type ou code d'erreur.
     *
     * @param error type d'erreur
     */
    public void setError(String error) {
        this.error = error;
    }

    /**
     * Retourne le message explicatif de l'erreur.
     *
     * @return message d'erreur
     */
    public String getMessage() {
        return message;
    }

    /**
     * Définit un message explicatif de l'erreur.
     *
     * @param message message d'erreur
     */
    public void setMessage(String message) {
        this.message = message;
    }

    /**
     * Retourne le timestamp de l'erreur.
     *
     * @return timestamp Unix en millisecondes
     */
    public Long getTimestamp() {
        return timestamp;
    }

    /**
     * Définit le timestamp de l'erreur.
     *
     * @param timestamp timestamp Unix en millisecondes
     */
    public void setTimestamp(Long timestamp) {
        this.timestamp = timestamp;
    }

    /**
     * Retourne le chemin HTTP de la requête ayant causé l'erreur.
     *
     * @return chemin de la requête
     */
    public String getPath() {
        return path;
    }

    /**
     * Définit le chemin HTTP où l'erreur s'est produite.
     *
     * @param path chemin de la requête
     */
    public void setPath(String path) {
        this.path = path;
    }
}