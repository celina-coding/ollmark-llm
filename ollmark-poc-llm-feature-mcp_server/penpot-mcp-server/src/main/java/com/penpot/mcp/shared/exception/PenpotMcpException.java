package com.penpot.mcp.shared.exception;

/**
 * Exception de base pour les erreurs du domaine Penpot MCP.
 * Suit le principe de hiérarchie d'exceptions claire.
 */
public class PenpotMcpException extends RuntimeException {
    public PenpotMcpException(String message) {
        super(message);
    }

    public PenpotMcpException(String message, Throwable cause) {
        super(message, cause);
    }
}