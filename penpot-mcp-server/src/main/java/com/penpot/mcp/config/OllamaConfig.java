package com.pnepot.mcp.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.ai.ollama.api.OllamaOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.*;

/**
 * Configuration Spring pour l'intégration avec Ollama AI.
 * <p>
 * Cette classe configure le client de chat AI en utilisant le modèle Ollama
 * spécifié dans les propriétés de l'application. Elle définit les options
 * par défaut pour les interactions avec le modèle, notamment la température
 * de génération.
 */
@Configuration
public class OllamaConfig {

    /**
     * Nom du modèle Ollama à utiliser, configuré via les propriétés Spring.
     * <p>
     * Cette valeur est injectée depuis {@code spring.ai.ollama.chat.options.model}
     * dans le fichier de configuration de l'application.
     * </p>
     */
    @Value("${spring.ai.ollama.chat.options.model}")
    private String modelName;

    /**
     * Crée et configure un bean ChatClient pour les interactions AI.
     * <p>
     * Le client est configuré avec:
     * </p>
     * <ul>
     *   <li>Le modèle Ollama spécifié dans les propriétés</li>
     *   <li>Une température de 0.7 pour équilibrer créativité et cohérence</li>
     * </ul>
     *
     * @param chatModel le modèle de chat Ollama injecté automatiquement
     * @return un client de chat configuré et prêt à l'emploi
     * @see OllamaChatModel
     * @see ChatClient
     */
    @Bean
    public ChatClient chatClient(OllamaChatModel chatModel) {
        return ChatClient.builder(chatModel)
                .defaultOptions(OllamaOptions.builder()
                    .withModel(modelName)
                    .withTemperature(0.7)
                    .build())
                .build();
    }
}