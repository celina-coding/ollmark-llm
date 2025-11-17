package com.example.aiPoc.controllers;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.aiPoc.dto.request.CodeGenerationRequest;
import com.example.aiPoc.dto.response.CodeGenerationResponse;
import com.example.aiPoc.models.EvaluationResult;
import com.example.aiPoc.services.evaluation.EvaluationService;
import com.example.aiPoc.services.orchestration.CodeGenerationOrchestrator;

/**
 * Contrôleur REST responsable de l'exécution, de l'évaluation et de l'exportation
 * des résultats du Proof of Concept (POC) d’évaluation des modèles d’IA.
 *
 * <p>Il offre des endpoints pour :
 * <ul>
 *   <li>Tester la stabilité des modèles IA sur un prompt donné,</li>
 *   <li>Exporter les résultats sous format CSV ou JSON,</li>
 *   <li>Consulter et filtrer les résultats stockés,</li>
 *   <li>Calculer des statistiques globales,</li>
 *   <li>Réinitialiser l’ensemble des évaluations.</li>
 * </ul>
 * </p>
 * 
 * @see CodeGenerationOrchestrator
 * @see EvaluationService
 */
@CrossOrigin(origins = {"http://localhost:61873", "http://localhost:8080"})
@RestController
@RequestMapping("/api/evaluation")
public class EvaluationController {

    /** Logger pour la journalisation des événements et erreurs. */
    private static final Logger logger = LoggerFactory.getLogger(EvaluationController.class);

    /** Orchestrateur responsable de la génération de code par l'IA. */
    private final CodeGenerationOrchestrator orchestrator;

    /** Service gérant l’évaluation qualitative et quantitative des résultats IA. */
    private final EvaluationService evaluationService;
    
    /** Liste interne maintenant les résultats des évaluations exécutées. */
    private final List<EvaluationResult> results = new ArrayList<>();

    /**
     * Constructeur principal injectant les dépendances nécessaires.
     *
     * @param orchestrator orchestrateur pour la génération de code IA
     * @param evaluationService service d’évaluation des résultats IA
     */
    public EvaluationController(
        CodeGenerationOrchestrator orchestrator,
        EvaluationService evaluationService
    ) {
        this.orchestrator = orchestrator;
        this.evaluationService = evaluationService;
    }

