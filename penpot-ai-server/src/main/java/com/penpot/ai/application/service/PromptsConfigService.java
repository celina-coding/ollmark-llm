package com.penpot.ai.application.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.yaml.snakeyaml.Yaml;
import jakarta.annotation.PostConstruct;
import java.io.*;
import java.util.Map;

/**
 * Service de configuration des prompts système.
 * Charge et gère la configuration des prompts depuis prompts.yml,
 * fournissant un accès type-safe aux instructions système et autres
 * configurations avec gestion des valeurs par défaut.
 */
@Slf4j
@Service
public class PromptsConfigService {

    private Map<String, Object> promptsConfig;

    @PostConstruct
    public void init() {
        loadPromptsConfig();
    }

    private void loadPromptsConfig() {
        try {
            ClassPathResource resource = new ClassPathResource("data/prompts.yml");
            if (!resource.exists()) {
                log.warn("prompts.yml not found, using default configuration");
                promptsConfig = Map.of("initial_instructions", getDefaultInstructions());
                return;
            }

            Yaml yaml = new Yaml();
            try (InputStream inputStream = resource.getInputStream()) {
                @SuppressWarnings("unchecked")
                Map<String, Object> config = yaml.load(inputStream);
                if (config == null) {
                    log.warn("prompts.yml is empty, using default configuration");
                    promptsConfig = Map.of("initial_instructions", getDefaultInstructions());
                    return;
                }
                promptsConfig = config;
                log.info("Loaded prompts configuration from prompts.yml");
            }
        } catch (IOException e) {
            log.error("Failed to load prompts.yml, using default configuration", e);
            promptsConfig = Map.of("initial_instructions", getDefaultInstructions());
        }
    }

    /**
     * Obtient les instructions initiales pour le système AI.
     * 
     * @return les instructions système
     */
    public String getInitialInstructions() {
        return (String) promptsConfig.getOrDefault(
            "initial_instructions", 
            getDefaultInstructions()
        );
    }

    public Object getConfigValue(String key) {
        return promptsConfig.get(key);
    }

    public void reloadConfiguration() {
        loadPromptsConfig();
        log.info("Configuration cache cleared and reloaded");
    }

    /**
     * Retourne les instructions par défaut si le fichier prompts.yml n'existe pas.
     */
    private String getDefaultInstructions() {
        return """
            You are an expert assistant for the Penpot Plugin API.

            Your role is to help users:
            - Generate JavaScript code for Penpot plugins

            Always provide clear, concise, and executable code examples.
            """;
    }
}