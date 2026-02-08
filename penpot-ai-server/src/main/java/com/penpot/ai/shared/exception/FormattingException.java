package com.penpot.ai.shared.exception;

/**
 * Exception levée lors d'erreurs de formatage de résultat.
 */
public class FormattingException extends PenpotAiException {
    public FormattingException(String message, Throwable cause) {
        super(message, cause);
    }
}