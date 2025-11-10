package com.example.aiPoc.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration des propriétés du SDK Penpot.
 *
 * <p>Cette classe charge automatiquement les propriétés définies 
 * dans le fichier <code>application.properties</code> sous le préfixe 
 * <code>penpot.sdk</code>.</p>
 *
 * <p>Elle centralise les chemins d'accès aux fichiers de ressources 
 * (templates, exemples de code, résumés d'API) ainsi que les options 
 * de validation et de nettoyage du code généré.</p>
 */
@Configuration
@ConfigurationProperties(prefix = "penpot.sdk")
public class PenpotSdkConfig {

    /** Chemin vers le fichier de résumé de l’API Penpot (format JSON). */
    private String apiSummaryPath = "classpath:templates/penpot-api-summary.json";

    /** Chemin vers le fichier contenant les modèles de prompts. */
    private String promptTemplatesPath = "classpath:templates/prompt-templates.json";

    /** Chemin vers le fichier d’exemples de code pour Penpot. */
    private String codeExamplesPath = "classpath:templates/code-examples.json";

    /** Indique si la validation du code généré est activée. */
    private boolean enableValidation = true;

    /** Indique si le nettoyage automatique du code généré est activé. */
    private boolean enableCodeCleaning = true;

    /**
     * Retourne le chemin du fichier de résumé de l’API Penpot.
     *
     * @return le chemin du fichier JSON contenant le résumé de l’API
     */
    public String getApiSummaryPath() {
        return apiSummaryPath;
    }

    /**
     * Définit le chemin du fichier de résumé de l’API Penpot.
     *
     * @param apiSummaryPath chemin du fichier JSON de résumé de l’API
     */
    public void setApiSummaryPath(String apiSummaryPath) {
        this.apiSummaryPath = apiSummaryPath;
    }

    /**
     * Retourne le chemin du fichier des modèles de prompts.
     *
     * @return le chemin du fichier JSON des modèles de prompts
     */
    public String getPromptTemplatesPath() {
        return promptTemplatesPath;
    }

    /**
     * Définit le chemin du fichier des modèles de prompts.
     *
     * @param promptTemplatesPath chemin du fichier JSON des modèles de prompts
     */
    public void setPromptTemplatesPath(String promptTemplatesPath) {
        this.promptTemplatesPath = promptTemplatesPath;
    }

    /**
     * Retourne le chemin du fichier contenant les exemples de code Penpot.
     *
     * @return le chemin du fichier JSON des exemples de code
     */
    public String getCodeExamplesPath() {
        return codeExamplesPath;
    }

    /**
     * Définit le chemin du fichier contenant les exemples de code Penpot.
     *
     * @param codeExamplesPath chemin du fichier JSON des exemples de code
     */
    public void setCodeExamplesPath(String codeExamplesPath) {
        this.codeExamplesPath = codeExamplesPath;
    }

    /**
     * Indique si la validation du code généré est activée.
     *
     * @return {@code true} si la validation est activée, sinon {@code false}
     */
    public boolean isEnableValidation() {
        return enableValidation;
    }

    /**
     * Active ou désactive la validation du code généré.
     *
     * @param enableValidation valeur booléenne pour activer/désactiver la validation
     */
    public void setEnableValidation(boolean enableValidation) {
        this.enableValidation = enableValidation;
    }

    /**
     * Indique si le nettoyage automatique du code généré est activé.
     *
     * @return {@code true} si le nettoyage est activé, sinon {@code false}
     */
    public boolean isEnableCodeCleaning() {
        return enableCodeCleaning;
    }

    /**
     * Active ou désactive le nettoyage automatique du code généré.
     *
     * @param enableCodeCleaning valeur booléenne pour activer/désactiver le nettoyage
     */
    public void setEnableCodeCleaning(boolean enableCodeCleaning) {
        this.enableCodeCleaning = enableCodeCleaning;
    }
}