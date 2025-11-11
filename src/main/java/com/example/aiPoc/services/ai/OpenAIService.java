/*package com.example.aiPoc.services.ai;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.stereotype.Service;
*/
/**
 * Implémentation concrète de l’interface {@link AIService} basée sur l’API OpenAI,
 * compatible avec le fournisseur <b>Groq</b>.
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
/*@Service
public class OpenAIService implements AIService {

    /** Logger SLF4J pour le suivi des opérations et des erreurs. */
  //  private static final Logger logger = LoggerFactory.getLogger(OpenAIService.class);

    /** Client de communication avec le modèle IA, fourni par Spring AI. */
    //private final ChatClient chatClient;

    /**
     * Construit un service OpenAI/Groq prêt à être utilisé.
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
/*    public OpenAIService(OpenAiChatModel chatModel) {
        this.chatClient = ChatClient.builder(chatModel).build();
        logger.info("OpenAIService initialisé avec succès");
    }

    /**
     * {@inheritDoc}
     * 
     * <p>
     * <b>Note :</b> Cette méthode utilise des paramètres par défaut optimisés.
     * Pour un contrôle plus fin, utilisez {@link #chat(String, int, double)}.
     * </p>
     */
  /*  @Override
    public String chat(String prompt) {
        return chat(prompt, 4096, 0.5);
    }

    /**
     * {@inheritDoc}
     * 
     * <p>
     * <b>Optimisations appliquées :</b>
     * </p>
     * <ul>
     *   <li>Configuration explicite de maxTokens pour limiter la réponse</li>
     *   <li>Ajustement de la température pour un équilibre créativité/vitesse</li>
     *   <li>Mesure précise du temps de réponse</li>
     * </ul>
     */
   /* @Override
    public String chat(String prompt, int maxTokens, double temperature) {
        logger.debug("Envoi du prompt à l'IA: {} caractères (maxTokens={}, temperature={})", 
                    prompt.length(), maxTokens, temperature);

        try {
            long startTime = System.currentTimeMillis();

            OpenAiChatOptions options = OpenAiChatOptions.builder()
                .maxTokens(maxTokens)
                .temperature(temperature)
                .build();

            String response = chatClient
                .prompt(prompt)
                .options(options)
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
}*/