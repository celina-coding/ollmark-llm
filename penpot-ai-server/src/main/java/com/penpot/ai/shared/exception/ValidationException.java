package com.penpot.ai.shared.exception;

/**
 * Exception levée lors d'erreurs de validation.
 */
public class ValidationException extends PenpotAiException {
    public ValidationException(String message) {
        super(message);
    }
}