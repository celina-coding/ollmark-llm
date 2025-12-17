package com.example.aiPoc.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration des templates de prompts système pour la génération de code.
 * 
 * <p>Cette classe charge les templates statiques depuis le fichier de configuration
 * (application.yml ou .env) tout en permettant l'injection de contenu dynamique
 * (apiSummary, ollcaContext, userPrompt).</p>
 */
@Configuration
@ConfigurationProperties(prefix = "prompt.templates")
public class PromptTemplateConfig {

    /** Section système du prompt décrivant le rôle de l'IA. */
    private String systemSection;
    
    /** Avertissements critiques sur les méthodes interdites. */
    private String criticalWarnings;
    
    /** Liste des méthodes autorisées pour les images. */
    private String allowedImageMethods;
    
    /** Instructions pour le format de sortie. */
    private String outputFormat;
    
    /** Template pour les prompts de correction de code. */
    private String correctionTemplate;
    
    /** Instructions strictes pour la correction. */
    private String correctionInstructions;

    // Constructeur par défaut
    public PromptTemplateConfig() {
    }

    // Getters et Setters
    
    public String getSystemSection() {
        return systemSection;
    }

    public void setSystemSection(String systemSection) {
        this.systemSection = systemSection;
    }

    public String getCriticalWarnings() {
        return criticalWarnings;
    }

    public void setCriticalWarnings(String criticalWarnings) {
        this.criticalWarnings = criticalWarnings;
    }

    public String getAllowedImageMethods() {
        return allowedImageMethods;
    }

    public void setAllowedImageMethods(String allowedImageMethods) {
        this.allowedImageMethods = allowedImageMethods;
    }

    public String getOutputFormat() {
        return outputFormat;
    }

    public void setOutputFormat(String outputFormat) {
        this.outputFormat = outputFormat;
    }

    public String getCorrectionTemplate() {
        return correctionTemplate;
    }

    public void setCorrectionTemplate(String correctionTemplate) {
        this.correctionTemplate = correctionTemplate;
    }

    public String getCorrectionInstructions() {
        return correctionInstructions;
    }

    public void setCorrectionInstructions(String correctionInstructions) {
        this.correctionInstructions = correctionInstructions;
    }

    /**
     * Construit le template complet de création en injectant les parties dynamiques.
     * 
     * @param apiSummary résumé de l'API généré dynamiquement
     * @param ollcaContext contexte OLLCA extrait dynamiquement
     * @param userPrompt demande de l'utilisateur
     * @return le prompt complet formaté
     */
    public String buildCreationPrompt(String apiSummary, String ollcaContext, String userPrompt) {
        return String.format("""
            %s

            %s

            %s

            [API DISPONIBLE]
            %s

            [OLLCA CONTEXTE SUPPLÉMENTAIRE]
            %s

            [TÂCHE]
            %s

            %s
            """,
            systemSection,
            criticalWarnings,
            allowedImageMethods,
            apiSummary,
            ollcaContext,
            userPrompt,
            outputFormat
        );
    }

    /**
     * Construit le prompt de correction en injectant les parties dynamiques.
     * 
     * @param originalPrompt demande originale de l'utilisateur
     * @param failedCode code qui a échoué
     * @param errorMessage message d'erreur
     * @return le prompt de correction formaté
     */
    public String buildCorrectionPrompt(String originalPrompt, String failedCode, String errorMessage) {
        return String.format(
            correctionTemplate,
            originalPrompt,
            failedCode,
            errorMessage,
            correctionInstructions
        );
    }
}