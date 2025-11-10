package com.example.aiPoc.services.orchestration;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.example.aiPoc.dto.request.CodeGenerationRequest;
import com.example.aiPoc.dto.response.CodeGenerationResponse;
import com.example.aiPoc.models.GenerationResult;
import com.example.aiPoc.models.PromptStrategy;
import com.example.aiPoc.models.ValidationError;
import com.example.aiPoc.services.ai.AIService;
import com.example.aiPoc.services.ai.PromptBuilderService;
import com.example.aiPoc.services.ai.PromptStrategyService;
import com.example.aiPoc.services.penpot.CodeCleanerService;
import com.example.aiPoc.services.penpot.CodeValidationService;

/**
 * Service d’orchestration principal pour la génération de code assistée par IA.
 * <p>
 * Cette classe coordonne les différents services impliqués dans le processus de génération :
 * <ul>
 *   <li>Analyse et sélection de la stratégie de génération</li>
 *   <li>Construction du prompt enrichi</li>
 *   <li>Appel au modèle IA pour produire le code</li>
 *   <li>Nettoyage et validation du code généré</li>
 *   <li>Suivi et enregistrement des métriques de performance</li>
 * </ul>
 * Elle constitue le point d’entrée principal pour les opérations de génération
 * et d’évaluation de code au sein du système.
 * </p>
 * 
 * @see AIService
 * @see PromptBuilderService
 * @see PromptStrategyService
 * @see CodeCleanerService
 * @see CodeValidationService
 */
@Service
public class CodeGenerationOrchestrator {

    private static final Logger logger = LoggerFactory.getLogger(CodeGenerationOrchestrator.class);

    private final AIService aiService;
    private final PromptBuilderService promptBuilderService;
    private final PromptStrategyService promptStrategyService;
    private final CodeCleanerService codeCleanerService;
    private final CodeValidationService codeValidationService;

    /**
     * Constructeur principal du service d’orchestration.
     *
     * @param aiService              service de communication avec le modèle IA
     * @param promptBuilderService   service responsable de la construction des prompts enrichis
     * @param promptStrategyService  service de gestion et sélection des stratégies de prompts
     * @param codeCleanerService     service de nettoyage du code généré
     * @param codeValidationService  service de validation syntaxique et logique du code
     */
    public CodeGenerationOrchestrator(
        AIService aiService,
        PromptBuilderService promptBuilderService,
        PromptStrategyService promptStrategyService,
        CodeCleanerService codeCleanerService,
        CodeValidationService codeValidationService
    ) {
        this.aiService = aiService;
        this.promptBuilderService = promptBuilderService;
        this.promptStrategyService = promptStrategyService;
        this.codeCleanerService = codeCleanerService;
        this.codeValidationService = codeValidationService;
    }

    /**
     * Génère du code à partir d’une requête complète.
     * <p>
     * Cette méthode orchestre tout le processus de génération :
     * sélection de la stratégie, enrichissement du prompt, appel IA,
     * nettoyage, validation et enregistrement des métriques.
     * </p>
     *
     * @param request objet {@link CodeGenerationRequest} contenant le prompt et les options
     * @return une instance {@link CodeGenerationResponse} contenant le code généré, les erreurs et les métriques
     */
    public CodeGenerationResponse generateCode(CodeGenerationRequest request) {
        logger.info("Début de génération de code: {}", request);

        long startTime = System.currentTimeMillis();
        CodeGenerationResponse response = new CodeGenerationResponse();

        try {
            // 1. Détermination de la stratégie
            PromptStrategy strategy = PromptStrategy.fromValue(request.getStrategy());
            response.setStrategy(strategy.getValue());
            response.setUserPrompt(request.getPrompt());

            logger.debug("Stratégie sélectionnée: {}", strategy);

            // 2. Construction du prompt
            String enrichedPrompt = promptBuilderService.buildPrompt(request.getPrompt(), strategy);
            response.setEnrichedPrompt(enrichedPrompt);

            logger.debug("Prompt construit: {} caractères", enrichedPrompt.length());

            // 3. Appel à l'IA
            String rawResponse = aiService.chat(enrichedPrompt);
            response.setRawResponse(rawResponse);

            logger.debug("Réponse IA reçue: {} caractères", rawResponse.length());

            // 4. Nettoyage du code
            String cleanedCode = rawResponse;
            if (request.isCleanCode()) {
                cleanedCode = codeCleanerService.clean(rawResponse);
                logger.debug("Code nettoyé: {} caractères", cleanedCode.length());
            }

            response.setGeneratedCode(cleanedCode);

            // 5. Validation du code
            if (request.isIncludeValidation()) {
                List<ValidationError> errors = codeValidationService.validate(cleanedCode);
                response.setValidationErrors(errors);
                response.setValid(errors.isEmpty());

                logger.debug("Validation terminée: {} erreur(s)", errors.size());

                if (!errors.isEmpty()) {
                    logger.warn("Code généré avec erreurs: {}", errors);
                }
            }

            // 6. Métriques
            long duration = System.currentTimeMillis() - startTime;
            response.setGenerationTimeMs(duration);

            // 7. Enregistrement des métriques de stratégie
            promptStrategyService.recordResult(
                strategy,
                response.isValid(),
                duration,
                cleanedCode.length()
            );

            logger.info("Génération terminée en {}ms: valid={}, length={}", 
                       duration, response.isValid(), cleanedCode.length());
            
            return response;
        } catch (Exception e) {
            logger.error("Erreur lors de la génération de code", e);

            response.setGeneratedCode("");
            response.setValid(false);
            response.addValidationError(new ValidationError(
                "GENERATION_ERROR",
                "Erreur lors de la génération: " + e.getMessage()
            ));

            long duration = System.currentTimeMillis() - startTime;
            response.setGenerationTimeMs(duration);

            return response;
        }
    }

