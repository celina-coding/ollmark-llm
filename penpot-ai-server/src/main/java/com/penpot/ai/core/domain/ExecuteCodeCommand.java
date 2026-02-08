package com.penpot.ai.core.domain;

import lombok.*;
import java.util.Optional;

/**
 * Command pour l'exécution de code JavaScript dans le plugin Penpot.
 * Implémente le Command Pattern pour encapsuler la requête.
 * Value Object immutable garantissant l'intégrité des données.
 */
@Value
@Builder
public class ExecuteCodeCommand {

    /**
     * Le code JavaScript à exécuter.
     */
    String code;

    /**
     * Token utilisateur optionnel pour le mode multi-utilisateur.
     */
    @Builder.Default
    Optional<String> userToken = Optional.empty();

    /**
     * Factory method pour créer une commande simple.
     * 
     * @param code le code à exécuter
     * @return la commande créée
     */
    public static ExecuteCodeCommand of(String code) {
        return ExecuteCodeCommand.builder()
            .code(code)
            .build();
    }

    /**
     * Factory method pour créer une commande avec token utilisateur.
     * 
     * @param code le code à exécuter
     * @param userToken le token utilisateur
     * @return la commande créée
     */
    public static ExecuteCodeCommand of(String code, String userToken) {
        return ExecuteCodeCommand.builder()
            .code(code)
            .userToken(Optional.ofNullable(userToken))
            .build();
    }

    /**
     * Valide que la commande est correcte.
     * 
     * @throws IllegalArgumentException si le code est null ou vide
     */
    public void validate() {
        if (code == null || code.isBlank()) {
            throw new IllegalArgumentException("Code cannot be null or empty");
        }
    }
}