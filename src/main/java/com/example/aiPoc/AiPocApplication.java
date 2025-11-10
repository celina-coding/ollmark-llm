package com.example.aiPoc;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Point d’entrée principal de l’application <b>AI PoC</b> (Proof of Concept).
 *
 * <p>
 * Cette classe initialise et démarre le contexte Spring Boot.
 * Elle sert de configuration principale grâce à l’annotation
 * {@link SpringBootApplication}, qui active :
 * <ul>
 *   <li>La configuration automatique de Spring Boot</li>
 *   <li>Le scan des composants dans le package racine {@code com.example.aiPoc}</li>
 *   <li>La possibilité de définir des beans personnalisés</li>
 * </ul>
 * </p>
 */
@SpringBootApplication
public class AiPocApplication {

    /**
     * Méthode principale de l’application, appelée au démarrage.
     *
     * <p>
     * Elle lance le contexte Spring Boot en initialisant tous les composants,
     * contrôleurs, services et configurations définis dans le projet.
     * </p>
     *
     * @param args les arguments de ligne de commande passés au démarrage.
     */
	public static void main(String[] args) {
		SpringApplication.run(AiPocApplication.class, args);
	}
}