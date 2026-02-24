package com.penpot.ai.application.advisor;

import com.penpot.ai.application.tools.PenpotInspectorTools;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.CallAdvisor;
import org.springframework.ai.chat.client.advisor.api.CallAdvisorChain;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * Advisor "Inspection First" : injecte automatiquement le contexte d'inspection Penpot
 * dans le prompt lorsque la catégorie INSPECTION est présente.
 *
 * <p>But : éviter les réponses du type "ID manquant" en fournissant au modèle
 * la liste des éléments existants (UUIDs + propriétés) avant l'appel LLM.</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class InspectionFirstAdvisor implements CallAdvisor {

    /** Clé contextuelle contenant les catégories détectées par le router. */
    public static final String CTX_TOOL_CATEGORIES = "toolCategories";

    /** Flag pour éviter de réinjecter plusieurs fois (retry, boucles). */
    private static final String CTX_ALREADY_INJECTED = "inspectionInjected";

    private final PenpotInspectorTools inspectorTools;

    @Override
    public ChatClientResponse adviseCall(ChatClientRequest request, CallAdvisorChain chain) {

        Map<String, Object> context = new HashMap<>(request.context());

        boolean alreadyInjected = Boolean.TRUE.equals(context.get(CTX_ALREADY_INJECTED));
        Set<String> categories = extractCategories(context.get(CTX_TOOL_CATEGORIES));
        boolean needsInspection =
            categories.contains("INSPECTION")
         || categories.contains("COLOR_AND_STYLE")
         || categories.contains("SHAPE_MODIFICATION")
         || categories.contains("DELETION");

        if (needsInspection && !alreadyInjected) {
            log.debug("[InspectionFirstAdvisor] INSPECTION detected → injecting page context");

            String inspectionJson = inspectorTools.describeElementsDetailed();

            String injection = """
                # CONTEXTE ACTUEL DE LA PAGE (INSPECTION AUTO)
                Voici les éléments présents sur la page Penpot avec leurs propriétés et UUIDs.
                Utilise ces UUIDs pour cibler précisément l'élément demandé.
                Si plusieurs candidats correspondent, pose UNE question courte.

                %s
                """.formatted(inspectionJson);

            Prompt augmented = request.prompt().augmentSystemMessage(injection);
            context.put(CTX_ALREADY_INJECTED, true);

            return chain.nextCall(new ChatClientRequest(augmented, context));
        }

        return chain.nextCall(request);
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE + 50;
    }

    @Override
    public String getName() {
        return "InspectionFirstAdvisor";
    }

    private Set<String> extractCategories(Object raw) {
        if (raw == null) return Collections.emptySet();
        if (raw instanceof Collection<?> col) {
            Set<String> out = new HashSet<>();
            for (Object o : col) out.add(String.valueOf(o));
            return out;
        }
        return Set.of(String.valueOf(raw));
    }
}