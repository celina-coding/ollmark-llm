package com.example.aiPoc.services.ai;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.stereotype.Service;

/**
 * <p>
 * Implémentation concrète de l’interface {@link AIService} basée sur l’API OpenAI,
 * compatible avec le fournisseur <b>Groq</b>.
 * </p>
 *
 * <p>
 * Bien que cette classe utilise le nom <i>OpenAIService</i>, elle peut fonctionner avec Groq
 * car ce dernier expose une API entièrement compatible avec les spécifications OpenAI.
 * Elle permet ainsi d’envoyer des prompts textuels à un modèle d’intelligence artificielle
 * et de récupérer la réponse générée sous forme de texte.
 * </p>
 *
 * <p>
 * Cette classe s’appuie sur la bibliothèque <b>Spring AI</b> et sur le client
 * {@link ChatClient} pour gérer la communication avec le modèle distant.
 * </p>
 *
 * <p><b>Responsabilités principales :</b></p>
 * <ul>
 *   <li>Initialiser le client de communication avec le modèle IA.</li>
 *   <li>Transmettre les prompts et collecter les réponses textuelles.</li>
 *   <li>Gérer les erreurs de communication et enregistrer les événements via SLF4J.</li>
 * </ul>
 *
 * @see AIService
 * @see ChatClient
 * @see OpenAiChatModel
 */
@Service
public class OpenAIService implements AIService {

    /** Logger SLF4J pour le suivi des opérations et des erreurs. */
    private static final Logger logger = LoggerFactory.getLogger(OpenAIService.class);

    /** Client de communication avec le modèle IA, fourni par Spring AI. */
    private final ChatClient chatClient;

    /**
     * <p>
     * Construit un service OpenAI/Groq prêt à être utilisé.
     * </p>
     *
     * <p>
     * Le constructeur initialise le client {@link ChatClient} à partir du modèle
     * {@link OpenAiChatModel} injecté par Spring, garantissant une compatibilité
     * native avec l’API OpenAI.
     * </p>
     *
     * @param chatModel le modèle de chat OpenAI ou compatible Groq utilisé pour
     *                  générer les réponses IA
     */
    public OpenAIService(OpenAiChatModel chatModel) {
        this.chatClient = ChatClient.builder(chatModel).build();
        logger.info("OpenAIService initialisé avec succès");
    }

    @Override
    public String chat(String prompt) {
        logger.debug("Envoi du prompt à l'IA: {} caractères", prompt.length());

        try {
            long startTime = System.currentTimeMillis();

            String response = chatClient
                .prompt(prompt)
                .call()
                .content();

            long duration = System.currentTimeMillis() - startTime;
            logger.debug("Réponse IA reçue en {}ms: {} caractères", duration, response.length());

            return response;
        } catch (Exception e) {
            logger.error("Erreur lors de l'appel à l'IA", e);
            throw new RuntimeException("Échec de communication avec l'IA: " + e.getMessage(), e);
        }
    }

    @Override
    public String chat(String prompt, int maxTokens, double temperature) {
        logger.debug("Envoi du prompt avec paramètres: maxTokens={}, temperature={}", 
                    maxTokens, temperature);

        // TODO: Implémenter avec paramètres personnalisés si nécessaire
        // Pour l'instant, utilise l'implémentation par défaut
        return chat(prompt);
    }
}