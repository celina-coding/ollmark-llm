package com.penpot.ai.shared.exception;

/**
 * Exception levée quand l'exécution d'une tâche échoue.
 */
public class TaskExecutionException extends PenpotAiException {
    public TaskExecutionException(String message) {
        super(message);
    }

    public TaskExecutionException(String message, Throwable cause) {
        super(message, cause);
    }
}