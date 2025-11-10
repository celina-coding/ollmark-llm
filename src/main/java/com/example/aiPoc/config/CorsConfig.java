package com.example.aiPoc.config;

import java.util.List;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

/**
 * Configuration de la politique CORS (Cross-Origin Resource Sharing) 
 * pour l'application.
 *
 * <p>Cette configuration permet aux clients externes (comme le plugin Penpot 
 * ou des applications locales de test) d'accéder aux endpoints du backend 
 * sans être bloqués par les restrictions de même origine imposées 
 * par les navigateurs.</p>
 *
 * <p>Les origines, méthodes HTTP et en-têtes autorisés sont explicitement 
 * définis afin d'assurer un contrôle précis sur les requêtes cross-origin.</p>
 */
@Configuration
public class CorsConfig {

    /**
     * Crée et configure un {@link CorsFilter} pour gérer les requêtes
     * cross-origin vers l'application.
     *
     * <p>Ce filtre applique les paramètres suivants :</p>
     * <ul>
     *   <li>Autorise les origines locales (Penpot et serveurs de test).</li>
     *   <li>Autorise les méthodes HTTP standard (GET, POST, PUT, DELETE, OPTIONS).</li>
     *   <li>Permet l’envoi de credentials (cookies, headers d’authentification).</li>
     *   <li>Définit un cache des requêtes préliminaires (preflight) d’une durée de 1 heure.</li>
     * </ul>
     *
     * @return une instance configurée de {@link CorsFilter}
     */
    @Bean
    public CorsFilter corsFilter() {
        CorsConfiguration config = new CorsConfiguration();

        // Origines autorisées (Penpot local + tests locaux)
        config.setAllowedOrigins(List.of(
            "http://localhost:61873",  // Plugin Penpot
            "http://localhost:8080",   // Application locale
            "http://127.0.0.1:8080"    // Alternative localhost
        ));

        // Méthodes HTTP autorisées
        config.setAllowedMethods(List.of(
            "GET", 
            "POST", 
            "PUT", 
            "DELETE", 
            "OPTIONS"
        ));

        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);

        return new CorsFilter(source);
    }
}