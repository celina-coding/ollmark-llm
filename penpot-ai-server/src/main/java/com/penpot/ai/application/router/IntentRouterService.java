package com.penpot.ai.application.router;

import com.penpot.ai.core.domain.ToolCategory;
import com.penpot.ai.core.ports.out.ToolRouterPort;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Service de routing d'intention basé sur le modèle phi3:mini.
 *
 * <h2>Responsabilité unique (SRP)</h2>
 * Ce service n'a qu'une seule responsabilité : analyser le message utilisateur
 * et retourner les catégories de tools pertinentes. Il ne connaît pas les tools
 * eux-mêmes, ni la logique d'exécution.
 *
 * <h2>Stratégie de parsing en deux niveaux</h2>
 * llama3.1 est un petit modèle (3.8B) qui peut produire une réponse JSON
 * malformée. La stratégie de parsing est donc défensive :
 * <ol>
 *   <li><b>Niveau 1 — entity()</b> : Spring AI tente la conversion JSON structurée.</li>
 *   <li><b>Niveau 2 — keyword scan</b> : si le niveau 1 échoue, on recherche
 *       les noms d'enum dans le texte brut de la réponse.</li>
 *   <li><b>Fallback final</b> : {@code INSPECTION} seul, pour que l'exécuteur
 *       puisse au moins lire la page avant de répondre à l'utilisateur.</li>
 * </ol>
 *
 * <h2>Configuration</h2>
 * Injecte le {@code ChatClient} qualifié {@code "routerChatClient"} défini
 * dans {@link com.penpot.ai.infrastructure.config.RouterConfig}.
 *
 * @see ToolRouterPort Port implémenté
 * @see com.penpot.ai.infrastructure.config.RouterConfig Configuration du bean router
 */
@Slf4j
@Service
public class IntentRouterService implements ToolRouterPort {

    /**
     * Prompt système optimisé pour phi3:mini.
     *
     * <p>Règles de rédaction appliquées pour les petits modèles :
     * <ul>
     *   <li>Instructions courtes et impératives</li>
     *   <li>Exemples concrets JSON dans le prompt</li>
     *   <li>Répétition de la contrainte "JSON uniquement"</li>
     *   <li>Chaque catégorie documentée avec des mots-clés trigger</li>
     * </ul>
     */
    private static final String ROUTER_SYSTEM_PROMPT = """
        You are a tool classifier for Penpot, a graphic design application.
        Your ONLY job is to return a JSON object with the relevant tool categories.

        AVAILABLE CATEGORIES (choose only from these exact values):
        - SHAPE_CREATION    : create rectangle, circle, ellipse, board, frame
        - SHAPE_MODIFICATION: move, resize, rotate, scale, duplicate, clone existing shape
        - COLOR_AND_STYLE   : fill color, stroke, gradient, shadow, opacity, blur
        - LAYOUT_AND_ALIGNMENT: align, distribute, group, ungroup, arrange, order
        - CONTENT_AND_TEXT  : text, title, paragraph, subtitle, image, media
        - ASSET_MANAGEMENT  : component, shared style, font, asset library
        - INSPECTION        : list elements, find shape, get properties, what is on the page
        - DELETION          : delete, remove, clear element
        - TEMPLATE_SEARCH   : template, marketing design, poster, social media post, flyer, email

        RULES:
        - Return ONLY valid JSON, nothing else.
        - Include INSPECTION when the request is ambiguous or requires reading page state.
        - Be minimal: only include categories truly needed.

        EXAMPLES:
        User: "put a red fill on the rectangle"
        Response: {"categories": ["COLOR_AND_STYLE", "INSPECTION"]}

        User: "create a 400x200 blue rectangle"
        Response: {"categories": ["SHAPE_CREATION"]}

        User: "align all elements to the left then group them"
        Response: {"categories": ["LAYOUT_AND_ALIGNMENT", "INSPECTION"]}

        User: "find me a social media template"
        Response: {"categories": ["TEMPLATE_SEARCH"]}

        User: "delete the header"
        Response: {"categories": ["DELETION", "INSPECTION"]}

        Return ONLY JSON. No explanation. No markdown.
        """;

