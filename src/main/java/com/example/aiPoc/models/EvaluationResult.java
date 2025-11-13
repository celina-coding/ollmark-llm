package com.example.aiPoc.models;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.*;

/**
 * Représente un résultat d'évaluation d'un modèle d'IA dans le cadre du POC.
 *
 * <p>Cette classe correspond à la structure utilisée pour collecter, stocker et
 * analyser les performances d'un modèle IA selon plusieurs critères qualitatifs et quantitatifs.
 * Elle reflète exactement la structure utilisée dans le tableau Excel d’analyse.</p>
 *
 * <p>Elle inclut : des notes (sur 5), le temps de réponse, la moyenne,
 * un indicateur de faisabilité, des observations, ainsi que le code généré.</p>
 */
public class EvaluationResult {

    /** Nom du modèle IA évalué. */
    @JsonProperty("Modèle IA testé")
    private String modeleIA;

    /** Identifiant unique du prompt utilisé pour l'évaluation. */
    @JsonProperty("ID_Prompt")
    private String idPrompt;

    /** Contenu textuel du prompt soumis au modèle IA. */
    @JsonProperty("Texte Prompt")
    private String promptTexte;

    /** Temps de réponse du modèle en secondes. */
    @JsonProperty("Temps réponse (s)")
    private Double tempsReponseS;

    /** Nombre moyen de tokens utilisés pour la génération. */
    @JsonProperty("Tokens moyens")
    private Integer tokensMoyens;

    /** Note de stabilité (1 à 5) : capacité du modèle à fournir des réponses cohérentes d’une exécution à l’autre. */
    @JsonProperty("Stabilité (/5)")
    private Integer stabilite;

    /** Note de cohérence (1 à 5) : structure logique, syntaxe et enchaînement des idées. */
    @JsonProperty("Cohérence (/5)")
    private Integer coherence;

    /** Note de respect du sujet (1 à 5) : adéquation de la réponse aux attentes du prompt. */
    @JsonProperty("Respect du sujet (/5)")
    private Integer respectSujet;

    /** Note de richesse structurelle (1 à 5) : qualité de l'organisation, hiérarchisation et complétude du contenu. */
    @JsonProperty("Richesse structurelle (/5)")
    private Integer richesseStructurelle;

    /** Note de créativité (1 à 5). */
    @JsonProperty("Créativité (/5)")
    private Integer creativite;

    /** Moyenne globale calculée sur les 5 critères notés. */
    @JsonProperty("Moyenne globale")
    private Double moyenneGlobale;

    /** Indique si la solution est exploitable et réaliste en contexte métier. */
    @JsonProperty("Réalisable ?")
    private Boolean realisable;

    /** Remarques ou points d'attention concernant l'évaluation. */
    @JsonProperty("Observations")
    private String observations;

    /** Code généré par le modèle IA, si applicable. */
    @JsonProperty("Code généré")
    private String codeGenere;

    /** Date et heure de l'évaluation. */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime timestamp;

    /**
     * Constructeur par défaut.
     * Initialise automatiquement le timestamp à la date et l'heure courante.
     */
    public EvaluationResult() {
        this.timestamp = LocalDateTime.now();
    }

    /**
     * Calcule automatiquement la moyenne globale sur les cinq critères notés.
     * <p>La valeur est arrondie à deux décimales.</p>
     *
     * <p>Ne calcule la moyenne que si toutes les notes sont renseignées.</p>
     */
    public void calculateMoyenneGlobale() {
        if (stabilite != null && coherence != null && respectSujet != null &&
            richesseStructurelle != null && creativite != null) {

            this.moyenneGlobale = (stabilite + coherence + respectSujet + 
                                  richesseStructurelle + creativite) / 5.0;

            this.moyenneGlobale = Math.round(this.moyenneGlobale * 100.0) / 100.0;
        }
    }

    @Override
    public String toString() {
        return String.format(
            "EvaluationResult{modèle='%s', prompt='%s', temps=%.2fs, " +
            "stabilité=%d, cohérence=%d, respect=%d, richesse=%d, créativité=%d, " +
            "moyenne=%.2f, réalisable=%s}",
            modeleIA, idPrompt, tempsReponseS,
            stabilite, coherence, respectSujet, richesseStructurelle, creativite,
            moyenneGlobale, realisable
        );
    }

    /**
     * Formatte les données sous forme d'une ligne CSV exportable dans Excel.
     *
     * @return une chaîne représentant la ligne CSV
     */
    public String toCsvLine() {
        return String.join(";",
            modeleIA != null ? modeleIA : "",
            idPrompt != null ? idPrompt : "",
            tempsReponseS != null ? String.format("%.2f", tempsReponseS) : "",
            tokensMoyens != null ? tokensMoyens.toString() : "",
            stabilite != null ? stabilite.toString() : "",
            coherence != null ? coherence.toString() : "",
            respectSujet != null ? respectSujet.toString() : "",
            richesseStructurelle != null ? richesseStructurelle.toString() : "",
            creativite != null ? creativite.toString() : "",
            moyenneGlobale != null ? String.format("%.2f", moyenneGlobale) : "",
            realisable != null ? (realisable ? "Oui" : "Non") : "",
            observations != null ? observations.replace(";", ",") : ""
        );
    }

    /**
     * Retourne le nom du modèle IA testé.
     *
     * @return nom du modèle testé
     */
    public String getModeleIA() {
        return modeleIA;
    }

    /**
     * Définit le nom du modèle IA testé.
     *
     * @param modeleIA nom du modèle
     */
    public void setModeleIA(String modeleIA) {
        this.modeleIA = modeleIA;
    }

