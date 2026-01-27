package com.penpot.mcp.shared.exception;

/**
 * Exception levée quand une tâche dépasse le délai d'attente.
 */
public class TaskTimeoutException extends TaskExecutionException {
    public TaskTimeoutException(String message, Throwable cause) {
        super(message, cause);
    }
}