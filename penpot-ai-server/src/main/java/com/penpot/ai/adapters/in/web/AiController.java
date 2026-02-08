package com.penpot.ai.adapters.in.web;

import com.penpot.ai.core.domain.*;
import com.penpot.ai.core.ports.in.*;
import lombok.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.*;

/**
 * Point d’entrée REST principal du module.
 * <p>
 * Ce contrôleur expose les endpoints HTTP permettant :
 * <ul>
 *     <li>d’exécuter du code JavaScript dans le contexte du plugin Penpot</li>
 *     <li>d’interagir avec un assistant IA conversationnel</li>
 *     <li>de gérer des conversations persistées via ChatMemory</li>
 * </ul>
 *
 * <h2>Architecture</h2>
 * <p>
 * Le contrôleur agit uniquement comme adaptateur HTTP (layer "in"),
 * déléguant toute la logique métier aux {@code UseCase} du cœur applicatif.
 */
@Slf4j
@RestController
@RequestMapping("/ai")
@RequiredArgsConstructor
public class AiController {

    /** Use case chargé de l’exécution de code JavaScript. */
    private final ExecuteCodeUseCase executeCodeUseCase;

    /** Use case gérant les conversations IA et la mémoire de chat. */
    private final ConversationChatUseCase conversationChatUseCase;

    /**
     * <p>
     * Exécute du code JavaScript dans le contexte du plugin Penpot.
     * </p>
     * <p><b>
     * Ne servira que lors de la phase de LEAN.
     * </b></p>
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
            log.info("POST /ai/execute-code (code length: {} chars)", 
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
            log.info("POST /ai/chat (conversationId: {}, message length: {} chars)", 
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
            log.info("POST /ai/chat/new (userId: {})", userId != null ? userId : "anonymous");

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
            log.info("DELETE /ai/chat/{}", conversationId);
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