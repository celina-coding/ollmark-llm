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
     * Ajoute les contraintes.
     * 
     * @param newConstraints contraintes à ajouter
     * @return nouveau contexte
     */
    public AiContext withTrimmedHistory(List<String> trimmedHistory) {
        return this.withConversationHistory(
            Collections.unmodifiableList(new ArrayList<>(trimmedHistory))
        );
    }

    /**
     * Construit le prompt utilisateur.
     * 
     * @return le prompt formaté
     */
    public String buildUserPrompt() {
        StringBuilder prompt = new StringBuilder();
        prompt.append("Task: ").append(task);
        if (!userContext.isBlank()) prompt.append("\n\nContext: ").append(userContext);
        return prompt.toString();
    }
}