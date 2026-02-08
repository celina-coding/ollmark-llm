package com.penpot.ai.infrastructure.strategy;

import org.springframework.stereotype.Component;

/**
 * Formatter texte simple pour les types primitifs et String.
 */
@Component
public class TextResultFormatter implements ResultFormatter {

    @Override
    public String format(Object result) {
        if (result == null) return "";
        return result.toString();
    }

    @Override
    public boolean supports(Class<?> resultType) {
        return String.class.equals(resultType) 
            || Number.class.isAssignableFrom(resultType)
            || Boolean.class.equals(resultType)
            || resultType.isPrimitive();
    }

    @Override
    public int priority() {
        return 5; // Priorité moyenne
    }
}