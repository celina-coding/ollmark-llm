package com.example.aiPoc.services.ai;

import org.slf4j.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.example.aiPoc.exceptions.AIServiceException;
import com.google.genai.Client;
import com.google.genai.types.*;

/**
 * Service d’intégration du modèle <b>Gemini</b> via le SDK officiel Google Gen AI.
 * 
 * <p>Cette classe implémente l’interface {@link AIService} et fournit un pont entre
 * l’application et le modèle d’IA générative Gemini. Elle permet d’envoyer un prompt,
 * d’ajuster les paramètres de génération et de récupérer le texte produit par le modèle.</p>
 * 
 * @see AIService
 * @see AIServiceException
 * @see com.google.genai.Client
 */
@Service
public class GeminiAIService implements AIService {

    /** Logger SLF4J pour le suivi des opérations et des erreurs. */
    private static final Logger logger = LoggerFactory.getLogger(GeminiAIService.class);

    /** Client Google Gen AI utilisé pour l’appel au modèle Gemini. */
    private final Client client;

    /** Nom du modèle Gemini utilisé (ex. {@code gemini-2.0-flash-exp}). */
    private final String model;

    /**
     * Construit une instance du service Gemini en initialisant le client Google Gen AI.
     *
     * @param apiKey clé API du compte Google Gen AI
     * @param model nom du modèle Gemini à utiliser (ex. {@code gemini-2.0-flash-exp})
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

            // Configuration de génération
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
            throw new AIServiceException("Échec de génération Gemini : " + e.getMessage(), e);
        }
    }
}