    /**
     * DTO de sortie structurée utilisé par Spring AI pour parser la réponse JSON.
     * Record Java — immutable par définition.
     */
    private record RouterResult(List<String> categories) {
        RouterResult {
            // Défense : garantir une liste non-null même si le modèle renvoie null
            categories = categories != null ? categories : List.of();
        }
    }

    private final ChatClient routerChatClient;

    public IntentRouterService(@Qualifier("routerChatClient") ChatClient routerChatClient) {
        this.routerChatClient = routerChatClient;
    }

    /**
     * {@inheritDoc}
     *
     * <p>Appelle phi3:mini pour classifier l'intention, puis parse la réponse
     * avec une stratégie défensive en deux niveaux.
     * Ne lève jamais d'exception (contrat du port).</p>
     */
    @Override
    public Set<ToolCategory> route(String userMessage) {
        log.debug("[Router] Classifying intent for message: '{}'", truncate(userMessage, 80));
        long start = System.currentTimeMillis();

        try {
            Set<ToolCategory> categories = classifyWithStructuredOutput(userMessage);
            log.info("[Router] Classified in {}ms → categories: {}",
                System.currentTimeMillis() - start, categories);
            return categories;

        } catch (Exception e) {
            log.warn("[Router] Structured output failed, falling back to keyword scan. Cause: {}", e.getMessage());

            try {
                Set<ToolCategory> categories = classifyWithKeywordScan(userMessage);
                log.info("[Router] Keyword scan result in {}ms → categories: {}",
                    System.currentTimeMillis() - start, categories);
                return categories;

            } catch (Exception ex) {
                log.error("[Router] All classification strategies failed. Using INSPECTION fallback.", ex);
                return EnumSet.of(ToolCategory.INSPECTION);
            }
        }
    }

    /**
     * Tente la classification via {@code entity()} de Spring AI.
     * Envoie le schema JSON au modèle pour guider sa réponse.
     */
    private Set<ToolCategory> classifyWithStructuredOutput(String userMessage) {
        RouterResult result = routerChatClient.prompt()
            .system(ROUTER_SYSTEM_PROMPT)
            .user(userMessage)
            .call()
            .entity(RouterResult.class);

        return parseCategories(result.categories());
    }

    /**
     * Fallback : appel texte brut + scan des noms d'enum dans la réponse.
     * Fonctionne même si phi3:mini produit du texte autour du JSON.
     */
    private Set<ToolCategory> classifyWithKeywordScan(String userMessage) {
        String rawResponse = routerChatClient.prompt()
            .system(ROUTER_SYSTEM_PROMPT)
            .user(userMessage)
            .call()
            .content();

        log.debug("[Router] Raw text response for keyword scan: {}", rawResponse);

        Set<ToolCategory> found = Arrays.stream(ToolCategory.values())
            .filter(category -> rawResponse != null
                && rawResponse.toUpperCase().contains(category.name()))
            .collect(Collectors.toCollection(() -> EnumSet.noneOf(ToolCategory.class)));

        return found.isEmpty() ? EnumSet.of(ToolCategory.INSPECTION) : found;
    }

    /**
     * Convertit une liste de strings (noms d'enum) en {@code Set<ToolCategory>}.
     *
     * <p>Les valeurs inconnues ou malformées sont ignorées avec un warning,
     * sans faire échouer l'ensemble du parsing.</p>
     */
    private Set<ToolCategory> parseCategories(List<String> rawCategories) {
        if (rawCategories.isEmpty()) {
            log.warn("[Router] Model returned empty categories list, using INSPECTION fallback");
            return EnumSet.of(ToolCategory.INSPECTION);
        }

        Set<ToolCategory> result = rawCategories.stream()
            .map(String::trim)
            .map(String::toUpperCase)
            .flatMap(name -> {
                try {
                    return java.util.stream.Stream.of(ToolCategory.valueOf(name));
                } catch (IllegalArgumentException e) {
                    log.warn("[Router] Unknown category name ignored: '{}'. "
                        + "Valid values: {}", name, Arrays.toString(ToolCategory.values()));
                    return java.util.stream.Stream.empty();
                }
            })
            .collect(Collectors.toCollection(() -> EnumSet.noneOf(ToolCategory.class)));

        return result.isEmpty() ? EnumSet.of(ToolCategory.INSPECTION) : result;
    }

    private String truncate(String str, int max) {
        if (str == null) return "null";
        return str.length() <= max ? str : str.substring(0, max) + "...";
    }
}