    /**
     * Exécute un test de stabilité d’un prompt en le soumettant cinq fois au même modèle IA.
     *
     * <p>Chaque itération génère un code indépendant afin d’évaluer la cohérence du modèle.
     * Les résultats sont ensuite analysés et stockés en mémoire.</p>
     *
     * @param request un {@link Map} contenant :
     *        <ul>
     *          <li><b>model</b> : nom du modèle IA (par défaut : <code>llama-3.3-70b</code>)</li>
     *          <li><b>promptId</b> : identifiant du prompt testé</li>
     *          <li><b>prompt</b> : texte du prompt à évaluer</li>
     *          <li><b>strategy</b> : stratégie de génération (par défaut : <code>creation</code>)</li>
     *        </ul>
     * @return une {@link ResponseEntity} contenant le résultat principal, les réponses brutes,
     *         et le nombre total de tests exécutés
     */
    @PostMapping("/test-prompt")
    public ResponseEntity<?> testPrompt(@RequestBody Map<String, String> request) {
        String modelName = request.getOrDefault("model", "gemini-2.5-flash");
        String promptId = request.get("promptId");
        String promptText = request.get("prompt");
        String strategy = request.getOrDefault("strategy", "creation");

        logger.info("Test d'évaluation: model={}, prompt={}", modelName, promptId);

        try {
            List<CodeGenerationResponse> stabilityTests = new ArrayList<>();

            // Exécute 5 fois le même prompt
            for (int i = 0; i < 5; i++) {
                logger.debug("Exécution {}/5 du prompt {}", i + 1, promptId);

                CodeGenerationRequest genRequest = new CodeGenerationRequest(promptText, strategy);
                CodeGenerationResponse response = orchestrator.generateCode(genRequest);

                stabilityTests.add(response);
                Thread.sleep(2000);
            }

            CodeGenerationResponse mainResponse = stabilityTests.get(0);

            EvaluationResult evaluation = evaluationService.evaluate(
                modelName, promptId, promptText, mainResponse, stabilityTests
            );

            evaluation.setCodeGenere(mainResponse.getGeneratedCode());
            results.add(evaluation);

            logger.info("Évaluation terminée: {}", evaluation);

            return ResponseEntity.ok(Map.of(
                "evaluation", evaluation,
                "allResponses", stabilityTests,
                "totalTests", results.size()
            ));
        } catch (Exception e) {
            logger.error("Erreur lors du test", e);
            return ResponseEntity.internalServerError()
                .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Exporte l’ensemble des résultats d’évaluation au format CSV (compatible Excel).
     *
     * @return un fichier CSV encapsulé dans une {@link ResponseEntity} avec les en-têtes HTTP appropriés
     */
    @GetMapping("/export/csv")
    public ResponseEntity<byte[]> exportCsv() {
        logger.info("Export CSV demandé: {} résultats", results.size());

        try {
            StringBuilder csv = new StringBuilder();
            csv.append("Modèle IA testé;ID_Prompt;Temps réponse (s);Tokens moyens;");
            csv.append("Stabilité (/5);Cohérence (/5);Respect du sujet (/5);");
            csv.append("Richesse structurelle (/5);Créativité (/5);Moyenne globale;");
            csv.append("Réalisable ?;Observations\n");

            for (EvaluationResult result : results) {
                csv.append(result.toCsvLine()).append("\n");
            }

            byte[] data = csv.toString().getBytes("UTF-8");

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.parseMediaType("text/csv"));
            String filename = "evaluation_" + 
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")) + 
                ".csv";
            headers.setContentDispositionFormData("attachment", filename);

            logger.info("Export CSV généré: {} octets", data.length);

            return new ResponseEntity<>(data, headers, HttpStatus.OK);
        } catch (Exception e) {
            logger.error("Erreur lors de l'export CSV", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Exporte tous les résultats sous format JSON.
     *
     * @return une {@link ResponseEntity} contenant la liste complète des résultats avec un horodatage
     */
    @GetMapping("/export/json")
    public ResponseEntity<?> exportJson() {
        logger.info("Export JSON demandé: {} résultats", results.size());
        return ResponseEntity.ok(Map.of(
            "timestamp", LocalDateTime.now(),
            "totalResults", results.size(),
            "results", results
        ));
    }

    /**
     * Sauvegarde les résultats actuels d’évaluation dans un fichier CSV sur le disque.
     *
     * @param request un {@link Map} contenant éventuellement le nom du fichier de sortie
     * @return une {@link ResponseEntity} indiquant le chemin, le nom du fichier et le nombre d’entrées sauvegardées
     */
    @PostMapping("/save")
    public ResponseEntity<?> saveResults(@RequestBody Map<String, String> request) {
        String filename = request.getOrDefault("filename", "evaluation_results.csv");

        try {
            Path outputPath = Paths.get("evaluations", filename);
            Files.createDirectories(outputPath.getParent());

            StringBuilder csv = new StringBuilder();
            csv.append("Modèle IA testé;ID_Prompt;Temps réponse (s);Tokens moyens;");
            csv.append("Stabilité (/5);Cohérence (/5);Respect du sujet (/5);");
            csv.append("Richesse structurelle (/5);Créativité (/5);Moyenne globale;");
            csv.append("Réalisable ?;Observations\n");

            for (EvaluationResult result : results) {
                csv.append(result.toCsvLine()).append("\n");
            }

            Files.writeString(outputPath, csv.toString());
            logger.info("Résultats sauvegardés dans: {}", outputPath.toAbsolutePath());

            return ResponseEntity.ok(Map.of(
                "message", "Résultats sauvegardés",
                "filename", filename,
                "path", outputPath.toAbsolutePath().toString(),
                "count", results.size()
            ));

        } catch (IOException e) {
            logger.error("Erreur lors de la sauvegarde", e);
            return ResponseEntity.internalServerError()
                .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Récupère et filtre les résultats d’évaluation selon le modèle ou l’identifiant du prompt.
     *
     * @param model le nom du modèle IA (facultatif)
     * @param promptId l’identifiant du prompt (facultatif)
     * @return une {@link ResponseEntity} contenant les résultats filtrés et des métadonnées de comptage
     */
    @GetMapping("/results")
    public ResponseEntity<?> getResults(
            @RequestParam(required = false) String model,
            @RequestParam(required = false) String promptId) {

        List<EvaluationResult> filtered = results;

        if (model != null) {
            filtered = filtered.stream()
                .filter(r -> model.equals(r.getModeleIA()))
                .toList();
        }

        if (promptId != null) {
            filtered = filtered.stream()
                .filter(r -> promptId.equals(r.getIdPrompt()))
                .toList();
        }

        return ResponseEntity.ok(Map.of(
            "results", filtered,
            "count", filtered.size(),
            "total", results.size()
        ));
    }

    /**
     * Calcule des statistiques globales à partir des résultats en mémoire.
     *
     * @return une {@link ResponseEntity} contenant les moyennes, taux de réussite et indicateurs agrégés
     */
    @GetMapping("/statistics")
    public ResponseEntity<?> getStatistics() {
        if (results.isEmpty()) {
            return ResponseEntity.ok(Map.of("message", "Aucun résultat disponible"));
        }

        double avgTemps = results.stream()
            .mapToDouble(r -> r.getTempsReponseS() != null ? r.getTempsReponseS() : 0.0)
            .average()
            .orElse(0.0);

        double avgMoyenne = results.stream()
            .mapToDouble(r -> r.getMoyenneGlobale() != null ? r.getMoyenneGlobale() : 0.0)
            .average()
            .orElse(0.0);

        long realisables = results.stream()
            .filter(r -> Boolean.TRUE.equals(r.isRealisable()))
            .count();

        Map<String, Object> stats = new HashMap<>();
        stats.put("totalTests", results.size());
        stats.put("avgTempsReponse", String.format("%.2f s", avgTemps));
        stats.put("avgMoyenneGlobale", String.format("%.2f/5", avgMoyenne));
        stats.put("tauxRealisable", String.format("%.1f%%", (realisables * 100.0 / results.size())));
        stats.put("realisables", realisables);
        stats.put("nonRealisables", results.size() - realisables);

        return ResponseEntity.ok(stats);
    }

    /**
     * Supprime tous les résultats actuellement stockés en mémoire.
     *
     * @return une {@link ResponseEntity} confirmant le nombre d’entrées effacées
     */
    @DeleteMapping("/clear")
    public ResponseEntity<?> clearResults() {
        int count = results.size();
        results.clear();
        logger.info("Résultats effacés: {} entrées supprimées", count);

        return ResponseEntity.ok(Map.of(
            "message", "Résultats effacés",
            "count", count
        ));
    }
}