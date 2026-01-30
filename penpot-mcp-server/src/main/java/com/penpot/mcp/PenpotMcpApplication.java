package com.penpot.mcp;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * Classe principale de l'application Spring Boot pour le serveur MCP Penpot.
 * 
 * <h2>Architecture</h2>
 * Cette application implémente une architecture hexagonale (Ports & Adapters)
 * avec les principes SOLID :
 * 
 * <h3>Couches</h3>
 * <ul>
 *   <li><b>Core Domain</b> : Entités et Value Objects métier</li>
 *   <li><b>Ports</b> : Interfaces définissant les contrats</li>
 *   <li><b>Use Cases</b> : Logique métier orchestrée</li>
 *   <li><b>Adapters</b> : Implémentations des ports (REST, WebSocket, AI, etc.)</li>
 *   <li><b>Infrastructure</b> : Factories, Strategies, Chains, Configuration</li>
 * </ul>
 * 
 * <h3>Fonctionnalités</h3>
 * <ul>
 *   <li>Exécution de code JavaScript dans le plugin Penpot via WebSocket</li>
 *   <li>Assistance AI pour la génération de code Penpot</li>
 *   <li>Accès à la documentation de l'API Penpot</li>
 *   <li>Dialogue contextuel avec un assistant AI</li>
 *   <li>RAG pour templates marketing</li>
 *   <li>Support multi-utilisateur optionnel</li>
 * </ul>
 * 
 * @see com.penpot.mcp.core.ports.in Use Cases (ports d'entrée)
 * @see com.penpot.mcp.core.ports.out Ports de sortie
 * @see com.penpot.mcp.adapters Adapters (implémentations)
 * @see com.penpot.mcp.infrastructure Infrastructure (factories, strategies)
 */
@Slf4j
@SpringBootApplication
@EnableAsync
public class PenpotMcpApplication {

    /**
     * Point d'entrée de l'application.
     * 
     * @param args arguments de ligne de commande
     */
    public static void main(String[] args) {
        log.info("========================================");
        log.info("Starting Penpot MCP Server");
        log.info("Architecture: Hexagonal (Ports & Adapters)");
        log.info("========================================");

        SpringApplication.run(PenpotMcpApplication.class, args);

        log.info("========================================");
        log.info("Penpot MCP Server started successfully");
        log.info("WebSocket endpoint: /plugin");
        log.info("REST API: /mcp/*");
        log.info("========================================");
    }
}