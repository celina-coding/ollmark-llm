package com.penpot.mcp.adapters.out.documentation;

import com.penpot.mcp.core.ports.out.ApiDocumentationPort;
import com.penpot.mcp.application.service.ApiDocsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Adapter pour accéder à la documentation API via ApiDocsService.
 * Implémente le port de sortie ApiDocumentationPort (Hexagonal Architecture).
 * Transforme l'interface du service existant en interface du domaine.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ApiDocumentationAdapter implements ApiDocumentationPort {

    private final ApiDocsService apiDocsService;

    @Override
    public Optional<String> getTypeInfo(String typeName, String memberName) {
        log.debug("Getting type info for: {} (member: {})", typeName, memberName);

        ApiDocsService.ApiType apiType = apiDocsService.getType(typeName);
        if (apiType == null) {
            log.debug("Type not found: {}", typeName);
            return Optional.empty();
        }

        if (memberName != null && !memberName.isBlank()) {
            String memberDoc = apiType.getMember(memberName);
            if (memberDoc != null) return Optional.of(memberDoc);
            log.debug("Member not found: {}.{}", typeName, memberName);
            return Optional.empty();
        }

        String fullDoc = apiType.getFullText();
        return Optional.of(fullDoc);
    }

    @Override
    public String getOverview() {
        log.debug("Getting API overview");

        StringBuilder overview = new StringBuilder();
        overview.append("Penpot Plugin API Overview\n\n");
        overview.append("Available API Types:\n");

        List<String> types = getAllTypeNames();
        types.forEach(type -> {
            ApiDocsService.ApiType apiType = apiDocsService.getType(type);
            if (apiType != null) {
                String typeOverview = apiType.getOverviewText();
                if (typeOverview.length() > 200) {
                    typeOverview = typeOverview.substring(0, 200) + "...";
                }
                overview.append("- ").append(type).append(": ")
                    .append(typeOverview).append("\n\n");
            }
        });

        return overview.toString();
    }

    @Override
    public List<String> getAllTypeNames() {
        log.debug("Getting all type names");
        return apiDocsService.getTypeNames();
    }

    @Override
    public List<String> searchTypes(String keyword) {
        log.debug("Searching types with keyword: {}", keyword);

        if (keyword == null || keyword.isBlank()) return List.of();
        String keywordLower = keyword.toLowerCase();

        return apiDocsService.getTypeNames().stream()
            .filter(typeName -> {
                if (typeName.toLowerCase().contains(keywordLower)) {
                    return true;
                }

                ApiDocsService.ApiType apiType = apiDocsService.getType(typeName);
                if (apiType != null) {
                    String overview = apiType.getOverviewText().toLowerCase();
                    return overview.contains(keywordLower);
                }

                return false;
            })
            .collect(Collectors.toList());
    }

    @Override
    public boolean typeExists(String typeName) {
        if (typeName == null || typeName.isBlank()) return false;
        return apiDocsService.getType(typeName) != null;
    }
}