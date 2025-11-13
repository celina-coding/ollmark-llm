package com.example.aiPoc.controllers;

import java.util.*;

import org.slf4j.*;
import org.springframework.http.ResponseEntity;
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
@CrossOrigin(origins = {"http://localhost:61873", "http://localhost:8080"})
@RestController
@RequestMapping("/api/penpot")
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
        logger.info("Requête de génération: prompt='{}', strategy='{}'", 
                   request.getPrompt(), request.getStrategy());

        try {
            if (request.getPrompt() == null || request.getPrompt().trim().isEmpty()) {
                return ResponseEntity.badRequest()
                    .body(new ErrorResponse("INVALID_REQUEST", "Le prompt ne peut pas être vide"));
            }

            CodeGenerationResponse response = orchestrator.generateCode(request);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Erreur lors de la génération de code", e);
            return ResponseEntity.internalServerError()
                .body(new ErrorResponse("GENERATION_ERROR", e.getMessage()));
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
        logger.info("Test de stratégie: prompt='{}', strategy='{}'", 
                   request.getPrompt(), request.getStrategy());

        try {
            if (request.getPrompt() == null || request.getPrompt().trim().isEmpty()) {
                return ResponseEntity.badRequest()
                    .body(new ErrorResponse("INVALID_REQUEST", "Le prompt ne peut pas être vide"));
            }

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