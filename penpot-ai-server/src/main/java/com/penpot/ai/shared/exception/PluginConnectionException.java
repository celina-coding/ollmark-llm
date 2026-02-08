package com.penpot.ai.shared.exception;

/**
 * Exception levée quand aucune connexion plugin n'est disponible.
 */
public class PluginConnectionException extends PenpotAiException {
    public PluginConnectionException(String message) {
        super(message);
    }
}