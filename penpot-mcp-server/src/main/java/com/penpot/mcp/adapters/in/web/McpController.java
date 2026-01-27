package com.penpot.mcp.adapters.in.web;

import com.penpot.mcp.core.domain.*;
import com.penpot.mcp.core.ports.in.*;
import com.penpot.mcp.model.MarketingTemplate;
import lombok.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.Message;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.*;

/**
 * Contrôleur REST complet pour les endpoints MCP (Model Context Protocol).
 * 
 * TEMPLATE FUNCTIONALITY:
 * The primary way to work with templates is now through AI function calling.
 * The AI has access to template search tools and will automatically use them
 * when users ask about creating marketing materials.
 * 
 * The REST endpoints below are kept for:
 * - Direct programmatic access
 * - Testing and debugging
 * - Integration with external systems
 * 
 * For conversational template discovery and generation, use the /mcp/chat or
 * /mcp/generate endpoints - the AI will automatically call template tools as needed.
 */
@Slf4j
@RestController
@RequestMapping("/mcp")
@RequiredArgsConstructor
public class McpController {

    private final ExecuteCodeUseCase executeCodeUseCase;
    private final ChatUseCase chatUseCase;
    private final GenerateCodeUseCase generateCodeUseCase;
    private final SearchTemplatesUseCase searchTemplatesUseCase;

