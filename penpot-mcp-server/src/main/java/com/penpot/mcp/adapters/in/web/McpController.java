package com.penpot.mcp.adapters.in.web;

import com.penpot.mcp.core.domain.*;
import com.penpot.mcp.core.ports.in.*;
import com.penpot.mcp.model.MarketingTemplate;
import lombok.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.*;

/**
 * Point d’entrée REST principal du module MCP (Marketing Copilot Plugin).
 * <p>
 * Ce contrôleur expose les endpoints HTTP permettant :
 * <ul>
 *     <li>d’exécuter du code JavaScript dans le contexte du plugin Penpot</li>
 *     <li>d’interagir avec un assistant IA conversationnel</li>
 *     <li>de gérer des conversations persistées via ChatMemory</li>
 *     <li>de générer du code JavaScript via IA</li>
 *     <li>de rechercher et consulter des templates marketing</li>
 * </ul>
 *
 * <h2>Architecture</h2>
 * <p>
 * Le contrôleur agit uniquement comme adaptateur HTTP (layer "in"),
 * déléguant toute la logique métier aux {@code UseCase} du cœur applicatif.
 */
@Slf4j
@RestController
@RequestMapping("/mcp")
@RequiredArgsConstructor
public class McpController {

    /** Use case chargé de l’exécution de code JavaScript. */
    private final ExecuteCodeUseCase executeCodeUseCase;

    /** Use case gérant les conversations IA et la mémoire de chat. */
    private final ConversationChatUseCase conversationChatUseCase;

    /** Use case responsable de la génération de code JavaScript via IA. */
    private final GenerateCodeUseCase generateCodeUseCase;

    /** Use case de recherche et récupération de templates marketing. */
    private final SearchTemplatesUseCase searchTemplatesUseCase;

    /**
     * Exécute du code JavaScript dans le contexte du plugin Penpot.
     * <p>
     * Le code est validé puis transmis au moteur d’exécution.
     * Un token utilisateur optionnel permet de gérer un contexte multi-utilisateur.
     *
     * @param request   corps de la requête contenant le code JavaScript à exécuter
     * @param userToken token optionnel d’identification utilisateur
     * @return une réponse HTTP contenant :
     * <ul>
     *     <li>le résultat de l’exécution en cas de succès</li>
     *     <li>une description d’erreur en cas d’échec</li>
     * </ul>
     */
    @PostMapping("/execute-code")
    public ResponseEntity<Map<String, Object>> executeCode(
        @RequestBody ExecuteCodeRequest request,
        @RequestHeader(value = "X-User-Token", required = false) String userToken
    ) {
        try {
            log.info("POST /mcp/execute-code (code length: {} chars)", 
                request.getCode() != null ? request.getCode().length() : 0);

            ExecuteCodeCommand command = ExecuteCodeCommand.builder()
                .code(request.getCode())
                .userToken(java.util.Optional.ofNullable(userToken))
                .build();

            command.validate();
            TaskResult result = executeCodeUseCase.execute(command);

            return ResponseEntity.ok(buildTaskResultResponse(result));
        } catch (IllegalArgumentException e) {
            log.warn("Invalid request: {}", e.getMessage());
            return ResponseEntity.badRequest()
                .body(buildErrorResponse(e.getMessage()));
        } catch (Exception e) {
            log.error("Execute code failed", e);
            return ResponseEntity.status(500)
                .body(buildErrorResponse(e.getMessage()));
        }
    }

    /**
     * Envoie un message à l’assistant IA dans le cadre d’une conversation existante.
     * <p>
     * La mémoire conversationnelle est entièrement gérée par ChatMemory :
     * <ul>
     *     <li>chargement automatique de l’historique</li>
     *     <li>persistance des messages</li>
     *     <li>gestion de la fenêtre de contexte</li>
     * </ul>
     *
     * <h3>Outils IA</h3>
     * <p>
     * L’IA peut invoquer automatiquement des outils de recherche de templates
     * (function calling) lorsqu’une intention marketing est détectée.
     *
     * @param request requête contenant l’identifiant de conversation et le message utilisateur
     * @return réponse HTTP contenant la réponse générée par l’IA
     */
    @PostMapping("/chat")
    public ResponseEntity<Map<String, Object>> chat(@RequestBody ChatRequest request) {
        try {
            log.info("POST /mcp/chat (conversationId: {}, message length: {} chars)", 
                request.getConversationId(),
                request.getMessage() != null ? request.getMessage().length() : 0);

            String response = conversationChatUseCase.chat(
                request.getConversationId(), 
                request.getMessage()
            );

            return ResponseEntity.ok(Map.of(
                "success", true,
                "conversationId", request.getConversationId(),
                "response", response,
                "info", "Conversation history managed automatically by ChatMemory"
            ));
        } catch (IllegalArgumentException e) {
            log.warn("Invalid chat request: {}", e.getMessage());
            return ResponseEntity.badRequest()
                .body(buildErrorResponse(e.getMessage()));
        } catch (Exception e) {
            log.error("Chat failed", e);
            return ResponseEntity.status(500)
                .body(buildErrorResponse(e.getMessage()));
        }
    }

