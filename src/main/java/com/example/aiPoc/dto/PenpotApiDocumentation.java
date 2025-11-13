package com.example.aiPoc.dto;

import com.fasterxml.jackson.annotation.*;
import java.util.*;

/**
 * Représente la structure complète du fichier JSON <b>penpot-api-summary.json</b>.
 * <p>
 * Ce DTO centralise les métadonnées, les méthodes, les propriétés communes et les utilitaires du SDK Penpot.
 * Il permet le chargement dynamique et typé de la documentation du SDK à partir du JSON source,
 * afin d'être exploité dans les services d'analyse et de génération de code.
 * </p>
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PenpotApiDocumentation {

    /** Version actuelle de la documentation Penpot. */
    @JsonProperty("version")
    private String version;

    /** Date de dernière mise à jour du fichier source. */
    @JsonProperty("lastUpdate")
    private String lastUpdate;

    /** Chemin ou URL source d'origine de la documentation. */
    @JsonProperty("source")
    private String source;

    /** Description générale de la documentation. */
    @JsonProperty("description")
    private String description;

    /** Catégories fonctionnelles des méthodes. */
    @JsonProperty("categories")
    private Map<String, Category> categories = new HashMap<>();

    /** Liste complète des méthodes documentées dans le SDK Penpot. */
    @JsonProperty("methods")
    private List<ApiMethod> methods = new ArrayList<>();

    /** Ensemble des propriétés communes aux différentes classes ou composants du SDK. */
    @JsonProperty("commonProperties")
    private Map<String, CommonProperty> commonProperties = new HashMap<>();

    /** Méthodes utilitaires ou raccourcis globaux proposés par le SDK. */
    @JsonProperty("utilities")
    private Map<String, Utility> utilities = new HashMap<>();

    /** Patterns de code courants. */
    @JsonProperty("commonPatterns")
    private Map<String, CommonPattern> commonPatterns = new HashMap<>();

    /** Conseils et astuces d'utilisation. */
    @JsonProperty("tips")
    private List<String> tips = new ArrayList<>();

    /**
     * Représente une catégorie regroupant un ensemble de méthodes Penpot.
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class Category {

        /** Description de la catégorie. */
        @JsonProperty("description")
        private String description;

        /** Mots-clés associés à la catégorie. */
        @JsonProperty("keywords")
        private List<String> keywords = new ArrayList<>();

        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        public List<String> getKeywords() { return keywords; }
        public void setKeywords(List<String> keywords) { this.keywords = keywords; }
    }

    /**
     * Représente une méthode documentée du SDK Penpot.
     * <p>
     * Contient les métadonnées nécessaires pour la génération, l’affichage et la validation :
     * nom, description, paramètres, type de retour, exemples et méthodes chainables.
     * </p>
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class ApiMethod {

        /** Nom de la méthode exposée dans le SDK. */
        @JsonProperty("name")
        private String name;

        /** Description fonctionnelle de la méthode. */
        @JsonProperty("description")
        private String description;

        /** Type de valeur renvoyée par la méthode. */
        @JsonProperty("returnType")
        private String returnType;

        /** Catégories auxquelles appartient la méthode. */
        @JsonProperty("categories")
        private List<String> categories = new ArrayList<>();

        /** Liste des paramètres nécessaires à l'appel de la méthode. */
        @JsonProperty("parameters")
        private List<Parameter> parameters = new ArrayList<>();

        /** Méthodes chainables disponibles sur l'objet retourné. */
        @JsonProperty("chainableMethods")
        private Map<String, ChainableMethod> chainableMethods = new HashMap<>();

        /** Indique si cette méthode est une méthode d'instance (plutôt que de penpot global). */
        @JsonProperty("isMethodOf")
        private String isMethodOf;

        /** Propriétés spécifiques (pour les méthodes comme createText). */
        @JsonProperty("textProperties")
        private Map<String, String> textProperties = new HashMap<>();

        /** Exemples d'utilisation courante. */
        @JsonProperty("commonUsage")
        private List<String> commonUsage = new ArrayList<>();

        /** Méthodes liées. */
        @JsonProperty("relatedMethods")
        private List<String> relatedMethods = new ArrayList<>();

        /** Notes complémentaires ou remarques sur l'utilisation de la méthode. */
        @JsonProperty("notes")
        private String notes;

        /**
         * Génère une signature formatée de la méthode en incluant ses paramètres.
         *
         * @return signature de la méthode sous la forme <code>penpot.nom(param: type, ...)</code>
         */
        public String formatSignature() {
            String params = parameters == null || parameters.isEmpty() ? "" :
                    parameters.stream()
                            .map(p -> {
                                String paramStr = p.getName() + ": " + p.getType();
                                if (!p.isRequired()) paramStr += "?";
                                return paramStr;
                            })
                            .reduce((a, b) -> a + ", " + b)
                            .orElse("");

            String prefix = isMethodOf != null ? isMethodOf.toLowerCase() + "." : "penpot.";
            return prefix + name + "(" + params + ")";
        }

        /**
         * Retourne le premier exemple d'utilisation s'il existe.
         * 
         * @return le premier exemple ou null
         */
        public String getExample() {
            return commonUsage != null && !commonUsage.isEmpty() ? commonUsage.get(0) : null;
        }

        /**
         * Retourne la première catégorie (pour compatibilité avec l'ancien code).
         * 
         * @return la première catégorie ou null
         */
        public String getCategory() {
            return categories != null && !categories.isEmpty() ? categories.get(0) : null;
        }

        // Getters et setters
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        public String getReturnType() { return returnType; }
        public void setReturnType(String returnType) { this.returnType = returnType; }
        public List<String> getCategories() { return categories; }
        public void setCategories(List<String> categories) { this.categories = categories; }
        public List<Parameter> getParameters() { return parameters; }
        public void setParameters(List<Parameter> parameters) { this.parameters = parameters; }
        public Map<String, ChainableMethod> getChainableMethods() { return chainableMethods; }
        public void setChainableMethods(Map<String, ChainableMethod> chainableMethods) { 
            this.chainableMethods = chainableMethods; 
        }
        public String getIsMethodOf() { return isMethodOf; }
        public void setIsMethodOf(String isMethodOf) { this.isMethodOf = isMethodOf; }
        public Map<String, String> getTextProperties() { return textProperties; }
        public void setTextProperties(Map<String, String> textProperties) { 
            this.textProperties = textProperties; 
        }
        public List<String> getCommonUsage() { return commonUsage; }
        public void setCommonUsage(List<String> commonUsage) { this.commonUsage = commonUsage; }
        public List<String> getRelatedMethods() { return relatedMethods; }
        public void setRelatedMethods(List<String> relatedMethods) { 
            this.relatedMethods = relatedMethods; 
        }
        public String getNotes() { return notes; }
        public void setNotes(String notes) { this.notes = notes; }

        /**
         * Retourne un map vide pour compatibilité avec l'ancien code qui utilisait "methods".
         * @deprecated Utiliser chainableMethods à la place
         */
        @Deprecated
        public Map<String, String> getMethods() {
            Map<String, String> result = new HashMap<>();
            if (chainableMethods != null) {
                chainableMethods.forEach((key, value) -> 
                    result.put(key, value.getDescription())
                );
            }
            return result;
        }

        /**
         * Retourne un map vide pour compatibilité avec l'ancien code qui utilisait "properties".
         * @deprecated Utiliser textProperties à la place
         */
        @Deprecated
        public Map<String, String> getProperties() {
            return textProperties != null ? textProperties : new HashMap<>();
        }
    }

    /**
     * Représente une méthode pouvant être appelée en chaîne après une autre méthode.
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class ChainableMethod {
        /** Signature de la méthode chainable. */
        @JsonProperty("signature")
        private String signature;

        /** Description de la méthode chainable. */
        @JsonProperty("description")
        private String description;

        public String getSignature() { return signature; }
        public void setSignature(String signature) { this.signature = signature; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
    }

    /**
     * Représente un paramètre d'entrée d'une méthode du SDK Penpot.
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class Parameter {

        /** Nom du paramètre. */
        @JsonProperty("name")
        private String name;

        /** Type de donnée attendu pour le paramètre. */
        @JsonProperty("type")
        private String type;

        /** Indique si le paramètre est obligatoire. */
        @JsonProperty("required")
        private boolean required;

        /** Description du rôle du paramètre. */
        @JsonProperty("description")
        private String description;

        /** Valeur par défaut utilisée si le paramètre n'est pas fourni. */
        @JsonProperty("defaultValue")
        private String defaultValue;

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getType() { return type; }
        public void setType(String type) { this.type = type; }
        public boolean isRequired() { return required; }
        public void setRequired(boolean required) { this.required = required; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        public String getDefaultValue() { return defaultValue; }
        public void setDefaultValue(String defaultValue) { this.defaultValue = defaultValue; }
    }

    /**
     * Représente une propriété commune.
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class CommonProperty {
        /** Description de la propriété. */
        @JsonProperty("description")
        private String description;

        /** Propriétés individuelles. */
        @JsonProperty("properties")
        private Map<String, PropertyDetail> properties = new HashMap<>();

        /** Exemple d'utilisation. */
        @JsonProperty("usage")
        private String usage;

        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        public Map<String, PropertyDetail> getProperties() { return properties; }
        public void setProperties(Map<String, PropertyDetail> properties) { 
            this.properties = properties; 
        }
        public String getUsage() { return usage; }
        public void setUsage(String usage) { this.usage = usage; }
    }

    /**
     * Détail d'une propriété individuelle.
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class PropertyDetail {
        /** Type de la propriété. */
        @JsonProperty("type")
        private String type;

        /** Description de la propriété. */
        @JsonProperty("description")
        private String description;

        /** Indique si la propriété est en lecture seule. */
        @JsonProperty("readOnly")
        private Boolean readOnly;

        /** Valeur par défaut. */
        @JsonProperty("defaultValue")
        private Object defaultValue;

        /** Exemple de valeur. */
        @JsonProperty("example")
        private String example;

        public String getType() { return type; }
        public void setType(String type) { this.type = type; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        public Boolean getReadOnly() { return readOnly; }
        public void setReadOnly(Boolean readOnly) { this.readOnly = readOnly; }
        public Object getDefaultValue() { return defaultValue; }
        public void setDefaultValue(Object defaultValue) { this.defaultValue = defaultValue; }
        public String getExample() { return example; }
        public void setExample(String example) { this.example = example; }
    }

    /**
     * Représente un utilitaire Penpot.
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class Utility {
        /** Type de l'utilitaire. */
        @JsonProperty("type")
        private String type;

        /** Description de l'utilitaire. */
        @JsonProperty("description")
        private String description;

        /** Signature (pour les fonctions). */
        @JsonProperty("signature")
        private String signature;

        /** Exemple d'utilisation. */
        @JsonProperty("usage")
        private String usage;

        public String getType() { return type; }
        public void setType(String type) { this.type = type; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        public String getSignature() { return signature; }
        public void setSignature(String signature) { this.signature = signature; }
        public String getUsage() { return usage; }
        public void setUsage(String usage) { this.usage = usage; }
    }

    /**
     * Représente un pattern de code courant.
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class CommonPattern {
        /** Description du pattern. */
        @JsonProperty("description")
        private String description;

        /** Code du pattern. */
        @JsonProperty("code")
        private List<String> code = new ArrayList<>();

        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        public List<String> getCode() { return code; }
        public void setCode(List<String> code) { this.code = code; }
    }

    // Getters et setters principaux
    public String getVersion() { return version; }
    public void setVersion(String version) { this.version = version; }
    public String getLastUpdate() { return lastUpdate; }
    public void setLastUpdate(String lastUpdate) { this.lastUpdate = lastUpdate; }
    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public Map<String, Category> getCategories() { return categories; }
    public void setCategories(Map<String, Category> categories) { this.categories = categories; }
    public List<ApiMethod> getMethods() { return methods; }
    public void setMethods(List<ApiMethod> methods) { this.methods = methods; }
    public Map<String, CommonProperty> getCommonProperties() { return commonProperties; }
    public void setCommonProperties(Map<String, CommonProperty> commonProperties) { 
        this.commonProperties = commonProperties; 
    }
    public Map<String, Utility> getUtilities() { return utilities; }
    public void setUtilities(Map<String, Utility> utilities) { this.utilities = utilities; }
    public Map<String, CommonPattern> getCommonPatterns() { return commonPatterns; }
    public void setCommonPatterns(Map<String, CommonPattern> commonPatterns) { 
        this.commonPatterns = commonPatterns; 
    }
    public List<String> getTips() { return tips; }
    public void setTips(List<String> tips) { this.tips = tips; }

    /**
     * Retourne l'ensemble des catégories uniques présentes dans la documentation.  
     *
     * @return ensemble trié de chaînes représentant les catégories disponibles
     */
    public Set<String> getAllCategories() {
        return categories != null ? categories.keySet() : Set.of();
    }
}