    /**
     * Retourne l'identifiant du prompt utilisé pour l'évaluation.
     *
     * @return identifiant du prompt
     */
    public String getIdPrompt() {
        return idPrompt;
    }

    /**
     * Définit l'identifiant du prompt évalué.
     *
     * @param idPrompt identifiant du prompt
     */
    public void setIdPrompt(String idPrompt) {
        this.idPrompt = idPrompt;
    }

    /**
     * Retourne le texte du prompt soumis au modèle IA.
     *
     * @return texte du prompt
     */
    public String getPromptTexte() {
        return promptTexte;
    }

    /**
     * Définit le texte du prompt soumis au modèle IA.
     *
     * @param promptTexte texte du prompt
     */
    public void setPromptTexte(String promptTexte) {
        this.promptTexte = promptTexte;
    }

    /**
     * Retourne le temps de réponse du modèle en secondes.
     *
     * @return temps de réponse en secondes
     */
    public Double getTempsReponseS() {
        return tempsReponseS;
    }

    /**
     * Définit le temps de réponse du modèle.
     *
     * @param tempsReponseS temps de réponse en secondes
     */
    public void setTempsReponseS(Double tempsReponseS) {
        this.tempsReponseS = tempsReponseS;
    }

    /**
     * Retourne le nombre moyen de tokens utilisés.
     *
     * @return nombre moyen de tokens
     */
    public Integer getTokensMoyens() {
        return tokensMoyens;
    }

    /**
     * Définit le nombre moyen de tokens utilisés par le modèle.
     *
     * @param tokensMoyens tokens moyens
     */
    public void setTokensMoyens(Integer tokensMoyens) {
        this.tokensMoyens = tokensMoyens;
    }

    /**
     * Retourne la note de stabilité.
     *
     * @return note de stabilité (1 à 5)
     */
    public Integer getStabilite() {
        return stabilite;
    }

    /**
     * Définit la note de stabilité.
     *
     * @param stabilite note de stabilité (1 à 5)
     */
    public void setStabilite(Integer stabilite) {
        this.stabilite = stabilite;
    }

    /**
     * Retourne la note de cohérence.
     *
     * @return note de cohérence (1 à 5)
     */
    public Integer getCoherence() {
        return coherence;
    }

    /**
     * Définit la note de cohérence.
     *
     * @param coherence note de cohérence (1 à 5)
     */
    public void setCoherence(Integer coherence) {
        this.coherence = coherence;
    }

    /**
     * Retourne la note de respect du sujet.
     *
     * @return note de respect du sujet (1 à 5)
     */
    public Integer getRespectSujet() {
        return respectSujet;
    }

    /**
     * Définit la note de respect du sujet.
     *
     * @param respectSujet note de respect du sujet (1 à 5)
     */
    public void setRespectSujet(Integer respectSujet) {
        this.respectSujet = respectSujet;
    }

    /**
     * Retourne la note de richesse structurelle.
     *
     * @return note de richesse structurelle (1 à 5)
     */
    public Integer getRichesseStructurelle() {
        return richesseStructurelle;
    }

    /**
     * Définit la note de richesse structurelle.
     *
     * @param richesseStructurelle note de richesse structurelle (1 à 5)
     */
    public void setRichesseStructurelle(Integer richesseStructurelle) {
        this.richesseStructurelle = richesseStructurelle;
    }

    /**
     * Retourne la note de créativité.
     *
     * @return note de créativité (1 à 5)
     */
    public Integer getCreativite() {
        return creativite;
    }

    /**
     * Définit la note de créativité.
     *
     * @param creativite note de créativité (1 à 5)
     */
    public void setCreativite(Integer creativite) {
        this.creativite = creativite;
    }

    /**
     * Retourne la moyenne globale de l'évaluation.
     *
     * @return moyenne globale
     */
    public Double getMoyenneGlobale() {
        return moyenneGlobale;
    }

    /**
     * Définit la moyenne globale.
     *
     * @param moyenneGlobale valeur de moyenne globale
     */
    public void setMoyenneGlobale(Double moyenneGlobale) {
        this.moyenneGlobale = moyenneGlobale;
    }

    /**
     * Indique si la génération est considérée comme exploitable.
     *
     * @return {@code true} si exploitable, sinon {@code false}
     */
    public Boolean isRealisable() {
        return realisable;
    }

    /**
     * Définit si la génération est exploitable dans un contexte réel.
     *
     * @param realisable valeur booléenne
     */
    public void setRealisable(Boolean realisable) {
        this.realisable = realisable;
    }

    /**
     * Retourne les observations associées à l'évaluation.
     *
     * @return observations
     */
    public String getObservations() {
        return observations;
    }

    /**
     * Définit les observations associées à l'évaluation.
     *
     * @param observations remarques
     */
    public void setObservations(String observations) {
        this.observations = observations;
    }

    /**
     * Retourne le code généré par le modèle IA.
     *
     * @return code généré
     */
    public String getCodeGenere() {
        return codeGenere;
    }

    /**
     * Définit le code généré par le modèle IA.
     *
     * @param codeGenere code généré
     */
    public void setCodeGenere(String codeGenere) {
        this.codeGenere = codeGenere;
    }

    /**
     * Retourne le timestamp de l'évaluation.
     *
     * @return date et heure de l'évaluation
     */
    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    /**
     * Définit le timestamp de l'évaluation.
     *
     * @param timestamp date et heure
     */
    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }
}