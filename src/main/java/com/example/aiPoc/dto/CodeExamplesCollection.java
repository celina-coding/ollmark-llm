package com.example.aiPoc.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.*;

/**
 * Représente une collection d'exemples de code, telle que décrite dans le fichier
 * JSON <b>code-examples.json</b>.  
 * Cette classe est utilisée pour sérialiser et désérialiser les exemples de code
 * utilisés par le système d'IA ou pour les démonstrations techniques.
 *
 * <p>Elle contient des métadonnées générales (version, date de mise à jour)
 * ainsi qu'une liste d'objets {@link CodeExample} représentant chaque exemple individuel.</p>
 *
 * <p>La classe utilise les annotations Jackson pour le mapping JSON ↔ Java.</p>
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CodeExamplesCollection {

    /** Version du fichier ou du schéma. */
    @JsonProperty("version")
    private String version;

    /** Date de la dernière mise à jour de la collection. */
    @JsonProperty("lastUpdate")
    private String lastUpdate;

    @JsonProperty("source")
    private String source;

    @JsonProperty("description")
    private String description;

    /** Liste des exemples de code disponibles. */
    @JsonProperty("examples")
    private List<CodeExample> examples = new ArrayList<>();

    /**
     * Représente un exemple de code unique avec ses métadonnées descriptives.
     * Chaque instance contient des informations comme l'identifiant, le titre,
     * la catégorie, le niveau de difficulté, les tags associés, une description,
     * et le contenu du code.
     *
     * <p>Cette classe est utilisée à la fois pour l’affichage et pour le
     * traitement des exemples par les modèles d’IA.</p>
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class CodeExample {

        /** Identifiant unique de l'exemple. */
        @JsonProperty("id")
        private String id;

        /** Titre court décrivant l'exemple. */
        @JsonProperty("title")
        private String title;

        /** Catégorie fonctionnelle ou thématique de l'exemple. */
        @JsonProperty("category")
        private String category;

        /** Niveau de difficulté (ex. : facile, moyen, avancé). */
        @JsonProperty("tags")
        private List<String> tags = new ArrayList<>();

        /** Brève description de l'exemple. */
        @JsonProperty("description")
        private String description;

        /** Contenu du code source associé à l'exemple. */
        @JsonProperty("code")
        private String code;

        /**
         * Retourne une chaîne formatée contenant un aperçu de l’exemple,
         * composée du titre, de la catégorie et du code.
         *
         * @return chaîne de texte prête à être affichée en console ou journalisée
         */
        public String formattedPreview() {
            return "// " + title + " [" + category + "]\n" + code;
        }

        /**
         * Retourne l'identifiant de l'exemple.
         * @return identifiant unique
         */
        public String getId() { return id; }

        /**
         * Définit l'identifiant de l'exemple.
         * @param id identifiant unique
         */
        public void setId(String id) { this.id = id; }

        /**
         * Retourne le titre de l'exemple.
         * @return titre de l'exemple
         */
        public String getTitle() { return title; }

        /**
         * Définit le titre de l'exemple.
         * @param title titre court
         */
        public void setTitle(String title) { this.title = title; }

        /**
         * Retourne la catégorie de l'exemple.
         * @return catégorie
         */
        public String getCategory() { return category; }

        /**
         * Définit la catégorie de l'exemple.
         * @param category nom de la catégorie
         */
        public void setCategory(String category) { this.category = category; }

        /**
         * Retourne la liste des tags associés.
         * @return liste de mots-clés
         */
        public List<String> getTags() { return tags; }

        /**
         * Définit la liste des tags associés à l'exemple.
         * @param tags liste de mots-clés
         */
        public void setTags(List<String> tags) { this.tags = tags; }

        /**
         * Retourne la description de l'exemple.
         * @return description textuelle
         */
        public String getDescription() { return description; }

        /**
         * Définit la description de l'exemple.
         * @param description description textuelle
         */
        public void setDescription(String description) { this.description = description; }

        /**
         * Retourne le contenu du code source.
         * @return code source
         */
        public String getCode() { return code; }


        /**
         * Définit le contenu du code source.
         * @param code texte du code
         */
        public void setCode(String code) { this.code = code; }
    }

    /**
     * Retourne la version de la collection d'exemples.
     * @return version
     */
    public String getVersion() { return version; }

    /**
     * Définit la version de la collection d'exemples.
     * @param version version à définir
     */
    public void setVersion(String version) { this.version = version; }

    /**
     * Retourne la date de dernière mise à jour.
     * @return date ISO 8601 ou autre format
     */
    public String getLastUpdate() { return lastUpdate; }

    /**
     * Définit la date de dernière mise à jour.
     * @param lastUpdate date ISO 8601 ou autre format
     */
    public void setLastUpdate(String lastUpdate) { this.lastUpdate = lastUpdate; }

    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    
    /**
     * Retourne la liste complète des exemples de code.
     * @return liste d'exemples
     */
    public List<CodeExample> getExamples() { return examples; }

    /**
     * Définit la liste complète des exemples de code.
     * @param examples liste d'exemples
     */
    public void setExamples(List<CodeExample> examples) { this.examples = examples; }
}