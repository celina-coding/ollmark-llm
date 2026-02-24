package com.penpot.ai.infrastructure.config;

import com.penpot.ai.core.domain.TaskComplexity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import com.penpot.ai.core.domain.TaskComplexity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.ai.ollama.api.OllamaChatOptions;
import org.springframework.beans.factory.annotation.*;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.context.annotation.*;

import java.util.Map;

/**
 * Configuration Spring pour l'intégration avec Ollama AI.
 *
 * <h2>Trois profils d'options</h2>
 * <ul>
 *     <li><b>SIMPLE</b>   — température 0.1, déterministe, pour les opérations atomiques</li>
 *     <li><b>CREATIVE</b> — température 0.8, diversité, pour les suggestions esthétiques</li>
 *     <li><b>COMPLEX</b>  — thinking activé, température 0.6, pour les orchestrations complètes</li>
 * </ul>
 *
 * <h2>Cohabitation avec RouterConfig</h2>
 * <p>Depuis l'introduction du router ({@link RouterConfig}), deux ChatClient coexistent :</p>
 * <ul>
 *     <li>{@code executorChatClient} ({@code @Primary}) — défini ici, modèle qwen3:8b
 *         avec mémoire et factory de complexité.</li>
 *     <li>{@code routerChatClient} — défini dans {@link RouterConfig}, modèle phi3:mini
 *         sans mémoire, dédié à la classification d'intention.</li>
 * </ul>
 * <p>Le qualifier {@code "executorChatClient"} est utilisé dans {@code OllamaAiAdapter}
 * pour lever toute ambiguïté Spring lors de l'injection.</p>
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
@RefreshScope
public class OllamaConfig {

    @Value("${spring.ai.ollama.chat.options.model}")
    private String modelName;
    
    @Value("${spring.ai.ollama.chat.options.temperature:0.7}")
    private Double defaultTemperature;

    @Value("${spring.ai.ollama.chat.options.max-tokens:32000}")
    private Integer maxTokens;

    /**
     * Options pour les tâches SIMPLES : déterministe, faible créativité.
     * Exemples : changer une couleur, déplacer un élément, opacité.
     */
    @Bean("simpleOptions")
    @RefreshScope
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
    @RefreshScope
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
     * Options pour les tâches COMPLEXES : thinking activé, raisonnement profond.
     * Exemples : créer un design complet, orchestrer une séquence multi-étapes.
     *
     * <p>Le mode thinking ({@code enableThinking()}) permet au modèle d'exposer
     * son raisonnement interne avant de répondre, accessible via
     * {@code response.getResult().getMetadata().get("thinking")}.</p>
     */
    @Bean("complexOptions")
    @RefreshScope
    public OllamaChatOptions complexOptions() {
        log.info("Configuring COMPLEX ChatOptions (thinking enabled, temperature=0.6)");
        return OllamaChatOptions.builder()
            .model(modelName)
            .enableThinking()
            .temperature(0.6)
            .build();
    }

    /**
     * {@link ChatClient.Builder} de l'exécuteur (qwen3:8b).
     *
     * <p>Qualifié {@code "executorChatClientBuilder"} pour ne pas entrer en conflit
     * lors de l'injection. Configuré avec :</p>
     * <ul>
     *   <li>Options SIMPLE par défaut (surchargées par la factory selon la complexité)</li>
     *   <li>{@link MessageChatMemoryAdvisor} pour la mémoire conversationnelle</li>
     * </ul>
     *
     * @param chatModel    le modèle Ollama auto-configuré par Spring AI
     * @param memoryAdvisor l'advisor de mémoire configuré dans {@link ChatMemoryConfig}
     * @return builder pré-configuré pour l'exécuteur
     */
    @Bean("executorChatClientBuilder")
    @RefreshScope
    public ChatClient.Builder chatClientBuilder(
        OllamaChatModel chatModel,
        MessageChatMemoryAdvisor memoryAdvisor
    ) {
        log.info("Configuring executor ChatClient.Builder with model: {}", modelName);
        log.info("Configuring executor ChatClient.Builder with model: {}", modelName);
        return ChatClient.builder(chatModel)
            .defaultOptions(simpleOptions())
            .defaultOptions(simpleOptions())
            .defaultAdvisors(memoryAdvisor);
    }

    /**
     * Bean {@link ChatClient} exécuteur principal.
     *
     * <p>{@code @Primary} : injecté par défaut lorsqu'aucun qualifier n'est spécifié
     * (tests, beans tiers). Le router ({@code routerChatClient}) est toujours accédé
     * via {@code @Qualifier("routerChatClient")} et n'entre jamais en conflit.</p>
     *
     * <p>Qualifié {@code "executorChatClient"} pour permettre une injection explicite
     * dans {@code OllamaAiAdapter}.</p>
     *
     * @param builder le builder exécuteur qualifié
     * @return le client prêt à l'emploi
     * Bean {@link ChatClient} exécuteur principal.
     *
     * <p>{@code @Primary} : injecté par défaut lorsqu'aucun qualifier n'est spécifié
     * (tests, beans tiers). Le router ({@code routerChatClient}) est toujours accédé
     * via {@code @Qualifier("routerChatClient")} et n'entre jamais en conflit.</p>
     *
     * <p>Qualifié {@code "executorChatClient"} pour permettre une injection explicite
     * dans {@code OllamaAiAdapter}.</p>
     *
     * @param builder le builder exécuteur qualifié
     * @return le client prêt à l'emploi
     */
    @Bean("executorChatClient")
    @RefreshScope
    @Primary
    public ChatClient chatClient(
        @Qualifier("executorChatClientBuilder") ChatClient.Builder builder
    ) {
        log.info("Building executor ChatClient (@Primary, SIMPLE profile default)");
        return builder.build();
    }

    /**
     * Factory pour construire un {@link ChatClient} adapté à chaque niveau de complexité.
     *
     * <p>Utilisée par {@code OllamaAiAdapter} pour adapter dynamiquement les options
     * (température, thinking mode, topK) selon l'intention détectée. La factory réutilise
     * le même {@link OllamaChatModel} et le même {@link MessageChatMemoryAdvisor} que
     * l'exécuteur principal.</p>
     *
     * @param chatModel     le modèle Ollama auto-configuré
     * @param memoryAdvisor l'advisor de mémoire
     * @return factory typée {@link ChatClientFactory}
     */
    @Bean
    @RefreshScope
    public ChatClientFactory chatClientFactory(
        OllamaChatModel chatModel,
        MessageChatMemoryAdvisor memoryAdvisor,
        @Qualifier("simpleOptions") OllamaChatOptions simple,
        @Qualifier("creativeOptions") OllamaChatOptions creative,
        @Qualifier("complexOptions") OllamaChatOptions complex
    ) {
        Map<TaskComplexity, OllamaChatOptions> optionsMap = Map.of(
            TaskComplexity.SIMPLE, simple,
            TaskComplexity.CREATIVE, creative,
            TaskComplexity.COMPLEX, complex
        );

        return complexity -> {
            OllamaChatOptions opts = optionsMap.getOrDefault(complexity, simple);
            return ChatClient.builder(chatModel)
                .defaultOptions(opts)
                .defaultAdvisors(memoryAdvisor)
                .build();
        };
    }

    /**
     * Interface fonctionnelle pour la factory de {@link ChatClient} par complexité.
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