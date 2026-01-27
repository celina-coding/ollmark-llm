package com.penpot.mcp.infrastructure.chain;

import com.penpot.mcp.core.domain.AiContext;
import com.penpot.mcp.core.ports.out.ApiDocumentationPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Enrichisseur qui ajoute la documentation API pertinente au contexte.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ApiDocumentationEnricher extends ContextEnricher {

    private final ApiDocumentationPort apiDocs;

    private static final List<String> COMMON_TYPES = List.of(
        "Penpot", "Shape", "Rectangle", "Board", "Text", "Ellipse"
    );

    private static final int MAX_DOC_LENGTH = 500;

    @Override
    protected AiContext doEnrich(AiContext context) {
        log.debug("Enriching context with API documentation");

        Set<String> relevantTypes = extractRelevantTypes(context);
        Map<String, String> documentation = new HashMap<>();

        for (String type : relevantTypes) {
            apiDocs.getTypeInfo(type, null)
                .ifPresent(doc -> {
                    String truncated = truncateDoc(doc);
                    documentation.put(type, truncated);
                    log.debug("Added documentation for type: {}", type);
                });
        }

        return context.withApiDocumentation(documentation);
    }

    @Override
    protected boolean shouldEnrich(AiContext context) {
        return context.isCodeGeneration();
    }

    /**
     * Extrait les types API mentionnés dans le contexte.
     */
    private Set<String> extractRelevantTypes(AiContext context) {
        Set<String> types = new HashSet<>();
        String text = context.getTask() + " " + context.getUserContext();
        types.addAll(COMMON_TYPES);

        for (String type : apiDocs.getAllTypeNames()) {
            if (Pattern.compile("\\b" + type + "\\b", Pattern.CASE_INSENSITIVE)
                    .matcher(text)
                    .find()) {
                types.add(type);
            }
        }

        return types;
    }

    /**
     * Tronque la documentation si trop longue.
     */
    private String truncateDoc(String doc) {
        if (doc.length() <= MAX_DOC_LENGTH) return doc;
        return doc.substring(0, MAX_DOC_LENGTH) + "...";
    }
}