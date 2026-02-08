package com.penpot.ai.infrastructure.strategy;

import org.springframework.stereotype.Component;

/**
 * Formatter par défaut qui retourne toujours toString().
 * Utilisé comme fallback.
 */
@Component
public class DefaultResultFormatter implements ResultFormatter {

    @Override
    public String format(Object result) {
        return result != null ? result.toString() : "";
    }

    @Override
    public boolean supports(Class<?> resultType) {
        return true; // Supporte tout
    }

    @Override
    public int priority() {
        return 0; // Priorité la plus basse
    }
}