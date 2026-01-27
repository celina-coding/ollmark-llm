package com.penpot.mcp.shared.exception;

/**
 * Exception levée lors d'erreurs de validation.
 */
public class ValidationException extends PenpotMcpException {
    public ValidationException(String message) {
        super(message);
    }
}