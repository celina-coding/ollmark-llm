package com.penpot.ai.infrastructure.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.ai.ollama.api.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.*;

/**
 * Configuration du ChatClient dédié au routing d'intention.
 *
 * <h2>Séparation des responsabilités</h2>
 * Cette classe est délibérément séparée de {@link OllamaConfig} pour respecter SRP :
 * {@code OllamaConfig} configure l'exécuteur (qwen3:8b avec mémoire),
 * {@code RouterConfig} configure le classifieur (phi3:mini sans mémoire).
 *
<<<<<<< HEAD
=======
 * <h2>Pourquoi phi3:mini pour le router ?</h2>
 * <ul>
 *   <li><b>Latence</b> : 3.8B params → ~100-200ms vs ~800ms+ pour qwen3:8b</li>
 *   <li><b>Classification</b> : phi3:mini est excellent sur les tâches de catégorisation
 *       courtes (instruction-tuned par Microsoft)</li>
 *   <li><b>Coût mémoire GPU</b> : charge minimale, laisse de la VRAM à l'exécuteur</li>
 * </ul>
 *
>>>>>>> feature/modification-contenu
 * <h2>Paramètres intentionnels</h2>
 * <ul>
 *   <li>{@code temperature=0.0} : classification déterministe, pas de créativité</li>
 *   <li>{@code numPredict=150} : une réponse JSON courte suffit, limite les tokens gaspillés</li>
 *   <li>Pas de {@code MessageChatMemoryAdvisor} : le router ne doit pas avoir de mémoire
 *       (chaque requête est indépendante)</li>
 * </ul>
<<<<<<< HEAD
=======
 *
 * <h2>Idée d'amélioration future</h2>
 * Ajouter un {@code SimpleLoggerAdvisor} conditionnel sur profil {@code dev}
 * pour loguer toutes les requêtes/réponses router en debug.
>>>>>>> feature/modification-contenu
 */
@Slf4j
@Configuration
public class RouterConfig {

    /**
     * Modèle utilisé pour le routing.
     * Configurable via {@code penpot.ai.router.model} dans application.properties.
     * Défaut : {@code phi3:mini} (3.8B params, excellent classifieur).
     */
    @Value("${penpot.ai.router.model:phi3:mini}")
    private String routerModel;

    /**
     * Crée une instance dédiée de {@link OllamaChatModel} pour phi3:mini.
     *
     * @param ollamaApi l'API Ollama partagée, injectée automatiquement par Spring AI
     * @return un ChatClient léger dédié à la classification
     */
    @Bean("routerChatClient")
    public ChatClient routerChatClient(OllamaApi ollamaApi) {
        log.info("[RouterConfig] Configuring router ChatClient with model: {}", routerModel);

        OllamaChatOptions routerOptions = OllamaChatOptions.builder()
            .model(routerModel)
            .temperature(0.0)
            .numPredict(150)
            .build();

        OllamaChatModel routerModel = OllamaChatModel.builder()
            .ollamaApi(ollamaApi)
            .defaultOptions(routerOptions)
            .build();

        log.info("[RouterConfig] Router ChatClient ready (model={}, temperature=0.0, numPredict=150)",
            this.routerModel);

        return ChatClient.builder(routerModel).build();
    }
}