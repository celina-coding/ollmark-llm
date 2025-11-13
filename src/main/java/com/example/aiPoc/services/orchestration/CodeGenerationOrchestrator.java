package com.example.aiPoc.services.orchestration;

import java.util.List;

import org.slf4j.*;
import org.springframework.stereotype.Service;

import com.example.aiPoc.dto.request.CodeGenerationRequest;
import com.example.aiPoc.dto.response.CodeGenerationResponse;
import com.example.aiPoc.models.*;
import com.example.aiPoc.services.ai.*;
import com.example.aiPoc.services.penpot.*;

/**
 * Service d'orchestration principal pour la génération de code assistée par intelligence artificielle.
 * <p>
 * Cette classe agit comme un point d’entrée unique pour coordonner les différentes étapes du processus de génération,
 * incluant la préparation du prompt, l’appel au modèle IA, le nettoyage et la validation du code obtenu.
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

    /** Logger principal du service pour le suivi des opérations. */
    private static final Logger logger = LoggerFactory.getLogger(CodeGenerationOrchestrator.class);

    /** Service de communication avec le modèle d’intelligence artificielle. */
    private final AIService aiService;

    /** Service responsable de la construction des prompts enrichis à partir des entrées utilisateur. */
    private final PromptBuilderService promptBuilderService;

    /** Service de sélection et d’enregistrement des stratégies de génération de prompts. */
    private final PromptStrategyService promptStrategyService;

    /** Service chargé du nettoyage syntaxique et stylistique du code généré. */
    private final CodeCleanerService codeCleanerService;

    /** Service de validation syntaxique et logique du code produit. */
    private final CodeValidationService codeValidationService;

    /**
     * Constructeur principal du service d'orchestration.
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
     * Génère du code à partir d'une requête complète.
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
        logger.debug("Début de génération de code: {}", request);

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
            logger.info("Appel au modèle IA...");
            String rawResponse = aiService.chat(enrichedPrompt, 10000, 0.5);

            // 4. Validation du code avec mesure de temps
            if (rawResponse == null || rawResponse.trim().isEmpty()) {
                logger.error("L'IA a retourné une réponse vide !");
                response.setRawResponse("");
                response.setGeneratedCode("");
                response.setValid(false);
                response.addValidationError(new ValidationError(
                    "EMPTY_AI_RESPONSE",
                    "Le modèle IA n'a retourné aucun contenu. Vérifiez la connexion au modèle."
                ));

                long duration = System.currentTimeMillis() - startTime;
                response.setGenerationTimeMs(duration);
                return response;
            }

            response.setRawResponse(rawResponse);
            logger.info("Réponse IA reçue: {} caractères", rawResponse.length());

            // 5. Nettoyage du code
            String cleanedCode = rawResponse;
            if (request.isCleanCode()) {
                logger.info("Nettoyage du code...");
                cleanedCode = codeCleanerService.clean(rawResponse);

                if (cleanedCode == null || cleanedCode.trim().isEmpty()) {
                    logger.warn("Le nettoyage a produit un code vide ! Code brut: {} chars", 
                               rawResponse.length());
                    cleanedCode = rawResponse.trim();
                }

                logger.info("Code nettoyé: {} caractères (avant: {})", 
                           cleanedCode.length(), rawResponse.length());
            }

            response.setGeneratedCode(cleanedCode);

            // Vérification du code nettoyé
            if (cleanedCode.trim().isEmpty()) {
                logger.error("Le code final est vide après nettoyage !");
                response.setValid(false);
                response.addValidationError(new ValidationError(
                    "EMPTY_CODE_AFTER_CLEANING",
                    "Le code est vide après nettoyage. Réponse brute: " + 
                    (rawResponse.length() > 100 ? rawResponse.substring(0, 100) + "..." : rawResponse)
                ));
            }

            // 5. Validation du code
            if (request.isIncludeValidation() && !cleanedCode.trim().isEmpty()) {
                logger.info("Validation du code...");
                List<ValidationError> errors = codeValidationService.validate(cleanedCode);
                response.setValidationErrors(errors);
                response.setValid(errors.isEmpty() || 
                                 errors.stream().allMatch(e -> "WARNING".equals(e.getSeverity())));

                if (errors.isEmpty()) {
                    logger.info("Validation réussie - Aucune erreur");
                } else {
                    long errorCount = errors.stream()
                        .filter(e -> "ERROR".equals(e.getSeverity()))
                        .count();
                    long warningCount = errors.stream()
                        .filter(e -> "WARNING".equals(e.getSeverity()))
                        .count();

                    logger.warn("Validation terminée: {} erreur(s), {} warning(s)", 
                               errorCount, warningCount);

                    errors.forEach(err -> 
                        logger.debug("  - [{}] {}: {}", err.getSeverity(), err.getType(), err.getMessage())
                    );
                }
            }

            // 6. Métriques finales
            long totalDuration = System.currentTimeMillis() - startTime;
            response.setGenerationTimeMs(totalDuration);

            // 7. Enregistrement des métriques de stratégie
            promptStrategyService.recordResult(
                strategy,
                response.isValid(),
                totalDuration,
                cleanedCode.length()
            );

            logger.info("Génération terminée en {}ms: valid={}, length={} caractères", 
                       totalDuration, response.isValid(), cleanedCode.length());

            return response;
        } catch (Exception e) {
            logger.error("Erreur critique lors de la génération de code", e);

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
}