package com.example.aiPoc.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Représente la structure de données issue du fichier <b>penpot-api-summary.json</b>.  
 * Ce DTO permet de charger dynamiquement la documentation des méthodes exposées par le SDK Penpot.
 *
 * <p>Il inclut les métadonnées globales de la documentation (version, date de mise à jour, source),
 * ainsi qu’une liste détaillée des méthodes, propriétés communes et utilitaires disponibles.</p>
 *
 * <p>Les annotations Jackson facilitent la sérialisation et la désérialisation JSON ↔ Java.</p>
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PenpotApiDocumentation {

    /** Version actuelle de la documentation Penpot. */
    @JsonProperty("version")
    private String version;

    /** Date de dernière mise à jour du fichier source. */
    @JsonProperty("lastUpdate")
    private String lastUpdate;

    /** Chemin ou URL source d’origine de la documentation. */
    @JsonProperty("source")
    private String source;

    /** Liste complète des méthodes documentées dans le SDK Penpot. */
    @JsonProperty("methods")
    private List<ApiMethod> methods = new ArrayList<>();

    /** Ensemble des propriétés communes aux différentes classes ou composants du SDK. */
    @JsonProperty("commonProperties")
    private Map<String, Map<String, String>> commonProperties = new HashMap<>();

    /** Méthodes utilitaires ou raccourcis globaux proposés par le SDK. */
    @JsonProperty("utilities")
    private Map<String, String> utilities = new HashMap<>();

    /**
     * Représente une méthode du SDK Penpot.  
     * Contient les informations nécessaires à sa description, ses paramètres,
     * son type de retour et des exemples d’utilisation.
     *
     * <p>Cette structure est utilisée pour générer des aides contextuelles
     * ou pour effectuer des vérifications automatiques dans le cadre d’un SDK client.</p>
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

        /** Catégorie ou module auquel appartient la méthode. */
        @JsonProperty("category")
        private String category;

        /** Liste des paramètres nécessaires à l’appel de la méthode. */
        @JsonProperty("parameters")
        private List<Parameter> parameters = new ArrayList<>();

        /** Ensemble des propriétés associées à la méthode (métadonnées, options, etc.). */
        @JsonProperty("properties")
        private Map<String, String> properties = new HashMap<>();

        /** Ensemble des sous-méthodes ou méthodes internes liées à celle-ci. */
        @JsonProperty("methods")
        private Map<String, String> methods = new HashMap<>();

        /** Exemple de code illustrant l’utilisation de la méthode. */
        @JsonProperty("example")
        private String example;

        /** Notes complémentaires ou remarques sur l’utilisation de la méthode. */
        @JsonProperty("notes")
        private String notes;

        /**
         * Génère une signature formatée de la méthode en incluant ses paramètres.
         *
         * @return signature de la méthode sous la forme <code>penpot.nom(param: type, ...)</code>
         */
        public String formatSignature() {
            String params = parameters == null ? "" :
                    parameters.stream()
                            .map(p -> p.getName() + ": " + p.getType())
                            .reduce((a, b) -> a + ", " + b)
                            .orElse("");
            return "penpot." + name + "(" + params + ")";
        }

        /** @return le nom de la méthode */
        public String getName() { return name; }

        /** @param name définit le nom de la méthode */
        public void setName(String name) { this.name = name; }

        /** @return la description de la méthode */
        public String getDescription() { return description; }

        /** @param description définit la description fonctionnelle */
        public void setDescription(String description) { this.description = description; }

        /** @return le type de retour de la méthode */
        public String getReturnType() { return returnType; }

        /** @param returnType définit le type de retour */
        public void setReturnType(String returnType) { this.returnType = returnType; }

        /** @return la catégorie à laquelle appartient la méthode */
        public String getCategory() { return category; }

        /** @param category définit la catégorie de la méthode */
        public void setCategory(String category) { this.category = category; }

        /** @return la liste des paramètres d’entrée */
        public List<Parameter> getParameters() { return parameters; }

        /** @param parameters définit la liste des paramètres */
        public void setParameters(List<Parameter> parameters) { this.parameters = parameters; }

        /** @return les propriétés associées à la méthode */
        public Map<String, String> getProperties() { return properties; }

        /** @param properties définit les propriétés de la méthode */
        public void setProperties(Map<String, String> properties) { this.properties = properties; }

        /** @return les sous-méthodes internes */
        public Map<String, String> getMethods() { return methods; }

        /** @param methods définit les sous-méthodes internes */
        public void setMethods(Map<String, String> methods) { this.methods = methods; }

        /** @return l’exemple de code associé */
        public String getExample() { return example; }

        /** @param example définit l’exemple de code */
        public void setExample(String example) { this.example = example; }

        /** @return les notes ou remarques associées */
        public String getNotes() { return notes; }

        /** @param notes définit les notes complémentaires */
        public void setNotes(String notes) { this.notes = notes; }
    }

    /**
     * Représente un paramètre d’entrée d’une méthode du SDK Penpot.
     * Contient les informations nécessaires pour comprendre le rôle
     * et le type attendu du paramètre.
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class Parameter {

        /** Nom du paramètre. */
        @JsonProperty("name")
        private String name;

        /** Type de donnée attendu pour le paramètre (ex. String, Integer). */
        @JsonProperty("type")
        private String type;

        /** Indique si le paramètre est obligatoire. */
        @JsonProperty("required")
        private boolean required;

        /** Description du rôle du paramètre. */
        @JsonProperty("description")
        private String description;

        /** Valeur par défaut utilisée si le paramètre n’est pas fourni. */
        @JsonProperty("defaultValue")
        private String defaultValue;

        /** @return le nom du paramètre */
        public String getName() { return name; }

        /** @param name définit le nom du paramètre */
        public void setName(String name) { this.name = name; }

        /** @return le type du paramètre */
        public String getType() { return type; }

        /** @param type définit le type du paramètre */
        public void setType(String type) { this.type = type; }

        /** @return vrai si le paramètre est requis */
        public boolean isRequired() { return required; }

        /** @param required définit si le paramètre est requis */
        public void setRequired(boolean required) { this.required = required; }

        /** @return la description du paramètre */
        public String getDescription() { return description; }

        /** @param description définit la description du paramètre */
        public void setDescription(String description) { this.description = description; }

        /** @return la valeur par défaut du paramètre */
        public String getDefaultValue() { return defaultValue; }

        /** @param defaultValue définit la valeur par défaut du paramètre */
        public void setDefaultValue(String defaultValue) { this.defaultValue = defaultValue; }
    }

    /** @return la version de la documentation */
    public String getVersion() { return version; }

    /** @param version définit la version de la documentation */
    public void setVersion(String version) { this.version = version; }

    /** @return la date de dernière mise à jour */
    public String getLastUpdate() { return lastUpdate; }

    /** @param lastUpdate définit la date de dernière mise à jour */
    public void setLastUpdate(String lastUpdate) { this.lastUpdate = lastUpdate; }

    /** @return la source de la documentation */
    public String getSource() { return source; }

    /** @param source définit la source de la documentation */
    public void setSource(String source) { this.source = source; }

    /** @return la liste des méthodes du SDK */
    public List<ApiMethod> getMethods() { return methods; }

    /** @param methods définit la liste des méthodes */
    public void setMethods(List<ApiMethod> methods) { this.methods = methods; }

    /** @return les propriétés communes entre plusieurs entités */
    public Map<String, Map<String, String>> getCommonProperties() { return commonProperties; }

    /** @param commonProperties définit les propriétés communes */
    public void setCommonProperties(Map<String, Map<String, String>> commonProperties) {
        this.commonProperties = commonProperties;
    }

    /** @return les utilitaires globaux définis dans le SDK */
    public Map<String, String> getUtilities() { return utilities; }

    /** @param utilities définit les utilitaires globaux */
    public void setUtilities(Map<String, String> utilities) { this.utilities = utilities; }

    /**
     * Retourne l’ensemble des catégories uniques présentes dans la documentation.  
     * Cette méthode permet de regrouper ou filtrer les méthodes selon leur domaine fonctionnel.
     *
     * @return ensemble trié de chaînes représentant les catégories disponibles
     */
    public Set<String> getAllCategories() {
        if (methods == null) return Set.of();
        return methods.stream()
                .map(ApiMethod::getCategory)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(TreeSet::new));
    }
}