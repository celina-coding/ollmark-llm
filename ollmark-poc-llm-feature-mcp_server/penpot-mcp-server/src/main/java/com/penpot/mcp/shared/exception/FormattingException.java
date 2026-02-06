package com.penpot.mcp.shared.exception;

/**
 * Exception levée lors d'erreurs de formatage de résultat.
 */
public class FormattingException extends PenpotMcpException {
    public FormattingException(String message, Throwable cause) {
        super(message, cause);
    }
}