    /**
     * Démarre une nouvelle conversation IA.
     * <p>
     * Une conversation peut être associée à un utilisateur identifié
     * ou rester anonyme.
     *
     * @param request requête optionnelle contenant l’identifiant utilisateur
     * @return un nouvel identifiant de conversation
     */
    @PostMapping("/chat/new")
    public ResponseEntity<Map<String, Object>> startNewConversation(
        @RequestBody(required = false) NewConversationRequest request
    ) {
        try {
            String userId = request != null ? request.getUserId() : null;
            log.info("POST /mcp/chat/new (userId: {})", userId != null ? userId : "anonymous");

            String conversationId = conversationChatUseCase.startNewConversation(userId);

            return ResponseEntity.ok(Map.of(
                "success", true,
                "conversationId", conversationId,
                "userId", userId != null ? userId : "anonymous",
                "info", "New conversation started. Use this conversationId for subsequent messages."
            ));
        } catch (Exception e) {
            log.error("Failed to start new conversation", e);
            return ResponseEntity.status(500).body(buildErrorResponse(e.getMessage()));
        }
    }

    /**
     * Supprime l’intégralité de l’historique d’une conversation.
     *
     * @param conversationId identifiant de la conversation à supprimer
     * @return confirmation de la suppression
     */
    @DeleteMapping("/chat/{conversationId}")
    public ResponseEntity<Map<String, Object>> clearConversation(
        @PathVariable String conversationId
    ) {
        try {
            log.info("DELETE /mcp/chat/{}", conversationId);
            conversationChatUseCase.clearConversation(conversationId);

            return ResponseEntity.ok(Map.of(
                "success", true,
                "conversationId", conversationId,
                "info", "Conversation history cleared successfully"
            ));
        } catch (IllegalArgumentException e) {
            log.warn("Invalid conversation ID: {}", e.getMessage());
            return ResponseEntity.badRequest().body(buildErrorResponse(e.getMessage()));
        } catch (Exception e) {
            log.error("Failed to clear conversation", e);
            return ResponseEntity.status(500).body(buildErrorResponse(e.getMessage()));
        }
    }

    /**
     * Génère du code JavaScript via IA et peut l’exécuter immédiatement.
     *
     * @param request   requête contenant la tâche et le contexte
     * @param userToken token optionnel utilisateur
     * @return code généré et résultat d’exécution éventuel
     */
    @PostMapping("/generate")
    public ResponseEntity<Map<String, Object>> generate(
        @RequestBody GenerateCodeRequest request,
        @RequestHeader(value = "X-User-Token", required = false) String userToken
    ) {
        try {
            log.info("POST /mcp/generate (task: {}, execute: {})", 
                request.getTask(),
                request.isExecuteImmediately());

            GenerateCodeResult result = generateCodeUseCase.generate(
                request.getTask(),
                request.getContext(),
                userToken,
                request.isExecuteImmediately()
            );

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("code", result.getGeneratedCode());

            result.getExecutionResult()
                    .ifPresent(exec -> response.put("execution", buildTaskResultResponse(exec)));

            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            log.warn("Invalid generate request: {}", e.getMessage());
            return ResponseEntity.badRequest().body(buildErrorResponse(e.getMessage()));
        } catch (Exception e) {
            log.error("Generate and execute failed", e);
            return ResponseEntity.status(500).body(buildErrorResponse(e.getMessage()));
        }
    }

    /**
     * Recherche des templates marketing par requête textuelle.
     *
     * @param request critères de recherche
     * @return liste des templates correspondants
     */
    @PostMapping("/templates/search")
    public ResponseEntity<Map<String, Object>> searchTemplates(
        @RequestBody TemplateSearchRequest request
    ) {
        try {
            log.info("POST /mcp/templates/search (query: {})", request.getQuery());
            List<MarketingTemplate> templates =
                    searchTemplatesUseCase.searchByQuery(request.getQuery());

            return ResponseEntity.ok(Map.of(
                "success", true,
                "count", templates.size(),
                "templates", templates
            ));
        } catch (IllegalArgumentException e) {
            log.warn("Invalid search request: {}", e.getMessage());
            return ResponseEntity.badRequest().body(buildErrorResponse(e.getMessage()));
        } catch (Exception e) {
            log.error("Template search failed", e);
            return ResponseEntity.status(500).body(buildErrorResponse(e.getMessage()));
        }
    }

