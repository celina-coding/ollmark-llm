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

    private final OllcaEntityExtractorService ollcaExtractorService;

    /**
     * Constructeur du service de génération de prompts.
     *
     * @param documentationAggregator service d’agrégation de documentation du SDK Penpot
     */
    public PromptBuilderService(DocumentationAggregatorService documentationAggregator, OllcaEntityExtractorService ollcaExtractorService) {
        this.documentationAggregator = documentationAggregator;
        this.ollcaExtractorService = ollcaExtractorService;
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
        String ollcaContext = ollcaExtractorService.extractAndFormatData(userPrompt);

        return String.format("""
            [SYSTÈME]
            Tu es un générateur de code Penpot expert.
            TU DOIS RESPECTER STRICTEMENT la documentation fournie.

            [AVERTISSEMENT CRITIQUE]
            - createImageFromUrl() N'EXISTE PAS dans l'API Penpot
            - createImageFromData() N'EXISTE PAS dans l'API Penpot  
            - setImageFill() N'EXISTE PAS dans l'API Penpot
            - createImagePlaceholder() N'EXISTE PAS dans l'API Penpot

            [MÉTHODES IMAGES AUTORISÉES UNIQUEMENT]
            - uploadMediaUrl(name, url) : upload une image depuis une URL
            - uploadMediaData(name, data, mimeType) : upload depuis données binaires
        

            [API DISPONIBLE]
            %s

            [OLLCA CONTEXTE SUPPLÉMENTAIRE]
            %s

            [TÂCHE]
            %s

            [FORMAT DE SORTIE]
            - Code JavaScript PUR uniquement
            - PAS de commentaires
            - DIRECTEMENT exécutable dans Penpot
            """,
            apiSummary,
            ollcaContext,
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
            Le code suivant a généré une erreur. Corrige-le en utilisant UNIQUEMENT les méthodes OFFICIELLES de l'API Penpot.

            Demande originale: %s

            Code généré (avec erreur):
            %s

            Erreur: %s

            [INSTRUCTIONS DE CORRECTION STRICTES]
            - MÉTHODES AUTORISÉES UNIQUEMENT: uploadMediaUrl(), uploadMediaData()
            - SYNTAXE EXACTE OBLIGATOIRE: rect.fills = [{fillImage: imageData}];
            - INTERDICTION: createImageFromUrl, createImageFromData, setImageFill, createImagePlaceholder
            - Vérifier que les méthodes async utilisent await
            - Utiliser fillImage: PAS image:

            Génère une version corrigée du code, sans explication.
            """,
            originalPrompt,
            failedCode,
            errorMessage
        );
    }
}