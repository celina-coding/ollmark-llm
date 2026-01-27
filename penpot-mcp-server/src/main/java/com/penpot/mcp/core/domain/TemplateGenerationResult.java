package com.penpot.mcp.core.domain;

import lombok.*;
import java.util.Optional;

/**
 * Value Object représentant le résultat de la génération d'un template.
 * Contient le code généré depuis la design_recipe et optionnellement
 * le résultat de son exécution dans Penpot.
 * Immutable pour garantir la cohérence des données.
 */
@Value
@Builder
public class TemplateGenerationResult {
    
    /**
     * L'ID du template utilisé.
     */
    String templateId;
    
    /**
     * Le code JavaScript généré depuis la design_recipe.
     */
    String generatedCode;
    
    /**
     * Le résultat de l'exécution du code (si exécuté).
     */
    @Builder.Default
    Optional<TaskResult> executionResult = Optional.empty();
    
    /**
     * Factory method pour un résultat avec code uniquement.
     */
    public static TemplateGenerationResult codeOnly(String templateId, String code) {
        return TemplateGenerationResult.builder()
            .templateId(templateId)
            .generatedCode(code)
            .build();
    }
    
    /**
     * Factory method pour un résultat avec code et exécution.
     */
    public static TemplateGenerationResult withExecution(
        String templateId, 
        String code, 
        TaskResult executionResult
    ) {
        return TemplateGenerationResult.builder()
            .templateId(templateId)
            .generatedCode(code)
            .executionResult(Optional.ofNullable(executionResult))
            .build();
    }
}