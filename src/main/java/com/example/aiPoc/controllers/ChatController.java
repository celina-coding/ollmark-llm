package com.example.aiPoc.controllers;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.aiPoc.dto.request.CodeGenerationRequest;
import com.example.aiPoc.services.ai.AIService;

/**
 * Contrôleur REST gérant les interactions de type "chat" avec le service d'intelligence artificielle.
 *
 * <p>Ce contrôleur expose un point d'entrée HTTP permettant d'envoyer un prompt texte 
 * et de recevoir une réponse générée par l'IA. Il inclut un mécanisme de validation 
 * du prompt et un nettoyage des réponses avant de les renvoyer au client.</p>
 *
 * <p>Les origines autorisées pour les requêtes CORS sont :
 * <ul>
 *   <li><code>http://localhost:61873</code></li>
 *   <li><code>http://localhost:8080</code></li>
 * </ul>
 * </p>
 */
@CrossOrigin(origins = {"http://localhost:61873", "http://localhost:8080"})
@RestController
@RequestMapping("/api/chat")
public class ChatController {

    /** Logger pour le suivi des requêtes et la journalisation des erreurs. */
    private static final Logger logger = LoggerFactory.getLogger(ChatController.class);

    /** Service central gérant les interactions avec le modèle d'intelligence artificielle. */
    private final AIService aiService;

    /**
     * Constructeur injectant le service d'IA.
     *
     * @param aiService service responsable de la communication avec le modèle IA
     */
    public ChatController(AIService aiService) {
        this.aiService = aiService;
    }

    /**
     * Endpoint principal permettant à un client d'envoyer un prompt à l'IA et de recevoir une réponse.
     *
     * <p>Cette méthode :
     * <ul>
     *   <li>Valide le contenu du prompt utilisateur,</li>
     *   <li>Transmet le prompt au service IA,</li>
     *   <li>Nettoie la réponse renvoyée (suppression des balises techniques comme &lt;think&gt;),</li>
     *   <li>Retourne la réponse prête à être affichée.</li>
     * </ul>
     * </p>
     *
     * @param request l’objet contenant le prompt utilisateur à transmettre à l’IA
     * @return une {@link ResponseEntity} contenant :
     *         <ul>
     *           <li>la clé <code>"response"</code> avec la réponse nettoyée si la requête réussit,</li>
     *           <li>ou la clé <code>"error"</code> avec un message d’erreur en cas d’échec.</li>
     *         </ul>
     */
    @PostMapping
    public ResponseEntity<Map<String, String>> chat(@RequestBody CodeGenerationRequest request) {
        logger.debug("Réception requête chat: {}", request.getPrompt());

        try {
            String prompt = request.getPrompt();

            if (prompt == null || prompt.trim().isEmpty()) {
                return ResponseEntity.badRequest()
                    .body(Map.of("error", "Le prompt ne peut pas être vide"));
            }

            String response = aiService.chat(prompt);
            String cleanedResponse = cleanResponse(response);

            logger.debug("Réponse générée: {} caractères", cleanedResponse.length());

            return ResponseEntity.ok(Map.of("response", cleanedResponse));
        } catch (Exception e) {
            logger.error("Erreur lors du chat", e);
            return ResponseEntity.internalServerError()
                .body(Map.of("error", "Erreur lors de la génération: " + e.getMessage()));
        }
    }

    /**
     * Nettoie le texte renvoyé par l'IA en supprimant les balises et espaces indésirables.
     *
     * <p>Les opérations de nettoyage comprennent :
     * <ul>
     *   <li>Suppression des blocs <code>&lt;think&gt;...&lt;/think&gt;</code>,</li>
     *   <li>Réduction des espaces multiples en un seul,</li>
     *   <li>Suppression des espaces en début et fin de texte.</li>
     * </ul>
     * </p>
     *
     * @param response la réponse brute renvoyée par le modèle IA
     * @return la réponse nettoyée prête à être renvoyée au client
     */
    private String cleanResponse(String response) {
        if (response == null) return "";

        response = response.replaceAll("(?s)<think>.*?</think>", "").trim();
        response = response.replaceAll("\\s+", " ");

        return response.trim();
    }
}