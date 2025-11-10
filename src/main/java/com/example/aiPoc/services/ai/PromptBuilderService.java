package com.example.aiPoc.services.ai;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.example.aiPoc.models.PromptStrategy;
import com.example.aiPoc.services.penpot.PenpotSdkService;

/**
 * <p>
 * Service responsable de la construction de prompts optimisés pour la génération de code via l’IA.
 * </p>
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
 * @see PenpotSdkService
 * @see com.example.aiPoc.services.ai.AIService
 */
@Service
public class PromptBuilderService {

    /** Logger pour le suivi des opérations et le diagnostic. */
    private static final Logger logger = LoggerFactory.getLogger(PromptBuilderService.class);

    /** Service d’accès au SDK Penpot pour obtenir le contexte API et les exemples de code. */
    private final PenpotSdkService penpotSdkService;

    /**
     * Initialise le service de construction de prompts avec le SDK Penpot.
     *
     * @param penpotSdkService le service fournissant les données du SDK Penpot (API, exemples, résumé, etc.)
     */
    public PromptBuilderService(PenpotSdkService penpotSdkService) {
        this.penpotSdkService = penpotSdkService;
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
     * <p>
     * Construit un prompt basique sans ajout de contexte ni de documentation.
     * </p>
     * <p>
     * Stratégie BASIC: Simple et directe.
     * </p>
     *
     * @param userPrompt la demande utilisateur
     * @return un prompt minimaliste contenant uniquement la consigne et la tâche
     */
    private String buildBasicPrompt(String userPrompt) {
        return String.format(
            "Génère du code JavaScript pour Penpot : %s\n\n" +
            "Réponds uniquement avec du code, sans explication.",
            userPrompt
        );
    }

    /**
     * Construit un prompt détaillé en incluant la documentation complète du SDK Penpot.
     * <p>
     * Stratégie DETAILED: Contexte riche avec documentation.
     * </p>
     *
     * @param userPrompt la demande de l’utilisateur
     * @return un prompt détaillé intégrant la documentation Penpot et des consignes de génération
     */
    private String buildDetailedPrompt(String userPrompt) {
        String apiSummary = penpotSdkService.getApiSummary();

        return String.format("""
            Tu es un assistant spécialisé en génération de code pour le SDK Penpot.

            %s

            Propriétés des objets Shape:
            - x: number (position X)
            - y: number (position Y)
            - width: number (largeur)
            - height: number (hauteur)
            - fills: Array<{color: string}> (couleurs de remplissage)
            - strokes: Array<{color: string, width: number}> (bordures)

            Tâche: %s

            Exigences de sortie:
            - Code JavaScript pur, sans commentaires excessifs
            - Pas de balises markdown (```javascript)
            - Directement exécutable dans un plugin Penpot
            - Gestion d'erreurs si nécessaire
            - Utilise des noms de variables descriptifs

            Code:
            """,
            apiSummary,
            userPrompt
        );
    }

    /**
     * Construit un prompt intégrant des exemples de code (approche few-shot learning).
     * <p>
     * Stratégie WITH_EXAMPLES: Apprentissage par exemples.
     * </p>
     *
     * @param userPrompt la consigne utilisateur
     * @return un prompt enrichi d’exemples de code et d’instructions précises
     */
    private String buildPromptWithExamples(String userPrompt) {
        List<String> examples = penpotSdkService.getCodeExamples();

        StringBuilder examplesText = new StringBuilder();
        for (int i = 0; i < Math.min(3, examples.size()); i++) {
            examplesText.append(String.format(
                "Exemple %d:\n%s\n\n",
                i + 1,
                examples.get(i)
            ));
        }

        return String.format("""
            Tu es un expert Penpot. Voici des exemples de code valide:

            %s

            Maintenant, génère du code pour: %s

            Important:
            - Utilise le même style que les exemples
            - Code uniquement, sans explication
            - Pas de markdown
            """,
            examplesText.toString(),
            userPrompt
        );
    }

    /**
     * Construit un prompt structuré, organisé en sections thématiques distinctes.
     * <p>
     * Stratégie STRUCTURED: Organisation claire et formatée.
     * </p>
     *
     * @param userPrompt la consigne utilisateur
     * @return un prompt structuré avec des balises de contexte et de sortie
     */
    private String buildStructuredPrompt(String userPrompt) {
        String compactApi = penpotSdkService.getCompactApiSummary();

        return String.format("""
            [SYSTÈME]
            Tu es un générateur de code Penpot expert.

            [API DISPONIBLE]
            %s

            [RÈGLES]
            1. Code JavaScript uniquement
            2. Pas de markdown ni commentaires
            3. Syntaxe ES6+ moderne
            4. Variables avec const/let
            5. Code fonctionnel et testé

            [TÂCHE]
            %s

            [FORMAT DE SORTIE]
            Code JavaScript uniquement, prêt à l'exécution.

            [CODE]
            """,
            compactApi,
            userPrompt
        );
    }

    /**
     * <p>
     * Construit un prompt destiné à corriger du code erroné généré par l’IA.
     * </p>
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

    /**
     * Estime la longueur totale du prompt généré (en nombre de caractères).
     *
     * @param userPrompt la demande utilisateur
     * @param strategy   la stratégie de construction utilisée
     * @return la longueur estimée du prompt en caractères
     */
    public int estimatePromptLength(String userPrompt, PromptStrategy strategy) {
        return buildPrompt(userPrompt, strategy).length();
    }

    /**
     * <p>
     * Détermine automatiquement la stratégie de prompt la plus adaptée
     * en fonction de la complexité de la demande utilisateur.
     * </p>
     *
     * <p>
     * La complexité est estimée sur la base du nombre de mots :
     * <ul>
     *   <li>< 5 mots → {@link PromptStrategy#WITH_EXAMPLES}</li>
     *   <li>5 à 15 mots → {@link PromptStrategy#DETAILED}</li>
     *   <li>> 15 mots → {@link PromptStrategy#STRUCTURED}</li>
     * </ul>
     * </p>
     *
     * @param userPrompt la requête textuelle de l’utilisateur
     * @return la stratégie recommandée selon la longueur et la complexité du prompt
     */
    public PromptStrategy recommendStrategy(String userPrompt) {
        int wordCount = userPrompt.split("\\s+").length;

        if (wordCount < 5) {
            return PromptStrategy.WITH_EXAMPLES;
        }

        if (wordCount < 15) {
            return PromptStrategy.DETAILED;
        }

        return PromptStrategy.STRUCTURED;
    }
}