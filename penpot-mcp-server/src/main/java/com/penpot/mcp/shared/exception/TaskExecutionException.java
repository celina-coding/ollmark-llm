package com.penpot.mcp.shared.exception;

/**
 * Exception levée quand l'exécution d'une tâche échoue.
 */
public class TaskExecutionException extends PenpotMcpException {
    public TaskExecutionException(String message) {
        super(message);
    }

    public TaskExecutionException(String message, Throwable cause) {
        super(message, cause);
    }
}