    /**
     * Récupère l’ensemble des templates disponibles.
     *
     * @return liste complète des templates marketing
     */
    @GetMapping("/templates")
    public ResponseEntity<Map<String, Object>> getAllTemplates() {
        try {
            log.info("GET /mcp/templates");
            List<MarketingTemplate> templates = searchTemplatesUseCase.getAllTemplates();

            return ResponseEntity.ok(Map.of(
                "success", true,
                "count", templates.size(),
                "templates", templates
            ));
        } catch (Exception e) {
            log.error("Failed to retrieve templates", e);
            return ResponseEntity.status(500).body(buildErrorResponse(e.getMessage()));
        }
    }

    /**
     * Récupère les templates d’un type donné.
     *
     * @param type type de template (ex: instagram, email, banner)
     * @return templates correspondant au type
     */
    @GetMapping("/templates/type/{type}")
    public ResponseEntity<Map<String, Object>> getTemplatesByType(@PathVariable String type) {
        try {
            log.info("GET /mcp/templates/type/{}", type);
            List<MarketingTemplate> templates = searchTemplatesUseCase.searchByType(type);

            return ResponseEntity.ok(Map.of(
                "success", true,
                "type", type,
                "count", templates.size(),
                "templates", templates
            ));
        } catch (Exception e) {
            log.error("Failed to retrieve templates by type", e);
            return ResponseEntity.status(500).body(buildErrorResponse(e.getMessage()));
        }
    }

    /**
     * Récupère les templates associés à un tag.
     *
     * @param tag tag fonctionnel ou marketing
     * @return templates associés au tag
     */
    @GetMapping("/templates/tag/{tag}")
    public ResponseEntity<Map<String, Object>> getTemplatesByTag(@PathVariable String tag) {
        try {
            log.info("GET /mcp/templates/tag/{}", tag);
            List<MarketingTemplate> templates = searchTemplatesUseCase.searchByTag(tag);

            return ResponseEntity.ok(Map.of(
                "success", true,
                "tag", tag,
                "count", templates.size(),
                "templates", templates
            ));
        } catch (Exception e) {
            log.error("Failed to retrieve templates by tag", e);
            return ResponseEntity.status(500).body(buildErrorResponse(e.getMessage()));
        }
    }

    /**
     * Construit une réponse HTTP normalisée à partir d’un {@link TaskResult}.
     *
     * @param result résultat métier d’une tâche
     * @return map sérialisable en JSON
     */
    private Map<String, Object> buildTaskResultResponse(TaskResult result) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", result.isSuccess());

        if (!result.isSuccess()) {
            result.getError().ifPresent(error -> response.put("error", error));
        } else {
            result.getData().ifPresent(data -> response.put("result", data));
        }

        if (!result.getLogs().isEmpty()) response.put("logs", result.getLogs());
        return response;
    }

    /**
     * Construit une réponse d’erreur standardisée.
     *
     * @param errorMessage message d’erreur
     * @return map d’erreur sérialisable en JSON
     */
    private Map<String, Object> buildErrorResponse(String errorMessage) {
        return Map.of(
            "success", false,
            "error", errorMessage != null ? errorMessage : "Unknown error"
        );
    }
}

/**
 * DTO représentant une requête d’exécution de code JavaScript.
 */
@Data
class ExecuteCodeRequest {

    /** Code JavaScript à exécuter. */
    private String code;
}

/**
 * DTO représentant une requête de chat conversationnel.
 */
@Data
class ChatRequest {

    /**
     * Identifiant unique de la conversation.
     * Permet de récupérer et persister l’historique.
     */
    private String conversationId;

    /** Message envoyé par l’utilisateur. */
    private String message;
}

/**
 * DTO utilisé pour démarrer une nouvelle conversation.
 */
@Data
class NewConversationRequest {

    /**
     * Identifiant utilisateur optionnel.
     * Peut être {@code null} pour une conversation anonyme.
     */
    private String userId;
}

/**
 * DTO représentant une requête de génération de code via IA.
 */
@Data
class GenerateCodeRequest {

    /** Description de la tâche à réaliser. */
    private String task;

    /** Contexte optionnel influençant la génération. */
    private String context;

    /**
     * Indique si le code généré doit être exécuté immédiatement.
     * Valeur par défaut : {@code true}.
     */
    private boolean executeImmediately = true;
}

/**
 * DTO représentant une requête de recherche de templates marketing.
 */
@Data
class TemplateSearchRequest {

    /** Texte de recherche libre. */
    private String query;
}