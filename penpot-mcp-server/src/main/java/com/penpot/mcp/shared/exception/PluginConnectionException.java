package com.penpot.mcp.shared.exception;

/**
 * Exception levée quand aucune connexion plugin n'est disponible.
 */
public class PluginConnectionException extends PenpotMcpException {
    public PluginConnectionException(String message) {
        super(message);
    }
}