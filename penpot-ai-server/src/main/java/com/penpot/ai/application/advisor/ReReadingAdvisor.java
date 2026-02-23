package com.penpot.ai.application.advisor;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.*;
import org.springframework.ai.chat.client.advisor.api.*;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.core.Ordered;

import java.util.*;

/**
 * Advisor implémentant la technique <b>Re-Reading (Re2)</b>.
 *
 * <h2>Principe Re2</h2>
 * La technique consiste à répéter la question de l'utilisateur dans le prompt
 * pour forcer le modèle à la relire attentivement avant de répondre.
 * Elle améliore significativement la précision sur les requêtes ambiguës
 * (ex: "rends ça plus moderne", "aligne les éléments correctement").
 *
 * <h2>Transformation appliquée</h2>
 * Le message utilisateur original :
 * <pre>{@code "mets du rouge sur ce rectangle"}</pre>
 * devient :
 * <pre>{@code
 * Read the question again: mets du rouge sur ce rectangle
 *
 * mets du rouge sur ce rectangle
 * }</pre>
 *
 * @see BaseAdvisor Interface parente fournissant les implémentations call/stream par défaut
 */
@Slf4j
public class ReReadingAdvisor implements BaseAdvisor {

    /** Template appliqué au message original pour construire le prompt Re2. */
    private static final String RE2_TEMPLATE = "Read the question again: %s\n\n%s";

    /**
     * Ordre d'exécution de l'advisor dans la chaîne.
     *
     * <p>Valeur {@code 0} : exécuté avant la mémoire ({@code DEFAULT_CHAT_MEMORY_PRECEDENCE_ORDER}
     * est positif) mais après les advisors de sécurité ({@code HIGHEST_PRECEDENCE}).
     * Positionner le Re2 tôt dans la chaîne garantit que le message reformulé
     * est celui qui sera enregistré en mémoire et envoyé au RAG.</p>
     */
    private final int order;

    /** Constructeur par défaut — ordre {@code 0}. */
    public ReReadingAdvisor() {
        this(0);
    }

    /**
     * Constructeur avec ordre configurable.
     *
     * @param order position dans la chaîne d'advisors ({@link Ordered})
     */
    public ReReadingAdvisor(int order) {
        this.order = order;
    }

    /**
     * Avant l'appel au modèle : reformule le dernier message utilisateur
     * avec la technique Re2 (répétition de la question).
     *
     * <p>Si la requête ne contient aucun message utilisateur (cas rare),
     * elle est retournée sans modification pour ne pas bloquer le pipeline.</p>
     *
     * {@inheritDoc}
     */
    @Override
    public ChatClientRequest before(ChatClientRequest request, AdvisorChain advisorChain) {
        Prompt originalPrompt = request.prompt();

        // Trouver le dernier UserMessage
        String originalUserText = originalPrompt.getInstructions().stream()
            .filter(msg -> msg instanceof UserMessage)
            .map(msg -> msg.getText())
            .reduce((first, second) -> second) // dernier message utilisateur
            .orElse(null);

        if (originalUserText == null || originalUserText.isBlank()) {
            log.debug("[ReReadingAdvisor] No user message found, skipping Re2 transformation");
            return request;
        }

        // Construire le message Re2
        String reReadMessage = String.format(RE2_TEMPLATE, originalUserText, originalUserText);
        log.debug("[ReReadingAdvisor] Applying Re2 to message of {} chars", originalUserText.length());

        // Reconstruire les messages en remplaçant le dernier UserMessage
        List<org.springframework.ai.chat.messages.Message> messages = new ArrayList<>(
            originalPrompt.getInstructions()
        );

        for (int i = messages.size() - 1; i >= 0; i--) {
            if (messages.get(i) instanceof UserMessage) {
                messages.set(i, new UserMessage(reReadMessage));
                break;
            }
        }

        Prompt modifiedPrompt = new Prompt(messages, originalPrompt.getOptions());
        return request.mutate().prompt(modifiedPrompt).build();
    }

    /**
     * Après l'appel au modèle : aucune transformation — Re2 n'intervient
     * qu'en pré-traitement.
     *
     * {@inheritDoc}
     */
    @Override
    public ChatClientResponse after(ChatClientResponse response, AdvisorChain advisorChain) {
        return response;
    }

    @Override
    public int getOrder() {
        return order;
    }

    @Override
    public String getName() {
        return "ReReadingAdvisor";
    }
}