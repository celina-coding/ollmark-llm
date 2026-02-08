package com.penpot.ai;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@Slf4j
@SpringBootApplication
@EnableAsync
public class PenpotAiApplication {

    /**
     * Point d'entrée de l'application.
     * 
     * @param args arguments de ligne de commande
     */
    public static void main(String[] args) {
        log.info("========================================");
        log.info("  Démarrage du serveur Penpot Serveur");
        log.info("========================================");

        SpringApplication.run(PenpotAiApplication.class, args);

        log.info("========================================");
        log.info("Le serveur Penpot a démarré correctement");
        log.info("========================================");
    }
}