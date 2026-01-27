package com.penpot.mcp.core.domain;

import lombok.*;
import java.util.Optional;

/**
 * Value Object représentant le résultat d'une génération de code.
 * Contient le code généré et optionnellement le résultat de son exécution.
 * Immutable pour garantir la cohérence des données.
 */
@Value
@Builder
public class GenerateCodeResult {

    /**
     * Le code JavaScript généré.
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
    public static GenerateCodeResult codeOnly(String code) {
        return GenerateCodeResult.builder()
            .generatedCode(code)
            .build();
    }

    /**
     * Factory method pour un résultat avec code et exécution.
     */
    public static GenerateCodeResult withExecution(String code, TaskResult executionResult) {
        return GenerateCodeResult.builder()
            .generatedCode(code)
            .executionResult(Optional.ofNullable(executionResult))
            .build();
    }
}