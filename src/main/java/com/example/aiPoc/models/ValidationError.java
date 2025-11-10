package com.example.aiPoc.models;

/**
 * <p>
 * Représente une erreur de validation survenue lors de l'analyse ou de l'exécution d'un code.
 * </p>
 *
 * <p>
 * Une {@code ValidationError} contient des informations détaillées sur la nature de l’erreur,
 * notamment son type, le message descriptif, la position dans le code (ligne et colonne)
 * et le niveau de sévérité associé.
 * </p>
 *
 * <p>
 * Les principaux types d’erreurs possibles sont :
 * <ul>
 *   <li><b>SYNTAX</b> : erreur liée à la structure du code.</li>
 *   <li><b>SEMANTIC</b> : erreur logique ou sémantique dans le code.</li>
 *   <li><b>API_USAGE</b> : erreur d’utilisation incorrecte d’une API ou d’un SDK.</li>
 * </ul>
 * </p>
 *
 * <p>
 * Les niveaux de sévérité disponibles sont :
 * <ul>
 *   <li><b>ERROR</b> : erreur bloquante nécessitant une correction immédiate.</li>
 *   <li><b>WARNING</b> : avertissement sur un comportement potentiellement problématique.</li>
 *   <li><b>INFO</b> : message informatif sans impact sur l’exécution.</li>
 * </ul>
 * </p>
 */
public class ValidationError {
    
    /** Type d’erreur (par exemple : SYNTAX, SEMANTIC, API_USAGE). */
    private String type;
    
    /** Message explicatif décrivant l’erreur détectée. */
    private String message;
    
    /** Ligne du code source où l’erreur a été détectée (facultative). */
    private Integer line;
    
    /** Colonne du code source où l’erreur a été détectée (facultative). */
    private Integer column;
    
    /** Niveau de sévérité de l’erreur (par exemple : ERROR, WARNING, INFO). */
    private String severity;

    /**
     * Constructeur par défaut.
     * <p>
     * Initialise une instance vide de {@code ValidationError}. 
     * Les champs doivent être définis via les accesseurs.
     * </p>
     */
    public ValidationError() {
    }

    /**
     * Constructeur simplifié.
     *
     * @param type    le type de l’erreur (ex. SYNTAX, SEMANTIC, API_USAGE)
     * @param message le message descriptif associé à l’erreur
     */
    public ValidationError(String type, String message) {
        this.type = type;
        this.message = message;
        this.severity = "ERROR";
    }

    /**
     * Constructeur complet avec position.
     *
     * @param type    le type de l’erreur (ex. SYNTAX, SEMANTIC, API_USAGE)
     * @param message le message explicatif décrivant l’erreur
     * @param line    le numéro de ligne où l’erreur est survenue
     * @param column  le numéro de colonne où l’erreur est survenue
     */
    public ValidationError(String type, String message, Integer line, Integer column) {
        this.type = type;
        this.message = message;
        this.line = line;
        this.column = column;
        this.severity = "ERROR";
    }

    /**
     * Retourne le type de l’erreur.
     *
     * @return le type d’erreur (SYNTAX, SEMANTIC, API_USAGE)
     */
    public String getType() {
        return type;
    }

    /**
     * Définit le type de l’erreur.
     *
     * @param type le type d’erreur (SYNTAX, SEMANTIC, API_USAGE)
     */
    public void setType(String type) {
        this.type = type;
    }

    /**
     * Retourne le message explicatif de l’erreur.
     *
     * @return le message associé à l’erreur
     */
    public String getMessage() {
        return message;
    }

    /**
     * Définit le message explicatif de l’erreur.
     *
     * @param message le message associé à l’erreur
     */
    public void setMessage(String message) {
        this.message = message;
    }

    /**
     * Retourne le numéro de ligne du code source où l’erreur a été détectée.
     *
     * @return la ligne correspondante, ou {@code null} si non renseignée
     */
    public Integer getLine() {
        return line;
    }

    /**
     * Définit le numéro de ligne du code source où l’erreur a été détectée.
     *
     * @param line la ligne de l’erreur
     */
    public void setLine(Integer line) {
        this.line = line;
    }

    /**
     * Retourne le numéro de colonne du code source où l’erreur a été détectée.
     *
     * @return la colonne correspondante, ou {@code null} si non renseignée
     */
    public Integer getColumn() {
        return column;
    }

    /**
     * Définit le numéro de colonne du code source où l’erreur a été détectée.
     * <p>
     * Cette méthode convertit la valeur textuelle fournie en entier avant affectation.
     * </p>
     *
     * @param column la colonne sous forme de chaîne à convertir
     * @throws NumberFormatException si la valeur ne peut pas être convertie en entier
     */
    public void setColumn(String column) {
        this.column = Integer.valueOf(column);
    }

    /**
     * Retourne le niveau de sévérité de l’erreur.
     *
     * @return la sévérité de l’erreur (ERROR, WARNING, INFO)
     */
    public String getSeverity() {
        return severity;
    }

    /**
     * Définit le niveau de sévérité de l’erreur.
     *
     * @param severity le niveau de sévérité (ERROR, WARNING, INFO)
     */
    public void setSeverity(String severity) {
        this.severity = severity;
    }

    @Override
    public String toString() {
        return "ValidationError{" +
                "type='" + type + '\'' +
                ", message='" + message + '\'' +
                ", line=" + line +
                ", column=" + column +
                ", severity='" + severity + '\'' +
                '}';
    }
}