package com.example.aiPoc.controllers;

import java.util.*;

import org.slf4j.*;
import org.springframework.http.*;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import com.example.aiPoc.dto.request.*;
import com.example.aiPoc.dto.response.*;
import com.example.aiPoc.services.orchestration.CodeGenerationOrchestrator;

/**
 * Controller REST fournissant des endpoints pour la génération,
 * le nettoyage et l'analyse de code Penpot généré par IA.
 *
 * <p>Ce contrôleur propose différents modes de génération :
 * <ul>
 *     <li>Génération standard</li>
 *     <li>Génération optimisée</li>
 *     <li>Tests comparatifs de stratégies de prompts</li>
 *     <li>Génération avec retry automatique</li>
 *     <li>Nettoyage de code généré</li>
 *     <li>Obtention de statistiques du système</li>
 * </ul>
 *
 * @see CodeGenerationOrchestrator
 */
@CrossOrigin(origins = {
    "http://localhost:61873",   // Plugin Penpot production
    "http://localhost:8080",    // Application locale
    "http://localhost:4400",    // Plugin Penpot en développement (Vite)
    "http://127.0.0.1:4400"     // Alternative localhost pour plugin
})
@RestController
@RequestMapping("/api/penpot")
@Validated
public class PenpotCodeController {

    /** Logger pour le suivi des requêtes et la journalisation des erreurs. */
    private static final Logger logger = LoggerFactory.getLogger(PenpotCodeController.class);

    private final CodeGenerationOrchestrator orchestrator;

    /**
     * Construit un nouveau contrôleur pour la génération de code Penpot.
     *
     * @param orchestrator orchestrateur gérant la logique métier de génération de code
     */
    public PenpotCodeController(CodeGenerationOrchestrator orchestrator) {
        this.orchestrator = orchestrator;
    }

    /**
     * Endpoint principal permettant de générer du code Penpot à partir d'un prompt et d'une stratégie.
     *
     * @param request objet contenant le prompt utilisateur et la stratégie de génération
     * @return 200 avec le {@link CodeGenerationResponse} si la génération réussit,  
     *         400 si le prompt est invalide,  
     *         500 en cas d'erreur interne.
     */
    @PostMapping("/generate")
    public ResponseEntity<?> generateCode(@RequestBody CodeGenerationRequest request) {
        String sanitizedPrompt = request.getPrompt().substring(0, Math.min(100, request.getPrompt().length()));
        logger.info("Requête de génération: prompt='{}...', strategy='{}'", 
                   sanitizedPrompt, request.getStrategy());

        try {
            CodeGenerationResponse response = orchestrator.generateCode(request);

            // Retourne 200 si valide, 422 si code invalide mais généré
            if (response.isValid()) {
                return ResponseEntity.ok(response);
            } else {
                return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(response);
            }
        } catch (IllegalArgumentException e) {
            logger.warn("Paramètres invalides: {}", e.getMessage());
            return ResponseEntity.badRequest()
                .body(new ErrorResponse("INVALID_PARAMETERS", e.getMessage()));
                
        } catch (Exception e) {
            logger.error("Erreur inattendue lors de la génération", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse("INTERNAL_ERROR", 
                    "Une erreur interne est survenue. Veuillez réessayer."));
        }
    }

    /**
     * Permet de tester différentes stratégies de prompts et de comparer les résultats
     * pour un même prompt utilisateur.
     *
     * @param request objet contenant le prompt et la stratégie à tester,
     *                ainsi qu'un indicateur permettant d'inclure des métriques détaillées
     * @return 200 avec une réponse enrichie contenant le résultat du test,
     *         400 si le prompt est vide,
     *         500 en cas d'erreur interne.
     */
    @PostMapping("/test-strategies")
    public ResponseEntity<?> testPromptStrategies(@RequestBody PromptTestRequest request) {
        logger.info("Test de stratégie: prompt='{}...', strategy='{}'", 
                   request.getPrompt().substring(0, Math.min(50, request.getPrompt().length())),
                   request.getStrategy());

        try {
            CodeGenerationRequest genRequest = new CodeGenerationRequest(
                request.getPrompt(),
                request.getStrategy()
            );

            CodeGenerationResponse response = orchestrator.generateCode(genRequest);

            Map<String, Object> testResponse = new HashMap<>();
            testResponse.put("strategy", response.getStrategy());
            testResponse.put("userPrompt", response.getUserPrompt());
            testResponse.put("generatedCode", response.getGeneratedCode());
            testResponse.put("isValid", response.isValid());
            testResponse.put("validationErrors", response.getValidationErrors());
            testResponse.put("codeLength", response.getCodeLength());
            testResponse.put("promptTokensEstimate", response.getPromptTokensEstimate());
            testResponse.put("generationTimeMs", response.getGenerationTimeMs());

            if (request.isIncludeMetrics()) {
                testResponse.put("enrichedPrompt", response.getEnrichedPrompt());
                testResponse.put("rawResponse", response.getRawResponse());
            }

            return ResponseEntity.ok(testResponse);
        } catch (Exception e) {
            logger.error("Erreur lors du test de stratégie", e);
            return ResponseEntity.internalServerError()
                .body(new ErrorResponse("TEST_ERROR", e.getMessage()));
        }
    }
}
