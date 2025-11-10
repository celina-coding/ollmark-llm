package com.example.aiPoc.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Configuration Spring MVC de l'application.
 *
 * <p>Cette classe définit la configuration du routage des vues web 
 * et la gestion des ressources statiques (fichiers CSS, JavaScript, etc.).</p>
 *
 * <p>Elle complète la configuration automatique de Spring Boot en 
 * permettant de rediriger certaines routes vers des fichiers statiques 
 * et d’ajouter manuellement des gestionnaires de ressources si nécessaire.</p>
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        registry.addViewController("/")
                .setViewName("forward:/index.html");
    }
}