package com.penpot.mcp.infrastructure.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.ai.ollama.api.OllamaOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.*;

/**
 * Configuration Spring pour l'intégration avec Ollama AI.
 * Configure le client de chat AI en utilisant le modèle Ollama spécifié.
 * Suit le principe de configuration centralisée.
 */
@Configuration
public class OllamaConfig {

    /**
     * Nom du modèle Ollama à utiliser.
     * Injecté depuis spring.ai.ollama.chat.options.model
     */
    @Value("${spring.ai.ollama.chat.options.model}")
    private String modelName;

    /**
     * Température pour la génération (créativité vs cohérence).
     * Injecté depuis spring.ai.ollama.chat.options.temperature
     */
    @Value("${spring.ai.ollama.chat.options.temperature:0.7}")
    private Double temperature;

    /**
     * Nombre maximum de tokens pour la génération.
     * Injecté depuis spring.ai.ollama.chat.options.max-tokens
     */
    @Value("${spring.ai.ollama.chat.options.max-tokens:4096}")
    private Integer maxTokens;

    /**
     * Crée et configure un bean ChatClient pour les interactions AI.
     * Le client est configuré avec le modèle Ollama et ses options.
     * 
     * @param chatModel le modèle de chat Ollama injecté automatiquement
     * @return un client de chat configuré et prêt à l'emploi
     */
    @Bean
    public ChatClient.Builder chatClientBuilder(OllamaChatModel chatModel) {
        return ChatClient.builder(chatModel)
            .defaultOptions(OllamaOptions.builder()
                .model(modelName)
                .temperature(temperature)
                .build());
    }

    /**
     * Crée le bean ChatClient par défaut.
     * Permet l'injection directe de ChatClient dans les services.
     * 
     * @param builder le builder configuré
     * @return le client de chat
     */
    @Bean
    public ChatClient chatClient(ChatClient.Builder builder) {
        return builder.build();
    }
}