package com.penpot.mcp.service;

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
 * <p>
 * Ce service charge et gère la documentation de l'API Penpot depuis des fichiers
 * YAML, fournissant un accès insensible à la casse aux types, interfaces et
 * membres de l'API.
 * </p>
 * <p>
 * La documentation est chargée au démarrage depuis {@code data/api_types.yml}.
 * 
 * @see ApiType
 */
@Slf4j
@Service
public class ApiDocsService {

    /** Cache des types API chargés, indexés par nom en minuscules */
    private final Map<String, ApiType> apiTypes = new HashMap<>();

    /**
     * Initialise le service en chargeant la documentation API.
     * <p>
     * Cette méthode est appelée automatiquement après la construction du bean.
     */
    @PostConstruct
    public void init() {
        loadApiTypes();
        log.info("Loaded {} API types from api_types.yml", apiTypes.size());
    }

    /**
     * Charge les types API depuis le fichier YAML de configuration.
     * <p>
     * Le fichier attendu est {@code classpath:data/api_types.yml} avec la structure:
     * </p>
     * <pre>
     * TypeName:
     *   overview: "Description du type..."
     *   members:
     *     Properties:
     *       propertyName: "Description..."
     *     Methods:
     *       methodName: "Description..."
     * </pre>
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
     * <p>
     * Gère les types primitifs, Boolean, Number, List et Map en les
     * convertissant en représentation String appropriée.
     *
     * @param value l'objet à convertir
     * @return la représentation String ou chaîne vide si null
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
     *
     * @param typeName le nom du type à rechercher
     * @return le type API ou null si non trouvé
     */
    public ApiType getType(String typeName) {
        return apiTypes.get(typeName.toLowerCase());
    }

    /**
     * Retourne tous les noms de types API disponibles, triés alphabétiquement.
     *
     * @return la liste des noms de types
     */
    public List<String> getTypeNames() {
        return apiTypes.values().stream()
                .map(ApiType::getName)
                .sorted()
                .collect(Collectors.toList());
    }

    /**
     * Retourne le nombre de types API chargés.
     *
     * @return le nombre de types
     */
    public int getTypeCount() {
        return apiTypes.size();
    }

    /**
     * Représente un type ou interface de l'API Penpot.
     * <p>
     * Un type API contient:
     * </p>
     * <ul>
     *   <li>Un nom identifiant le type</li>
     *   <li>Une vue d'ensemble (description générale)</li>
     *   <li>Des membres organisés par catégories (Properties, Methods, etc.)</li>
     * </ul>
     * <p>
     * Le texte complet de documentation est construit et mis en cache
     * lors du premier accès pour optimiser les performances.
     */
    public static class ApiType {
        /** Nom du type API */
        private final String name;

        /** Description générale du type */
        private final String overview;

        /** Membres du type organisés par catégorie (Properties, Methods, etc.) */
        private final Map<String, Map<String, String>> members;

        /** Cache du texte complet de documentation */
        private String cachedFullText = null;

        /**
         * Construit un nouveau type API.
         *
         * @param name le nom du type
         * @param overview la description générale
         * @param members les membres organisés par catégorie
         */
        public ApiType(String name, String overview, Map<String, Map<String, String>> members) {
            this.name = name;
            this.overview = overview;
            this.members = members;
        }

        /**
         * Retourne le nom du type.
         *
         * @return le nom du type
         */
        public String getName() {
            return name;
        }

        /**
         * Retourne la vue d'ensemble du type.
         *
         * @return la description générale
         */
        public String getOverviewText() {
            return overview;
        }

        /**
         * Génère le texte complet de documentation au format Markdown.
         * <p>
         * Le texte est mis en cache après la première génération.
         * La structure du document est:
         * </p>
         * <pre>
         * [Overview]
         * 
         * ## [Category1]
         * ### [Member1]
         * [Description]
         * 
         * ### [Member2]
         * [Description]
         * ...
         * </pre>
         *
         * @return le texte complet de documentation
         */
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

        /**
         * Recherche et retourne la documentation d'un membre spécifique.
         * <p>
         * La recherche est effectuée dans toutes les catégories de membres,
         * car les noms de membres sont uniques au sein d'un type API.
         *
         * @param memberName le nom du membre à rechercher
         * @return la description du membre ou null si non trouvé
         */
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