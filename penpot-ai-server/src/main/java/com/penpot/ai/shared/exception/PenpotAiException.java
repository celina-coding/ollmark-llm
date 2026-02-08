package com.penpot.ai.shared.exception;

/**
 * Exception de base pour les erreurs du domaine Penpot.
 * Suit le principe de hiérarchie d'exceptions claire.
 */
public class PenpotAiException extends RuntimeException {
    public PenpotAiException(String message) {
        super(message);
    }

    public PenpotAiException(String message, Throwable cause) {
        super(message, cause);
    }
}