    /**
     * Génère du code avec plusieurs tentatives en cas d’échec.
     * <p>
     * Cette méthode relance le processus de génération jusqu’à {@code maxRetries}
     * fois, en changeant la stratégie entre chaque tentative si nécessaire.
     * </p>
     *
     * @param request    la requête initiale de génération
     * @param maxRetries le nombre maximal de tentatives
     * @return le dernier {@link CodeGenerationResponse} produit
     */
    public CodeGenerationResponse generateCodeWithRetry(CodeGenerationRequest request, int maxRetries) {
        logger.info("Génération avec retry (max: {})", maxRetries);

        CodeGenerationResponse lastResponse = null;

        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            logger.debug("Tentative {}/{}", attempt, maxRetries);

            lastResponse = generateCode(request);

            if (lastResponse.isValid()) {
                logger.info("Succès à la tentative {}", attempt);
                return lastResponse;
            }

            logger.warn("Échec à la tentative {}: {} erreur(s)", 
                       attempt, lastResponse.getValidationErrors().size());

            if (attempt < maxRetries) {
                request.setStrategy(getNextStrategy(request.getStrategy()));
            }
        }

        logger.error("Échec après {} tentatives", maxRetries);
        return lastResponse;
    }

    /**
     * Génère du code avec sélection automatique de la stratégie la plus adaptée.
     *
     * @param userPrompt le prompt utilisateur brut
     * @return un objet {@link CodeGenerationResponse} complet avec code, validation et métriques
     */
    public CodeGenerationResponse generateCodeOptimized(String userPrompt) {
        logger.info("Génération optimisée pour: {}", 
                   userPrompt.substring(0, Math.min(50, userPrompt.length())));

        PromptStrategy strategy = promptStrategyService.selectBestStrategy(userPrompt);

        CodeGenerationRequest request = new CodeGenerationRequest(userPrompt, strategy.getValue());
        request.setCleanCode(true);
        request.setIncludeValidation(true);

        return generateCode(request);
    }

    /**
     * Valide un code existant sans passer par la génération.
     *
     * @param code le code source à valider
     * @return une liste de {@link ValidationError} identifiant les problèmes détectés
     */
    public List<ValidationError> validateCode(String code) {
        logger.debug("Validation de code: {} caractères", code.length());
        return codeValidationService.validate(code);
    }

    /**
     * Nettoie un code existant sans passer par la génération.
     *
     * @param code le code à nettoyer
     * @return le code nettoyé
     */
    public String cleanCode(String code) {
        logger.debug("Nettoyage de code: {} caractères", code.length());
        return codeCleanerService.clean(code);
    }

    /**
     * Détermine la stratégie suivante à utiliser en cas d’échec de génération.
     *
     * @param currentStrategy la stratégie actuellement utilisée
     * @return la stratégie suivante à tester
     */
    private String getNextStrategy(String currentStrategy) {
        return switch (currentStrategy) {
            case "basic" -> "detailed";
            case "detailed" -> "with-examples";
            case "with-examples" -> "structured";
            case "structured" -> "detailed";
            default -> "detailed";
        };
    }

    /**
     * Produit un résultat complet de génération, incluant code brut,
     * code nettoyé, validation et métriques.
     *
     * @param userPrompt le prompt utilisateur initial
     * @param strategy   la stratégie utilisée pour la génération
     * @return un objet {@link GenerationResult} consolidé
     */
    public GenerationResult generateResult(String userPrompt, PromptStrategy strategy) {
        CodeGenerationRequest request = new CodeGenerationRequest(userPrompt, strategy.getValue());
        CodeGenerationResponse response = generateCode(request);

        GenerationResult result = new GenerationResult();
        result.setCode(response.getGeneratedCode());
        result.setRawCode(response.getRawResponse());
        result.setValid(response.isValid());
        result.setErrors(response.getValidationErrors());
        result.setGenerationTimeMs(response.getGenerationTimeMs());
        result.setTokenCount(response.getPromptTokensEstimate());
        result.setStrategy(strategy);

        return result;
    }

    /**
     * Génère une représentation textuelle des statistiques globales de génération.
     *
     * @return une chaîne de caractères contenant les statistiques agrégées
     */
    public String getStatistics() {
        StringBuilder stats = new StringBuilder();
        stats.append("=== Statistiques de Génération ===\n\n");

        promptStrategyService.getAllMetrics().forEach((strategy, metrics) -> {
            stats.append(String.format(
                "%s:\n" +
                "  - Tentatives: %d\n" +
                "  - Succès: %d (%.1f%%)\n" +
                "  - Durée moyenne: %.0fms\n" +
                "  - Longueur moyenne: %.0f caractères\n\n",
                strategy,
                metrics.getTotalAttempts(),
                metrics.getSuccessfulAttempts(),
                metrics.getSuccessRate() * 100,
                metrics.getAverageDurationMs(),
                metrics.getAverageCodeLength()
            ));
        });

        return stats.toString();
    }
}