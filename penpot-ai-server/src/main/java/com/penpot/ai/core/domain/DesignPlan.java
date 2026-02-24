package com.penpot.ai.core.domain;

import com.fasterxml.jackson.annotation.*;
import java.util.*;

/**
 * Plan de design structuré généré par l'IA via {@code entity(DesignPlan.class)}.
 *
 * <h2>Rôle</h2>
 * Ce record remplace le parsing de texte brut dans {@code OllamaAiAdapter}.
 * L'IA est contrainte de répondre en JSON conforme à ce schéma, ce qui permet
 * un traitement fiable et typé des intentions de design.
 *
 * <h2>Validation</h2>
 * Le {@code StructuredOutputValidationAdvisor} vérifie que la réponse respecte
 * le schéma JSON généré depuis ce record avant de le retourner.
 * En cas d'échec, il retente jusqu'à 3 fois en incluant les erreurs de validation
 * dans le prochain prompt.
 *
 * @param action          action principale à effectuer (ex: "create_social_media_post")
 * @param complexity      complexité estimée par l'IA (simple / creative / complex)
 * @param templateId      ID du template RAG à utiliser (peut être null si non applicable)
 * @param shapes          liste des formes à créer, dans l'ordre d'exécution
 * @param globalParameters paramètres globaux du design (dimensions, couleurs, etc.)
 * @param executionOrder  ordre textuel des étapes pour les logs et le debug
 * @param userFacingMessage message de confirmation à afficher à l'utilisateur
 */
public record DesignPlan(

    @JsonProperty(required = true, value = "action")
    @JsonPropertyDescription("Primary action: create_design, modify_element, search_template, explain")
    String action,

    @JsonProperty(required = true, value = "complexity")
    @JsonPropertyDescription("Estimated complexity: simple, creative, or complex")
    String complexity,

    @JsonProperty(value = "template_id")
    @JsonPropertyDescription("RAG template ID to use, null if not applicable")
    String templateId,

    @JsonProperty(required = true, value = "shapes")
    @JsonPropertyDescription("Ordered list of shapes to create or modify")
    List<ShapeInstruction> shapes,

    @JsonProperty(value = "global_parameters")
    @JsonPropertyDescription("Global design parameters: board dimensions, colors, fonts")
    Map<String, Object> globalParameters,

    @JsonProperty(required = true, value = "execution_order")
    @JsonPropertyDescription("Human-readable ordered steps for logging and debugging")
    List<String> executionOrder,

    @JsonProperty(required = true, value = "user_facing_message")
    @JsonPropertyDescription("Confirmation message to display to the user after execution")
    String userFacingMessage
) {
    /**
     * Instruction de création ou modification d'une forme Penpot.
     *
     * @param tool       nom du tool Spring AI à appeler (ex: "createRectangle", "createText")
     * @param name       nom descriptif de la forme
     * @param parameters paramètres du tool (x, y, width, height, fillColor, content, etc.)
     * @param dependsOn  noms de formes dont cette forme dépend (ex: elle doit être dans un board créé avant)
     */
    public record ShapeInstruction(

        @JsonProperty(required = true, value = "tool")
        @JsonPropertyDescription("Tool name: createBoard, createRectangle, createEllipse, createText, createStar")
        String tool,

        @JsonProperty(required = true, value = "name")
        @JsonPropertyDescription("Descriptive name for this shape")
        String name,

        @JsonProperty(required = true, value = "parameters")
        @JsonPropertyDescription("Tool parameters: x, y, width, height, fillColor, content, fontSize, etc.")
        Map<String, Object> parameters,

        @JsonProperty(value = "depends_on")
        @JsonPropertyDescription("Names of shapes that must be created before this one")
        List<String> dependsOn
    ) {}

    /**
     * Crée un DesignPlan vide pour les cas où l'IA ne retourne pas de plan structuré.
     * Utilisé comme fallback dans {@code OllamaAiAdapter}.
     *
     * @param message message d'explication à afficher à l'utilisateur
     * @return plan vide avec action "explain"
     */
    public static DesignPlan explain(String message) {
        return new DesignPlan(
            "explain",
            "simple",
            null,
            List.of(),
            Map.of(),
            List.of("1. Explain to user"),
            message
        );
    }

    /**
     * Vérifie si ce plan contient des formes à créer.
     *
     * @return true si au moins une forme est planifiée
     */
    public boolean hasShapes() {
        return shapes != null && !shapes.isEmpty();
    }

    /**
     * Vérifie si ce plan utilise un template RAG.
     *
     * @return true si un templateId est spécifié
     */
    public boolean hasTemplate() {
        return templateId != null && !templateId.isBlank();
    }
}