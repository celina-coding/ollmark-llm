package com.penpot.ai.infrastructure.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.ai.ollama.api.OllamaChatOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Configuration Spring pour l'intégration avec Ollama AI et Chat Memory.
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
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
     * Crée et configure un bean ChatClient.Builder avec mémoire de conversation.
     * 
     * Le builder est configuré avec :
     * - Le modèle Ollama et ses options
     * - Le MessageChatMemoryAdvisor pour la gestion automatique de l'historique
     * 
     * @param chatModel le modèle de chat Ollama injecté automatiquement
     * @param memoryAdvisor l'advisor de mémoire configuré
     * @return un builder de chat client configuré
     */
    @Bean
    public ChatClient.Builder chatClientBuilder(
        OllamaChatModel chatModel,
        MessageChatMemoryAdvisor memoryAdvisor
    ) {
        log.info("Configuring ChatClient with Ollama model: {}", modelName);
        log.info("Memory advisor enabled: {}", memoryAdvisor.getClass().getSimpleName());

        return ChatClient.builder(chatModel)
            .defaultOptions(OllamaChatOptions.builder()
                .model(modelName)
                .temperature(temperature)
                .build())
            .defaultAdvisors(memoryAdvisor);
    }

    /**
     * Crée le bean ChatClient par défaut.
     * Permet l'injection directe de ChatClient dans les services.
     * 
     * @param builder le builder configuré avec mémoire
     * @return le client de chat prêt à l'emploi
     */
    @Bean
    public ChatClient chatClient(ChatClient.Builder builder) {
        log.info("Building default ChatClient instance");
        return builder.build();
    }
}