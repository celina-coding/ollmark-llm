package com.penpot.mcp;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.web.socket.config.annotation.EnableWebSocket;

/**
 * Classe principale de l'application Spring Boot pour le serveur MCP Penpot.
 * <p>
 * Cette application fournit un serveur MCP (Model Context Protocol) permettant:
 * </p>
 * <ul>
 *   <li>L'exécution de code JavaScript dans le plugin Penpot via WebSocket</li>
 *   <li>L'assistance AI pour la génération de code Penpot</li>
 *   <li>L'accès à la documentation de l'API Penpot</li>
 *   <li>Le dialogue contextuel avec un assistant AI</li>
 * </ul>
 * <p>
 * Architecture:
 * </p>
 * <ul>
 *   <li><b>Controllers</b>: Exposent les API REST MCP</li>
 *   <li><b>Services</b>: Logique métier (AI, pont plugin, documentation)</li>
 *   <li><b>WebSocket</b>: Communication temps réel avec le plugin</li>
 *   <li><b>Tools</b>: Outils de haut niveau pour les opérations courantes</li>
 * </ul>
 */
@SpringBootApplication
@EnableWebSocket
@EnableAsync
public class PenpotMcpApplication {

    /**
     * Point d'entrée de l'application.
     *
     * @param args arguments de ligne de commande
     */
    public static void main(String[] args) {
        SpringApplication.run(PenpotMcpApplication.class, args);
    }
}