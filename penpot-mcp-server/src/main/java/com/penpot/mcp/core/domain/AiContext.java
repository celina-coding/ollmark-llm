package com.penpot.mcp.core.domain;

import lombok.*;
import java.util.*;

/**
 * Value Object représentant le contexte pour les opérations AI.
 * Immutable avec support pour la modification via méthodes with*().
 * Utilisé par la chaîne d'enrichissement (Chain of Responsibility).
 */
@Value
@Builder
@With
public class AiContext {

    /**
     * La tâche ou requête principale.
     */
    String task;

    /**
     * Contexte additionnel fourni par l'utilisateur.
     */
    @Builder.Default
    String userContext = "";

    /**
     * Indique si c'est une génération de code.
     */
    @Builder.Default
    boolean codeGeneration = false;

    /**
     * Documentation API associée (type -> documentation).
     */
    @Builder.Default
    Map<String, String> apiDocumentation = Collections.emptyMap();

    /**
     * Exemples de code pertinents.
     */
    @Builder.Default
    List<String> examples = Collections.emptyList();

    /**
     * Best practices à appliquer.
     */
    @Builder.Default
    List<String> bestPractices = Collections.emptyList();

    /**
     * Contraintes à respecter.
     */
    @Builder.Default
    List<String> constraints = Collections.emptyList();

    /**
     * Historique de conversation.
     */
    @Builder.Default
    List<String> conversationHistory = Collections.emptyList();

    /**
     * Factory method pour créer un contexte de génération de code.
     */
    public static AiContext forCodeGeneration(String task, String userContext) {
        return AiContext.builder()
            .task(task)
            .userContext(userContext)
            .codeGeneration(true)
            .build();
    }
    
    /**
     * Factory method pour créer un contexte de chat.
     */
    public static AiContext forChat(String message) {
        return AiContext.builder()
            .task(message)
            .codeGeneration(false)
            .build();
    }
    
    /**
     * Factory method depuis une chaîne de contexte.
     */
    public static AiContext from(String context) {
        return AiContext.builder()
            .userContext(context != null ? context : "")
            .build();
    }

    /**
     * Ajoute la documentation API au contexte.
     * 
     * @param documentation map type -> doc
     * @return nouveau contexte avec la documentation
     */
    public AiContext withApiDocumentation(Map<String, String> documentation) {
        Map<String, String> newDocs = new HashMap<>(this.apiDocumentation);
        newDocs.putAll(documentation);
        return this.withApiDocumentation(Collections.unmodifiableMap(newDocs));
    }

    /**
     * Ajoute des exemples de code.
     * 
     * @param newExamples exemples à ajouter
     * @return nouveau contexte avec les exemples
     */
    public AiContext withExamples(List<String> newExamples) {
        List<String> combined = new ArrayList<>(this.examples);
        combined.addAll(newExamples);
        return this.withExamples(Collections.unmodifiableList(combined));
    }

    /**
     * Ajoute les best practices.
     * 
     * @param practices best practices à ajouter
     * @return nouveau contexte
     */
    public AiContext withBestPractices(List<String> practices) {
        return this.withBestPractices(
            Collections.unmodifiableList(new ArrayList<>(practices))
        );
    }

    /**
     * Ajoute les contraintes.
     * 
     * @param newConstraints contraintes à ajouter
     * @return nouveau contexte
     */
    public AiContext withConstraints(List<String> newConstraints) {
        return this.withConstraints(
            Collections.unmodifiableList(new ArrayList<>(newConstraints))
        );
    }

    /**
     * Remplace l'historique par une version tronquée.
     * 
     * @param trimmedHistory historique tronqué
     * @return nouveau contexte
     */
    public AiContext withTrimmedHistory(List<String> trimmedHistory) {
        return this.withConversationHistory(
            Collections.unmodifiableList(new ArrayList<>(trimmedHistory))
        );
    }

    /**
     * Vérifie si le contexte requiert des exemples.
     * 
     * @return true si des exemples seraient utiles
     */
    public boolean requiresExamples() {
        // Heuristique: si la tâche contient certains mots-clés
        String taskLower = task.toLowerCase();
        return taskLower.contains("create") 
            || taskLower.contains("make") 
            || taskLower.contains("build")
            || taskLower.contains("draw");
    }

    /**
     * Construit le prompt complet en combinant tous les éléments.
     * 
     * @return le prompt formaté
     */
    public String buildFullPrompt() {
        StringBuilder prompt = new StringBuilder();

        // Tâche principale
        prompt.append("Task: ").append(task).append("\n\n");

        // Contexte utilisateur
        if (!userContext.isBlank()) {
            prompt.append("Context: ").append(userContext).append("\n\n");
        }

        // Documentation API
        if (!apiDocumentation.isEmpty()) {
            prompt.append("API Documentation:\n");
            apiDocumentation.forEach((type, doc) -> {
                prompt.append("- ").append(type).append(": ").append(doc).append("\n");
            });
            prompt.append("\n");
        }

        // Exemples
        if (!examples.isEmpty()) {
            prompt.append("Examples:\n");
            examples.forEach(example -> {
                prompt.append(example).append("\n\n");
            });
        }

        // Best Practices
        if (!bestPractices.isEmpty()) {
            prompt.append("Best Practices:\n");
            bestPractices.forEach(practice -> {
                prompt.append("- ").append(practice).append("\n");
            });
            prompt.append("\n");
        }

        // Contraintes
        if (!constraints.isEmpty()) {
            prompt.append("Constraints:\n");
            constraints.forEach(constraint -> {
                prompt.append("- ").append(constraint).append("\n");
            });
            prompt.append("\n");
        }

        return prompt.toString();
    }
}