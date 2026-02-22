package com.penpot.ai.infrastructure.config;

import com.penpot.ai.core.domain.TaskComplexity;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.ai.ollama.api.OllamaChatOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;

/**
 * Configuration Spring pour l'intégration avec Ollama AI.
 *
 * <h2>Trois profils d'options</h2>
 * <ul>
 *     <li><b>SIMPLE</b>  — température 0.1, déterministe, pour les opérations atomiques</li>
 *     <li><b>CREATIVE</b> — température 0.8, diversité, pour les suggestions esthétiques</li>
 *     <li><b>COMPLEX</b>  — thinking activé, température 0.6, pour les orchestrations complètes</li>
 * </ul>
 *
 * <p>Le {@link ChatClient.Builder} de base est configuré avec les options DEFAULT (SIMPLE).
 * {@link com.penpot.ai.adapters.out.ai.OllamaAiAdapter} utilise
 * {@link #buildChatClientForComplexity(TaskComplexity)} pour obtenir un client adapté
 * à chaque requête.</p>
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class OllamaConfig {

    @Value("${spring.ai.ollama.chat.options.model}")
    private String modelName;

    @Value("${spring.ai.ollama.chat.options.temperature:0.7}")
    private Double defaultTemperature;

    @Value("${spring.ai.ollama.chat.options.max-tokens:32000}")
    private Integer maxTokens;

    // ==================== OPTIONS PAR PROFIL ====================

    /**
     * Options pour les tâches SIMPLES : déterministe, faible créativité.
     * Exemples : changer une couleur, déplacer un élément, opacité.
     */
    @Bean("simpleOptions")
    public OllamaChatOptions simpleOptions() {
        log.info("Configuring SIMPLE ChatOptions (temperature=0.1, topK=10)");
        return OllamaChatOptions.builder()
            .model(modelName)
            .temperature(0.1)
            .topK(10)
            .build();
    }

    /**
     * Options pour les tâches CRÉATIVES : diversité élevée, exploration.
     * Exemples : suggérer un layout, proposer une palette de couleurs.
     */
    @Bean("creativeOptions")
    public OllamaChatOptions creativeOptions() {
        log.info("Configuring CREATIVE ChatOptions (temperature=0.8, topK=40, topP=0.9)");
        return OllamaChatOptions.builder()
            .model(modelName)
            .temperature(0.8)
            .topK(40)
            .topP(0.9)
            .build();
    }

    /**
     * Options pour les tâches COMPLEXES : thinking activé, raisonnement profond.
     * Exemples : créer un design complet, orchestrer une séquence multi-étapes.
     *
     * <p>Le mode thinking ({@code enableThinking()}) permet au modèle d'exposer
     * son raisonnement interne avant de répondre, accessible via
     * {@code response.getResult().getMetadata().get("thinking")}.</p>
     */
    @Bean("complexOptions")
    public OllamaChatOptions complexOptions() {
        log.info("Configuring COMPLEX ChatOptions (thinking enabled, temperature=0.6)");
        return OllamaChatOptions.builder()
            .model(modelName)
            .enableThinking()
            .temperature(0.6)
            .build();
    }

    // ==================== CHAT CLIENT ====================

    /**
     * {@link ChatClient.Builder} par défaut (profil SIMPLE).
     * Configuré avec le {@link MessageChatMemoryAdvisor} pour la mémoire conversationnelle.
     *
     * <p>Ce bean est utilisé comme base par {@link #chatClient(ChatClient.Builder)},
     * et également comme source pour créer des builders dérivés via
     * {@link ChatClient.Builder#mutate()} dans {@code OllamaAiAdapter}.</p>
     */
    @Bean
    public ChatClient.Builder chatClientBuilder(
        OllamaChatModel chatModel,
        MessageChatMemoryAdvisor memoryAdvisor
    ) {
        log.info("Configuring default ChatClient.Builder with model: {}", modelName);
        return ChatClient.builder(chatModel)
            .defaultOptions(simpleOptions())
            .defaultAdvisors(memoryAdvisor);
    }

    /**
     * Bean {@link ChatClient} par défaut — utilisé pour les appels SIMPLES.
     */
    @Bean
    public ChatClient chatClient(ChatClient.Builder builder) {
        log.info("Building default ChatClient instance (SIMPLE profile)");
        return builder.build();
    }

    // ==================== FACTORY PAR COMPLEXITÉ ====================

    /**
     * Factory exposée en tant que bean Spring pour permettre à {@code OllamaAiAdapter}
     * de construire un {@link ChatClient} adapté à chaque niveau de complexité.
     *
     * @param chatModel    le modèle Ollama auto-configuré
     * @param memoryAdvisor l'advisor de mémoire
     * @return une factory typée
     */
    @Bean
    public ChatClientFactory chatClientFactory(
        OllamaChatModel chatModel,
        MessageChatMemoryAdvisor memoryAdvisor
    ) {
        Map<TaskComplexity, OllamaChatOptions> optionsMap = Map.of(
            TaskComplexity.SIMPLE,   simpleOptions(),
            TaskComplexity.CREATIVE, creativeOptions(),
            TaskComplexity.COMPLEX,  complexOptions()
        );

        return complexity -> {
            OllamaChatOptions opts = optionsMap.getOrDefault(complexity, simpleOptions());
            log.debug("Building ChatClient for complexity={} (options={})", complexity, opts);
            return ChatClient.builder(chatModel)
                .defaultOptions(opts)
                .defaultAdvisors(memoryAdvisor)
                .build();
        };
    }

    // ==================== INTERFACE INTERNE ====================

    /**
     * Interface fonctionnelle pour la factory de {@link ChatClient} par complexité.
     * Permet l'injection et le mock en tests.
     */
    @FunctionalInterface
    public interface ChatClientFactory {
        /**
         * Construit un {@link ChatClient} configuré pour le niveau de complexité donné.
         *
         * @param complexity le niveau de complexité
         * @return le client configuré
         */
        ChatClient buildForComplexity(TaskComplexity complexity);
    }
}