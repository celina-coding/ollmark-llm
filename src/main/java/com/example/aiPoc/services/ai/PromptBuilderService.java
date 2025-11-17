package com.example.aiPoc.services.ai;

import org.slf4j.*;
import org.springframework.stereotype.Service;

import com.example.aiPoc.models.PromptStrategy;
import com.example.aiPoc.services.penpot.DocumentationAggregatorService;

/**
 * Service responsable de la construction de prompts optimisés pour la génération de code via l’IA.
 *
 * <p>
 * Ce service applique différentes stratégies de construction de prompts selon le besoin de l'utilisateur.  
 * Il intègre également des informations contextuelles issues du SDK Penpot afin d’améliorer
 * la qualité et la pertinence des réponses produites par le modèle d’intelligence artificielle.
 * </p>
 *
 * <p><b>Principales responsabilités :</b></p>
 * <ul>
 *   <li>Construire des prompts selon plusieurs stratégies prédéfinies ({@link PromptStrategy}).</li>
 *   <li>Injecter automatiquement les éléments de contexte du SDK Penpot (documentation, exemples, API, etc.).</li>
 * </ul>
 *
 * @see PromptStrategy
 * @see com.example.aiPoc.services.ai.AIService
 */
@Service
public class PromptBuilderService {

    /** Logger pour le suivi des opérations et le diagnostic. */
    private static final Logger logger = LoggerFactory.getLogger(PromptBuilderService.class);

    /** Service d’agrégation de documentation pour enrichir le contexte des prompts. */
    private final DocumentationAggregatorService documentationAggregator;

    /**
     * Constructeur du service de génération de prompts.
     *
     * @param documentationAggregator service d’agrégation de documentation du SDK Penpot
     */
    public PromptBuilderService(DocumentationAggregatorService documentationAggregator) {
        this.documentationAggregator = documentationAggregator;
    }

    /**
     * Construit un prompt complet selon la stratégie spécifiée.
     *
     * @param userPrompt le texte fourni par l’utilisateur
     * @param strategy   la stratégie de construction de prompt à appliquer
     * @return le prompt complet généré selon la stratégie choisie
     */
    public String buildPrompt(String userPrompt, PromptStrategy strategy) {
        logger.debug("Construction du prompt avec stratégie: {}", strategy);

        return switch (strategy) {
            case CREATION -> buildCreationPrompt(userPrompt);
        };
    }

    /**
     * Construit un prompt dans le cas d'une création de composant dans Penpot.
     *
     * <p><b>Stratégie :</b> {@link PromptStrategy#CREATION}</p>
     *
     * @param userPrompt la demande utilisateur
     * @return un prompt enrichi de documentation et d’instructions détaillées
     */
    private String buildCreationPrompt(String userPrompt) {
        String apiSummary = documentationAggregator.generateSummary(userPrompt);

        return String.format("""
            [SYSTÈME]
            Tu es un générateur de code Penpot expert.
            Tu ne dois te fier qu'à la documentation de l'API donnée ci-dessous.
            Tu as l'interdiction d'utiliser ne serait-ce qu'un attribut qui n'est pas mentionné ici.

            [API DISPONIBLE]
            %s

            [TÂCHE]
            %s

            [FORMAT DE SORTIE]
            - Code JavaScript uniquement pas le droit au texte
            - Tu as l'interdiction de commenter le code
            - Tu as l'interdiction d'argumenter tes choix
            - Pas de balises markdown (```javascript)
            - Directement exécutable dans un plugin Penpot
            """,
            apiSummary,
            userPrompt
        );
    }

    /**
     * Construit un prompt destiné à corriger du code erroné généré par l’IA.
     *
     * <p>
     * Ce prompt fournit le code initial, le message d’erreur et la demande originale,
     * afin de permettre au modèle de générer une version corrigée.
     * </p>
     *
     * @param originalPrompt la demande initiale de l’utilisateur
     * @param failedCode     le code produit par l’IA qui a échoué
     * @param errorMessage   le message d’erreur rencontré
     * @return un prompt demandant la correction du code sans explications textuelles
     */
    public String buildCorrectionPrompt(String originalPrompt, String failedCode, String errorMessage) {
        return String.format("""
            Le code suivant a généré une erreur. Corrige-le.

            Demande originale: %s

            Code généré (avec erreur):
            %s

            Erreur: %s

            Génère une version corrigée du code, sans explication.
            """,
            originalPrompt,
            failedCode,
            errorMessage
        );
    }
}