    /**
     * Exécute du code JavaScript dans le contexte du plugin Penpot.
     * 
     * @param request le corps de la requête contenant le code
     * @param userToken token optionnel pour le mode multi-utilisateur
     * @return réponse avec le résultat ou une erreur
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
     * Dialogue avec l'assistant AI en fournissant un contexte conversationnel.
     * 
     * The AI has access to template search tools via function calling.
     * When users ask about creating marketing materials, the AI will:
     * 1. Automatically search for relevant templates
     * 2. Present options to the user
     * 3. Generate code from selected templates
     * 
     * Example queries that trigger template tools:
     * - "Create an Instagram post for my bakery's morning deal"
     * - "I need a social media post about a product launch"
     * - "Help me make an email newsletter"
     * 
     * @param request la requête contenant le message et l'historique
     * @return réponse contenant la réponse de l'AI ou une erreur
     */
    @PostMapping("/chat")
    public ResponseEntity<Map<String, Object>> chat(@RequestBody ChatRequest request) {
        try {
            log.info("POST /mcp/chat (message length: {} chars, history: {} messages)", 
                request.getMessage() != null ? request.getMessage().length() : 0,
                request.getHistory() != null ? request.getHistory().size() : 0);

            String response = chatUseCase.chat(
                request.getMessage(), 
                request.getHistory()
            );

            return ResponseEntity.ok(Map.of(
                "success", true,
                "response", response,
                "info", "AI has access to template search tools via function calling"
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
     * Génère du code JavaScript via AI puis optionnellement l'exécute dans le plugin.
     * Combine la génération de code assistée par AI et son exécution.
     * 
     * The AI can use template search tools during code generation.
     * If the task involves marketing materials, the AI may:
     * - Search for relevant templates
     * - Use template design recipes as a starting point
     * - Customize generated code based on user requirements
     * 
     * @param request la requête contenant la tâche et le contexte
     * @param userToken token optionnel pour le mode multi-utilisateur
     * @return réponse contenant le code généré, le résultat ou une erreur
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

            result.getExecutionResult().ifPresent(execResult -> {
                response.put("execution", buildTaskResultResponse(execResult));
            });

            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            log.warn("Invalid generate request: {}", e.getMessage());
            return ResponseEntity.badRequest()
                .body(buildErrorResponse(e.getMessage()));
        } catch (Exception e) {
            log.error("Generate and execute failed", e);
            return ResponseEntity.status(500)
                .body(buildErrorResponse(e.getMessage()));
        }
    }

    /**
     * Direct REST endpoint for template search.
     * 
     * NOTE: This is a direct programmatic endpoint.
     * For conversational access, prefer using /mcp/chat where the AI
     * will automatically call the searchTemplates tool as needed.
     * 
     * @param request requête contenant la query de recherche
     * @return liste des templates correspondants
     */
    @PostMapping("/templates/search")
    public ResponseEntity<Map<String, Object>> searchTemplates(
        @RequestBody TemplateSearchRequest request
    ) {
        try {
            log.info("POST /mcp/templates/search (query: {}) - Direct API access", 
                    request.getQuery());

            List<MarketingTemplate> templates = searchTemplatesUseCase.searchByQuery(
                request.getQuery()
            );

            return ResponseEntity.ok(Map.of(
                "success", true,
                "count", templates.size(),
                "templates", templates
            ));
        } catch (IllegalArgumentException e) {
            log.warn("Invalid search request: {}", e.getMessage());
            return ResponseEntity.badRequest()
                .body(buildErrorResponse(e.getMessage()));
        } catch (Exception e) {
            log.error("Template search failed", e);
            return ResponseEntity.status(500)
                .body(buildErrorResponse(e.getMessage()));
        }
    }

    /**
     * Direct REST endpoint to get all templates.
     * 
     * NOTE: For conversational access with filtering and recommendations,
     * prefer using /mcp/chat where the AI can help find the right template.
     * 
     * @return liste complète des templates
     */
    @GetMapping("/templates")
    public ResponseEntity<Map<String, Object>> getAllTemplates() {
        try {
            log.info("GET /mcp/templates - Direct API access");

            List<MarketingTemplate> templates = searchTemplatesUseCase.getAllTemplates();

            return ResponseEntity.ok(Map.of(
                "success", true,
                "count", templates.size(),
                "templates", templates
            ));
        } catch (Exception e) {
            log.error("Failed to retrieve templates", e);
            return ResponseEntity.status(500)
                .body(buildErrorResponse(e.getMessage()));
        }
    }

    /**
     * Direct REST endpoint to get templates by type.
     * 
     * @param type le type recherché
     * @return liste des templates de cette catégorie
     */
    @GetMapping("/templates/type/{type}")
    public ResponseEntity<Map<String, Object>> getTemplatesByType(
        @PathVariable String type
    ) {
        try {
            log.info("GET /mcp/templates/type/{} - Direct API access", type);

            List<MarketingTemplate> templates = searchTemplatesUseCase.searchByType(type);

            return ResponseEntity.ok(Map.of(
                "success", true,
                "type", type,
                "count", templates.size(),
                "templates", templates
            ));
        } catch (Exception e) {
            log.error("Failed to retrieve templates by type", e);
            return ResponseEntity.status(500)
                .body(buildErrorResponse(e.getMessage()));
        }
    }

    /**
     * Direct REST endpoint to get templates by tag.
     * 
     * @param tag le tag recherché
     * @return liste des templates ayant ce tag
     */
    @GetMapping("/templates/tag/{tag}")
    public ResponseEntity<Map<String, Object>> getTemplatesByTag(
        @PathVariable String tag
    ) {
        try {
            log.info("GET /mcp/templates/tag/{} - Direct API access", tag);

            List<MarketingTemplate> templates = searchTemplatesUseCase.searchByTag(tag);

            return ResponseEntity.ok(Map.of(
                "success", true,
                "tag", tag,
                "count", templates.size(),
                "templates", templates
            ));
        } catch (Exception e) {
            log.error("Failed to retrieve templates by tag", e);
            return ResponseEntity.status(500)
                .body(buildErrorResponse(e.getMessage()));
        }
    }

    // ========== MÉTHODES UTILITAIRES ==========

    /**
     * Construit une réponse depuis un TaskResult.
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
     * Construit une réponse d'erreur.
     */
    private Map<String, Object> buildErrorResponse(String errorMessage) {
        return Map.of(
            "success", false,
            "error", errorMessage != null ? errorMessage : "Unknown error"
        );
    }
}

/**
 * DTO pour la requête d'exécution de code.
 */
@Data
class ExecuteCodeRequest {
    private String code;
}

/**
 * DTO pour la requête de chat.
 */
@Data
class ChatRequest {
    private String message;
    private List<Message> history;
}

/**
 * DTO pour la requête de génération de code.
 */
@Data
class GenerateCodeRequest {
    private String task;
    private String context;
    private boolean executeImmediately = true;
}

@Data
class TemplateSearchRequest {
    private String query;
}