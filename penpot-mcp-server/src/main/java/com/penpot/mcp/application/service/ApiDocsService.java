package com.penpot.mcp.application.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.yaml.snakeyaml.Yaml;
import jakarta.annotation.PostConstruct;
import java.io.*;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service de documentation API pour Penpot.
 * Ce service charge et gère la documentation de l'API Penpot depuis des fichiers
 * YAML, fournissant un accès insensible à la casse aux types, interfaces et
 * membres de l'API.
 */
@Slf4j
@Service
public class ApiDocsService {

    /** Cache des types API chargés, indexés par nom en minuscules */
    private final Map<String, ApiType> apiTypes = new HashMap<>();

    /**
     * Initialise le service en chargeant la documentation API.
     * Cette méthode est appelée automatiquement après la construction du bean.
     */
    @PostConstruct
    public void init() {
        loadApiTypes();
        log.info("Loaded {} API types from api_types.yml", apiTypes.size());
    }

    /**
     * Charge les types API depuis le fichier YAML de configuration.
     */
    private void loadApiTypes() {
        try {
            ClassPathResource resource = new ClassPathResource("data/api_types.yml");
            if (!resource.exists()) {
                log.warn("api_types.yml not found in classpath:data/");
                return;
            }

            Yaml yaml = new Yaml();
            try (InputStream inputStream = resource.getInputStream()) {
                @SuppressWarnings("unchecked")
                Map<String, Map<String, Object>> data = yaml.load(inputStream);

                if (data == null) {
                    log.warn("api_types.yml is empty or invalid");
                    return;
                }

                for (Map.Entry<String, Map<String, Object>> entry : data.entrySet()) {
                    String typeName = entry.getKey();
                    Map<String, Object> typeData = entry.getValue();
                    String overview = convertToString(typeData.get("overview"));

                    Map<String, Map<String, String>> members = new HashMap<>();
                    Object membersObj = typeData.get("members");

                    if (membersObj instanceof Map) {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> membersMap = (Map<String, Object>) membersObj;

                        for (Map.Entry<String, Object> memberTypeEntry : membersMap.entrySet()) {
                            String memberType = memberTypeEntry.getKey();

                            if (memberTypeEntry.getValue() instanceof Map) {
                                @SuppressWarnings("unchecked")
                                Map<String, Object> memberEntries = (Map<String, Object>) memberTypeEntry.getValue();
                                Map<String, String> convertedEntries = new HashMap<>();

                                for (Map.Entry<String, Object> memberEntry : memberEntries.entrySet()) {
                                    String key = convertToString(memberEntry.getKey());
                                    String value = convertToString(memberEntry.getValue());
                                    convertedEntries.put(key, value);
                                }

                                members.put(memberType, convertedEntries);
                            }
                        }
                    }

                    ApiType apiType = new ApiType(typeName, overview, members);
                    apiTypes.put(typeName.toLowerCase(), apiType);
                    log.debug("Loaded API type: {} with {} member categories", 
                        typeName, members.size());
                }
            }
        } catch (IOException e) {
            log.error("Failed to load api_types.yml", e);
        }
    }

    /**
     * Convertit un objet en String de manière sûre.
     */
    private String convertToString(Object value) {
        if (value == null) return "";

        if (value instanceof String) {
            return (String) value;
        } else if (value instanceof Boolean) {
            return value.toString();
        } else if (value instanceof Number) {
            return value.toString();
        } else if (value instanceof List) {
            @SuppressWarnings("unchecked")
            List<Object> list = (List<Object>) value;
            return list.stream()
                .map(this::convertToString)
                .collect(Collectors.joining(", "));
        } else if (value instanceof Map) {
            return value.toString();
        } else {
            return String.valueOf(value);
        }
    }

    /**
     * Récupère un type API par nom (recherche insensible à la casse).
     */
    public ApiType getType(String typeName) {
        return apiTypes.get(typeName.toLowerCase());
    }

    /**
     * Retourne tous les noms de types API disponibles, triés alphabétiquement.
     */
    public List<String> getTypeNames() {
        return apiTypes.values().stream()
            .map(ApiType::getName)
            .sorted()
            .collect(Collectors.toList());
    }

    /**
     * Retourne le nombre de types API chargés.
     */
    public int getTypeCount() {
        return apiTypes.size();
    }

    /**
     * Représente un type ou interface de l'API Penpot.
     */
    public static class ApiType {
        private final String name;
        private final String overview;
        private final Map<String, Map<String, String>> members;
        private String cachedFullText = null;

        public ApiType(String name, String overview, Map<String, Map<String, String>> members) {
            this.name = name;
            this.overview = overview;
            this.members = members;
        }

        public String getName() {
            return name;
        }

        public String getOverviewText() {
            return overview;
        }

        public String getFullText() {
            if (cachedFullText == null) {
                StringBuilder text = new StringBuilder(overview);

                for (Map.Entry<String, Map<String, String>> memberTypeEntry : members.entrySet()) {
                    String memberType = memberTypeEntry.getKey();
                    text.append("\n\n## ").append(memberType).append("\n");

                    for (Map.Entry<String, String> memberEntry : memberTypeEntry.getValue().entrySet()) {
                        String memberName = memberEntry.getKey();
                        String memberDescription = memberEntry.getValue();

                        text.append("\n### ").append(memberName).append("\n\n")
                            .append(memberDescription != null ? memberDescription : "");
                    }
                }

                cachedFullText = text.toString();
            }
            return cachedFullText;
        }

        public String getMember(String memberName) {
            for (Map<String, String> memberEntries : members.values()) {
                if (memberEntries.containsKey(memberName)) {
                    return memberEntries.get(memberName);
                }
            }
            return null;
        }
    }
}