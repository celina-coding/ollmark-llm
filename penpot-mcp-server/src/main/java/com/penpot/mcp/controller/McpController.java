package com.penpot.mcp.controller;

import com.penpot.mcp.service.PenpotAiService;
import com.penpot.mcp.tools.*;
import lombok.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.Message;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.*;

/**
 * Contrôleur REST pour les endpoints du protocole MCP (Model Context Protocol).
 * <p>
 * Ce contrôleur expose les API REST permettant aux clients d'interagir avec
 * le serveur MCP pour:
 * </p>
 * <ul>
 *   <li>Exécuter du code JavaScript dans le plugin Penpot</li>
 *   <li>Obtenir une vue d'ensemble de l'API Penpot</li>
 *   <li>Consulter la documentation des types API</li>
 *   <li>Dialoguer avec l'assistant AI</li>
 *   <li>Générer et exécuter du code via AI</li>
 * </ul>
 */
@Slf4j
@RestController
@RequestMapping("/mcp")
@RequiredArgsConstructor
public class McpController {

    /** Outil pour l'exécution de code dans le plugin */
    private final ExecuteCodeTool executeCodeTool;

    /** Outil pour obtenir la vue d'ensemble de l'API */
    private final HighLevelOverviewTool overviewTool;

    /** Outil pour consulter la documentation API */
    private final PenpotApiInfoTool apiInfoTool;

    /** Service AI pour la génération de code et le chat */
    private final PenpotAiService aiService;

    /**
     * Exécute du code JavaScript dans le contexte du plugin Penpot.
     *
     * @param request le corps de la requête contenant le code à exécuter
     * @param userToken token optionnel pour le mode multi-utilisateur (header X-User-Token)
     * @return une réponse contenant le résultat de l'exécution ou une erreur
     *         <ul>
     *           <li>Succès (200): {@code {success: true, result: "..."}}</li>
     *           <li>Erreur (500): {@code {success: false, error: "..."}}</li>
     *         </ul>
     */
    @PostMapping("/execute-code")
    public ResponseEntity<Map<String, Object>> executeCode(
        @RequestBody ExecuteCodeRequest request,
        @RequestHeader(value = "X-User-Token", required = false) String userToken
    ) {
        try {
            String result = executeCodeTool.execute(request.getCode(), userToken);
            return ResponseEntity.ok(Map.of(
                "success", true,
                "result", result
            ));
        } catch (Exception e) {
            log.error("Execute code failed", e);
            return ResponseEntity.status(500).body(Map.of(
                "success", false,
                "error", e.getMessage()
            ));
        }
    }

    /**
     * Obtient une vue d'ensemble de haut niveau de l'API Penpot.
     *
     * @return la documentation d'ensemble au format texte/markdown
     *         ou un message d'erreur en cas d'échec (status 500)
     */
    @GetMapping("/overview")
    public ResponseEntity<String> getOverview() {
        try {
            return ResponseEntity.ok(overviewTool.execute());
        } catch (Exception e) {
            log.error("Get overview failed", e);
            return ResponseEntity.status(500).body("Error: " + e.getMessage());
        }
    }

    /**
     * Récupère la documentation pour un type ou membre spécifique de l'API Penpot.
     *
     * @param type le nom du type API (ex: "Shape", "Board", "Penpot")
     * @param member le nom du membre optionnel (propriété ou méthode)
     * @return la documentation au format texte/markdown
     *         ou un message d'erreur en cas d'échec (status 500)
     */
    @GetMapping("/api-info")
    public ResponseEntity<String> getApiInfo(
        @RequestParam String type,
        @RequestParam(required = false) String member
    ) {
        try {
            return ResponseEntity.ok(apiInfoTool.execute(type, member));
        } catch (Exception e) {
            log.error("Get API info failed", e);
            return ResponseEntity.status(500).body("Error: " + e.getMessage());
        }
    }

    /**
     * Dialogue avec l'assistant AI en fournissant un contexte conversationnel.
     *
     * @param request la requête contenant le message et l'historique de conversation
     * @return une réponse contenant la réponse de l'AI ou une erreur
     *         <ul>
     *           <li>Succès (200): {@code {success: true, response: "..."}}</li>
     *           <li>Erreur (500): {@code {success: false, error: "..."}}</li>
     *         </ul>
     */
    @PostMapping("/chat")
    public ResponseEntity<Map<String, Object>> chat(
        @RequestBody ChatRequest request
    ) {
        try {
            String response = aiService.chat(
                request.getMessage(), 
                request.getHistory()
            );
            return ResponseEntity.ok(Map.of(
                "success", true,
                "response", response
            ));
        } catch (Exception e) {
            log.error("Chat failed", e);
            return ResponseEntity.status(500).body(Map.of(
                "success", false,
                "error", e.getMessage()
            ));
        }
    }

    /**
     * Génère du code JavaScript via AI puis l'exécute dans le plugin.
     * <p>
     * Cette endpoint combine la génération de code assistée par AI et son
     * exécution immédiate dans le contexte du plugin Penpot.
     *
     * @param request la requête contenant la tâche à accomplir et le contexte
     * @param userToken token optionnel pour le mode multi-utilisateur
     * @return une réponse contenant le code généré, le résultat ou une erreur
     *         <ul>
     *           <li>Succès (200): {@code {success: true, code: "...", result: "..."}}</li>
     *           <li>Erreur (500): {@code {success: false, error: "..."}}</li>
     *         </ul>
     */
    @PostMapping("/generate")
    public ResponseEntity<Map<String, Object>> generate(
        @RequestBody GenerateCodeRequest request,
        @RequestHeader(value = "X-User-Token", required = false) String userToken
    ) {
        try {
            String code = aiService.executeCodeWithAi(
                request.getTask(), 
                request.getContext()
            );
            log.info("Generated code: {}", code);

            String result = executeCodeTool.execute(code, userToken);
            return ResponseEntity.ok(Map.of(
                "success", true,
                "code", code,
                "result", result
            ));
        } catch (Exception e) {
            log.error("Generate and execute failed", e);
            return ResponseEntity.status(500).body(Map.of(
                "success", false,
                "error", e.getMessage()
            ));
        }
    }
}

/**
 * Objet de requête pour l'exécution de code.
 * <p>
 * Contient le code JavaScript à exécuter dans le plugin Penpot.
 */
@Data
class ExecuteCodeRequest {
    /** Le code JavaScript à exécuter */
    private String code;
}

/**
 * Objet de requête pour le chat AI.
 * <p>
 * Contient le message utilisateur et l'historique de conversation
 * pour maintenir le contexte.
 */
@Data
class ChatRequest {
    /** Le message de l'utilisateur */
    private String message;

    /** L'historique des messages précédents pour le contexte */
    private List<Message> history;
}

/**
 * Objet de requête pour la génération de code.
 * <p>
 * Contient la description de la tâche à accomplir et le contexte
 * nécessaire pour la génération de code.
 */
@Data
class GenerateCodeRequest {
    /** La tâche à accomplir en langage naturel */
    private String task;

    /** Le contexte additionnel pour la génération */
    private String context;
}