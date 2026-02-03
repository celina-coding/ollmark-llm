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
     * Historique de conversation.
     */
    @Builder.Default
    List<String> conversationHistory = Collections.emptyList();

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