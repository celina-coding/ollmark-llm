package com.penpot.ai.shared.exception;

/**
 * Exception levée lors d'erreurs d'exécution d'outil.
 */
public class ToolExecutionException extends PenpotAiException {
    public ToolExecutionException(String message, Throwable cause) {
        super(message, cause);
    }
}