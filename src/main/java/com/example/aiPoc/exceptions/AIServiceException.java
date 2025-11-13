package com.example.aiPoc.exceptions;

/**
 * Exception spécifique utilisée pour signaler une erreur survenue
 * lors de l'interaction avec un service d'intelligence artificielle (IA).
 *
 * <p>Cette exception permet d'identifier le fournisseur et le modèle IA
 * à l'origine du problème afin de faciliter le diagnostic et la traçabilité
 * des erreurs dans les opérations de génération ou d'analyse automatisées.</p>
 *
 * @see RuntimeException
 */
public class AIServiceException extends RuntimeException {

    /** Nom du fournisseur IA ayant généré l'erreur. */
    private String provider;

    /** Nom du modèle IA concerné par l'erreur. */
    private String modelName;

    /**
     * Construit une nouvelle exception avec uniquement un message descriptif.
     *
     * @param message message décrivant la cause de l’erreur
     */
    public AIServiceException(String message) {
        super(message);
    }

    /**
     * Construit une nouvelle exception avec un message et une cause sous-jacente.
     *
     * @param message message décrivant la cause de l’erreur
     * @param cause   exception d'origine à l'origine de cette erreur
     */
    public AIServiceException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Construit une nouvelle exception en spécifiant le fournisseur et le modèle IA.
     *
     * @param message   message décrivant la cause de l’erreur
     * @param provider  nom du fournisseur IA concerné (ex. : "OpenAI")
     * @param modelName nom du modèle IA concerné (ex. : "gpt-5")
     */
    public AIServiceException(String message, String provider, String modelName) {
        super(message);
        this.provider = provider;
        this.modelName = modelName;
    }

    /**
     * Retourne le nom du fournisseur IA concerné par l'erreur.
     *
     * @return le nom du fournisseur IA, ou {@code null} si non défini
     */
    public String getProvider() {
        return provider;
    }

    /**
     * Définit le nom du fournisseur IA ayant généré l'erreur.
     *
     * @param provider nom du fournisseur IA (ex. : "OpenAI", "Anthropic")
     */
    public void setProvider(String provider) {
        this.provider = provider;
    }

    /**
     * Retourne le nom du modèle IA concerné par l'erreur.
     *
     * @return le nom du modèle IA, ou {@code null} si non défini
     */
    public String getModelName() {
        return modelName;
    }

    /**
     * Définit le nom du modèle IA concerné par l'erreur.
     *
     * @param modelName nom du modèle IA (ex. : "gpt-5", "claude-3-opus")
     */
    public void setModelName(String modelName) {
        this.modelName = modelName;
    }

    @Override
    public String toString() {
        return "AIServiceException{" +
                "message='" + getMessage() + '\'' +
                ", provider='" + provider + '\'' +
                ", modelName='" + modelName + '\'' +
                '}';
    }
}