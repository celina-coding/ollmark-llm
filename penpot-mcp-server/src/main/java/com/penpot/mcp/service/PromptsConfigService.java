package com.penpot.mcp.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.yaml.snakeyaml.Yaml;
import jakarta.annotation.PostConstruct;
import java.io.*;
import java.util.Map;

/**
 * Service de configuration des prompts système.
 * <p>
 * Ce service charge et gère la configuration des prompts depuis prompts.yml,
 * fournissant un accès type-safe aux instructions système et autres
 * configurations avec gestion des valeurs par défaut.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PromptsConfigService {

    /** Service de documentation API pour l'injection de types */
    private final ApiDocsService apiDocsService;

    /** Configuration des prompts chargée depuis YAML */
    private Map<String, Object> promptsConfig;

    /**
     * Initialise le service en chargeant la configuration.
     * <p>
     * Appelé automatiquement après la construction du bean.
     */
    @PostConstruct
    public void init() {
        loadPromptsConfig();
    }

    /**
     * Charge la configuration des prompts depuis le fichier YAML.
     * <p>
     * Lit {@code classpath:data/prompts.yml} et met en cache les valeurs.
     * En cas d'erreur ou de fichier manquant, utilise une configuration par défaut.
     */
    private void loadPromptsConfig() {
        try {
            ClassPathResource resource = new ClassPathResource("data/prompts.yml");
            if (!resource.exists()) {
                log.warn("prompts.yml not found, using default configuration");
                promptsConfig = Map.of("initial_instructions", "");
                return;
            }

            Yaml yaml = new Yaml();
            try (InputStream inputStream = resource.getInputStream()) {
                @SuppressWarnings("unchecked")
                Map<String, Object> config = yaml.load(inputStream);
                if (config == null) {
                    log.warn("prompts.yml is empty, using default configuration");
                    promptsConfig = Map.of("initial_instructions", "");
                    return;
                }
                promptsConfig = config;
                log.info("Loaded prompts configuration from prompts.yml");
            }
        } catch (IOException e) {
            log.error("Failed to load prompts.yml, using default configuration", e);
            promptsConfig = Map.of("initial_instructions", "");
        }
    }

    /**
     * Obtient les instructions initiales pour le système AI.
     * <p>
     * Les instructions sont traitées pour remplacer la variable {@code $api_types}
     * par la liste complète des types API disponibles.
     *
     * @return les instructions système enrichies avec les noms de types API
     */
    public String getInitialInstructions() {
        String instructions = (String) promptsConfig.getOrDefault("initial_instructions", "");
        String apiTypesList = String.join(", ", apiDocsService.getTypeNames());
        instructions = instructions.replace("$api_types", apiTypesList);
        return instructions;
    }

    /**
     * Obtient une valeur de configuration par clé.
     *
     * @param key la clé de configuration
     * @return la valeur ou null si la clé n'existe pas
     */
    public Object getConfigValue(String key) {
        return promptsConfig.get(key);
    }

    /**
     * Recharge la configuration depuis le disque.
     * <p>
     * Force une nouvelle lecture du fichier, utile pour le développement
     * ou les mises à jour de configuration à chaud.
     */
    public void reloadConfiguration() {
        loadPromptsConfig();
        log.info("Configuration cache cleared and reloaded");
    }
}