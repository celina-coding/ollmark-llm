package com.penpot.mcp.shared.exception;

/**
 * Exception levée lors d'erreurs d'exécution d'outil.
 */
public class ToolExecutionException extends PenpotMcpException {
    public ToolExecutionException(String message, Throwable cause) {
        super(message, cause);
    }
}