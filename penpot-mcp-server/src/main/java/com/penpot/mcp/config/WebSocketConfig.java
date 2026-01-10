package com.penpot.mcp.config;

import com.penpot.mcp.service.PluginBridge;
import com.penpot.mcp.websocket.PluginWebSocketHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.*;
import jakarta.annotation.PostConstruct;

/**
 * Configuration WebSocket pour la communication avec le plugin Penpot.
 * <p>
 * Cette classe configure l'infrastructure WebSocket permettant la communication
 * bidirectionnelle en temps réel entre le serveur MCP et le plugin Penpot
 * s'exécutant dans le navigateur.
 * </p>
 * <p>
 * Elle résout également la dépendance circulaire entre {@link PluginWebSocketHandler}
 * et {@link PluginBridge} via l'injection post-construction.
 */
@Configuration
@EnableWebSocket
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketConfigurer {

    /** Handler pour les connexions WebSocket du plugin */
    private final PluginWebSocketHandler pluginWebSocketHandler;

    /** Service pont pour l'exécution des tâches dans le plugin */
    private final PluginBridge pluginBridge;

    /**
     * Port WebSocket configuré via les propriétés de l'application.
     * La valeur par défaut est 4402 si non spécifiée.
     */
    @Value("${penpot.mcp.websocket-port:4402}")
    private int websocketPort;

    /**
     * Résout la dépendance circulaire après construction des beans.
     * <p>
     * Cette méthode est appelée automatiquement après l'injection des dépendances
     * pour configurer le lien bidirectionnel entre le handler WebSocket et le pont plugin.
     */
    @PostConstruct
    public void init() {
        pluginWebSocketHandler.setPluginBridge(pluginBridge);
    }

    /**
     * Enregistre les handlers WebSocket avec leurs endpoints.
     * <p>
     * Configure l'endpoint {@code /plugin} pour accepter les connexions WebSocket
     * du plugin Penpot avec CORS désactivé (tous les origins acceptés).
     *
     * @param registry le registre pour enregistrer les handlers WebSocket
     */
    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(pluginWebSocketHandler, "/plugin")
                .setAllowedOrigins("*");
    }
}