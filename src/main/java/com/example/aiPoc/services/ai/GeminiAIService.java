package com.example.aiPoc.services.ai;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.google.genai.Client;
import com.google.genai.types.GenerateContentConfig;
import com.google.genai.types.GenerateContentResponse;

/**
 * Service d’intégration du modèle Gemini utilisant le SDK Google Gen AI (v1.17.0).
 * 
 * <p>Ce service reproduit la structure de {@link OpenAIService} :
 * - injection du modèle au constructeur ;
 * - configuration dynamique (maxTokens, temperature) ;
 * - gestion d’exceptions et journalisation.</p>
 *
 * @see AIService
 * @see OpenAIService
 */
@Service
public class GeminiAIService implements AIService {

    private static final Logger logger = LoggerFactory.getLogger(GeminiAIService.class);

    /** Client Google Gen AI pour communiquer avec les modèles Gemini. */
    private final Client client;
    private final String model;

    /**
     * Initialise le service Gemini avec le modèle souhaité.
     *
     * @param modelName nom du modèle Gemini (ex. {@code gemini-2.5-flash})
     */
    public GeminiAIService(
        @Value("${gemini.api-key}") String apiKey,
        @Value("${gemini.model}") String model
    ) {
        this.model = model;
        this.client = new Client.Builder()
                .apiKey(apiKey)
                .build();
    }

    @Override
    public String chat(String prompt) {
        return chat(prompt, 4096, 0.5);
    }

    @Override
    public String chat(String prompt, int maxTokens, double temperature) {
        logger.debug("Envoi du prompt à Gemini ({} caractères, maxTokens={}, temperature={})",
                prompt.length(), maxTokens, temperature);

        try {
            long startTime = System.currentTimeMillis();

            // Configuration de génération (temperature en float)
            GenerateContentConfig config = GenerateContentConfig.builder()
                    .maxOutputTokens(maxTokens)
                    .temperature((float) temperature)
                    .build();

            // Envoi du prompt au modèle
            GenerateContentResponse response = client.models
                    .generateContent(model, prompt, config);

            String text = response.text() != null ? response.text().trim() : "";

            long duration = System.currentTimeMillis() - startTime;
            logger.debug("Réponse Gemini reçue en {} ms ({} caractères)", duration, text.length());

            return text;

        } catch (Exception e) {
            logger.error("Erreur lors de la génération Gemini", e);
            throw new RuntimeException("Échec de génération Gemini : " + e.getMessage(), e);
        }
    }
}