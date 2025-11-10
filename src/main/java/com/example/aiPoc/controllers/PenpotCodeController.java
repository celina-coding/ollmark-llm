package com.example.aiPoc.controllers;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.aiPoc.dto.request.CodeGenerationRequest;
import com.example.aiPoc.dto.request.PromptTestRequest;
import com.example.aiPoc.dto.response.CodeGenerationResponse;
import com.example.aiPoc.dto.response.ErrorResponse;
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
            }

            return ResponseEntity.ok(testResponse);
        } catch (Exception e) {
            logger.error("Erreur lors du test de stratégie", e);
            return ResponseEntity.internalServerError()
                .body(new ErrorResponse("TEST_ERROR", e.getMessage()));
        }
    }

    /**
     * Génère du code Penpot en utilisant automatiquement la stratégie jugée optimale
     * en fonction du prompt fourni.
     *
     * @param request map contenant au minimum la clé "prompt"
     * @return 200 avec un {@link CodeGenerationResponse} optimisé,
     *         400 si le prompt est vide,
     *         500 en cas d'erreur interne.
     */
    @PostMapping("/generate-optimized")
    public ResponseEntity<?> generateOptimized(@RequestBody Map<String, String> request) {
        String prompt = request.get("prompt");

        logger.info("Génération optimisée: prompt='{}'", prompt);

        try {
            if (prompt == null || prompt.trim().isEmpty()) {
                return ResponseEntity.badRequest()
                    .body(new ErrorResponse("INVALID_REQUEST", "Le prompt ne peut pas être vide"));
            }

            CodeGenerationResponse response = orchestrator.generateCodeOptimized(prompt);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Erreur lors de la génération optimisée", e);
            return ResponseEntity.internalServerError()
                .body(new ErrorResponse("GENERATION_ERROR", e.getMessage()));
        }
    }

    /**
     * Nettoie un code généré (suppression d’artefacts, de commentaires, formatage, etc.).
     *
     * @param request map contenant la clé "code" représentant le code brut à nettoyer
     * @return 200 avec le code nettoyé et des métriques de réduction,
     *         400 si aucun code n'est fourni,
     *         500 en cas d'erreur interne.
     */
    @PostMapping("/clean")
    public ResponseEntity<?> cleanCode(@RequestBody Map<String, String> request) {
        String code = request.get("code");

        logger.debug("Nettoyage de code: {} caractères", code != null ? code.length() : 0);

        try {
            if (code == null || code.trim().isEmpty()) {
                return ResponseEntity.badRequest()
                    .body(new ErrorResponse("INVALID_REQUEST", "Le code ne peut pas être vide"));
            }

            String cleanedCode = orchestrator.cleanCode(code);

            Map<String, Object> response = new HashMap<>();
            response.put("originalCode", code);
            response.put("cleanedCode", cleanedCode);
            response.put("originalLength", code.length());
            response.put("cleanedLength", cleanedCode.length());
            response.put("reductionPercent", 
                        ((code.length() - cleanedCode.length()) * 100.0) / code.length());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Erreur lors du nettoyage", e);
            return ResponseEntity.internalServerError()
                .body(new ErrorResponse("CLEANING_ERROR", e.getMessage()));
        }
    }

    /**
     * Renvoie des statistiques globales liées aux générations de code
     * (ex: nombre de requêtes, taux de réussite, temps moyen de génération…).
     *
     * @return 200 avec un objet contenant les statistiques et un timestamp,
     *         500 en cas d'erreur interne.
     */
    @GetMapping("/statistics")
    public ResponseEntity<?> getStatistics() {
        logger.debug("Récupération des statistiques");

        try {
            String stats = orchestrator.getStatistics();

            Map<String, Object> response = new HashMap<>();
            response.put("statistics", stats);
            response.put("timestamp", System.currentTimeMillis());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Erreur lors de la récupération des statistiques", e);
            return ResponseEntity.internalServerError()
                .body(new ErrorResponse("STATS_ERROR", e.getMessage()));
        }
    }

    /**
     * Génère du code avec un mécanisme de retry automatique.  
     * Si la génération échoue, une nouvelle tentative est lancée jusqu'à atteindre le nombre maximum défini.
     *
     * @param request map contenant :
     *                <ul>
     *                    <li>prompt (obligatoire)</li>
     *                    <li>strategy (optionnel – défaut : "detailed")</li>
     *                    <li>maxRetries (optionnel – défaut : 3)</li>
     *                </ul>
     * @return 200 avec la réponse finale après retry(s),
     *         400 si le prompt est vide,
     *         500 en cas d'erreur interne.
     */
    @PostMapping("/generate-retry")
    public ResponseEntity<?> generateWithRetry(@RequestBody Map<String, Object> request) {
        String prompt = (String) request.get("prompt");
        String strategy = (String) request.getOrDefault("strategy", "detailed");
        int maxRetries = (int) request.getOrDefault("maxRetries", 3);

        logger.info("Génération avec retry: prompt='{}', maxRetries={}", prompt, maxRetries);

        try {
            if (prompt == null || prompt.trim().isEmpty()) {
                return ResponseEntity.badRequest()
                    .body(new ErrorResponse("INVALID_REQUEST", "Le prompt ne peut pas être vide"));
            }

            CodeGenerationRequest genRequest = new CodeGenerationRequest(prompt, strategy);
            CodeGenerationResponse response = orchestrator.generateCodeWithRetry(genRequest, maxRetries);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Erreur lors de la génération avec retry", e);
            return ResponseEntity.internalServerError()
                .body(new ErrorResponse("GENERATION_ERROR", e.getMessage()));
        }
    }
}