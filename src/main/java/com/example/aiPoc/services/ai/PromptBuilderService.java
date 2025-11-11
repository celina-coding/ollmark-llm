package com.example.aiPoc.services.ai;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.example.aiPoc.models.PromptStrategy;
import com.example.aiPoc.services.penpot.DocumentationAggregatorService;

/**
 * Service responsable de la construction de prompts optimisés pour la génération de code via l’IA.
 *
 * <p>
 * Ce service applique différentes stratégies de construction de prompts selon le besoin et
 * le niveau de complexité de la demande utilisateur.  
 * Il intègre également des informations contextuelles issues du SDK Penpot afin d’améliorer
 * la qualité et la pertinence des réponses produites par le modèle d’intelligence artificielle.
 * </p>
 *
 * <p><b>Principales responsabilités :</b></p>
 * <ul>
 *   <li>Construire des prompts selon plusieurs stratégies prédéfinies ({@link PromptStrategy}).</li>
 *   <li>Injecter automatiquement les éléments de contexte du SDK Penpot (documentation, exemples, API, etc.).</li>
 *   <li>Optimiser le format, la structure et la cohérence des prompts pour le modèle IA cible.</li>
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
            case BASIC -> buildBasicPrompt(userPrompt);
            case DETAILED -> buildDetailedPrompt(userPrompt);
            case WITH_EXAMPLES -> buildPromptWithExamples(userPrompt);
            case STRUCTURED -> buildStructuredPrompt(userPrompt);
        };
    }

    /**
     * Construit un prompt à partir du nom d’une stratégie.
     *
     * @param userPrompt   le texte fourni par l’utilisateur
     * @param strategyName le nom de la stratégie à utiliser (ex. "basic", "detailed", "structured")
     * @return le prompt généré selon la stratégie correspondante
     */
    public String buildPrompt(String userPrompt, String strategyName) {
        PromptStrategy strategy = PromptStrategy.fromValue(strategyName);
        return buildPrompt(userPrompt, strategy);
    }

    /**
     * Construit un prompt basique, sans ajout de contexte ni de documentation.
     *
     * <p><b>Stratégie :</b> {@link PromptStrategy#BASIC}</p>
     *
     * @param userPrompt la consigne utilisateur
     * @return un prompt minimal contenant uniquement la demande et l’instruction de génération
     */
    private String buildBasicPrompt(String userPrompt) {
        String minimalApi = documentationAggregator.generateMinimalSummary(userPrompt);

        return String.format(
            "Génère du code JavaScript pour Penpot : %s\n\n" +
            "API disponible:\n%s\n\n" +
            "Réponds uniquement avec du code, sans explication.",
            userPrompt,
            minimalApi
        );
    }

    /**
     * Construit un prompt détaillé intégrant la documentation complète du SDK Penpot.
     *
     * <p><b>Stratégie :</b> {@link PromptStrategy#DETAILED}</p>
     *
     * @param userPrompt la demande utilisateur
     * @return un prompt enrichi de documentation et d’instructions détaillées
     */
    private String buildDetailedPrompt(String userPrompt) {
        String optimizedApiSummary = documentationAggregator.generateOptimizedSummary(userPrompt);

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
            optimizedApiSummary,
            userPrompt
        );
    }

    /**
     * Construit un prompt enrichi d’exemples de code (apprentissage par démonstration).
     *
     * <p><b>Stratégie :</b> {@link PromptStrategy#WITH_EXAMPLES}</p>
     *
     * @param userPrompt la consigne utilisateur
     * @return un prompt contenant plusieurs exemples de code pertinents
     */
    private String buildPromptWithExamples(String userPrompt) {
        String exampleRichSummary = documentationAggregator.generateExampleRichSummary(userPrompt);

        return String.format("""
            Tu es un expert Penpot. Voici des exemples de code valide:

            %s

            Maintenant, génère du code pour: %s

            Important:
            - Code uniquement, sans explication
            - Pas de markdown
            """,
            exampleRichSummary,
            userPrompt
        );
    }

    /**
     * Construit un prompt structuré en plusieurs sections thématiques.
     *
     * <p><b>Stratégie :</b> {@link PromptStrategy#STRUCTURED}</p>
     *
     * @param userPrompt la demande utilisateur
     * @return un prompt structuré et formaté selon un modèle prédéfini
     */
    private String buildStructuredPrompt(String userPrompt) {
        String compactApi = documentationAggregator.generateMinimalSummary(userPrompt);

        return String.format("""
            [SYSTÈME]
            Tu es un générateur de code Penpot expert.

            [API DISPONIBLE]
            %s

            [RÈGLES]
            1. Code JavaScript fonctionnel uniquement
            2. Pas de markdown ni commentaires

            [TÂCHE]
            %s

            [FORMAT DE SORTIE]
            Uniquement et strictement du code JavaScript, prêt à l'exécution.

            [CODE]
            """,
            compactApi,
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