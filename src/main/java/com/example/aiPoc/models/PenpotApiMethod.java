package com.example.aiPoc.models;

import java.util.ArrayList;
import java.util.List;

/**
 * Représente une méthode du SDK Penpot pouvant être utilisée pour générer du code ou documenter les capacités de l'API Penpot.
 *
 * <p>Ce modèle permet de décrire une méthode exposée par le SDK, incluant son nom, sa description,
 * son type de retour, ses paramètres, un exemple d’utilisation ainsi qu'une catégorie permettant
 * de mieux organiser les différentes fonctionnalités (ex. : SHAPE_CREATION, TEXT, STYLING, etc.).</p>
 */
public class PenpotApiMethod {

    /** Nom de la méthode telle qu'exposée par le SDK Penpot. */
    private String name;

    /** Brève description du rôle ou de l’objectif de la méthode. */
    private String description;

    /** Type de valeur retournée par la méthode (ex. : void, Shape, List<TextNode>). */
    private String returnType;

    /** Liste des paramètres attendus par la méthode. */
    private List<Parameter> parameters = new ArrayList<>();

    /** Exemple d'utilisation de la méthode, généralement sous forme de snippet de code. */
    private String example;

    /** Catégorie fonctionnelle de la méthode (ex. : SHAPE_CREATION, TEXT, STYLING). */
    private String category;

    /**
     * Classe interne représentant un paramètre d'une méthode du SDK Penpot.
     *
     * <p>Un paramètre inclut son nom, son type, s’il est obligatoire, sa valeur par défaut éventuelle
     * ainsi qu'une description facilitant la compréhension de son usage.</p>
     */
    public static class Parameter {

        /** Nom du paramètre. */
        private String name;

        /** Type attendu pour le paramètre (ex. : String, Number, Boolean, Shape). */
        private String type;

        /** Indique si le paramètre est obligatoire. */
        private boolean required;

        /** Valeur par défaut si le paramètre est optionnel. */
        private String defaultValue;

        /** Description du paramètre et de son usage. */
        private String description;

        /**
         * Constructeur par défaut.
         */
        public Parameter() {
        }

        /**
         * Constructeur minimal définissant le nom, le type et la contrainte d'obligation du paramètre.
         *
         * @param name     nom du paramètre
         * @param type     type attendu
         * @param required {@code true} si le paramètre est obligatoire, sinon {@code false}
         */
        public Parameter(String name, String type, boolean required) {
            this.name = name;
            this.type = type;
            this.required = required;
        }

        /**
         * Retourne le nom du paramètre.
         *
         * @return nom du paramètre
         */
        public String getName() {
            return name;
        }

        /**
         * Définit le nom du paramètre.
         *
         * @param name nom du paramètre
         */
        public void setName(String name) {
            this.name = name;
        }

        /**
         * Retourne le type du paramètre.
         *
         * @return type attendu
         */
        public String getType() {
            return type;
        }

        /**
         * Définit le type du paramètre.
         *
         * @param type type attendu
         */
        public void setType(String type) {
            this.type = type;
        }

        /**
         * Indique si le paramètre est obligatoire.
         *
         * @return {@code true} si obligatoire, sinon {@code false}
         */
        public boolean isRequired() {
            return required;
        }

        /**
         * Définit si le paramètre est obligatoire.
         *
         * @param required {@code true} si obligatoire, sinon {@code false}
         */
        public void setRequired(boolean required) {
            this.required = required;
        }

        /**
         * Retourne la valeur par défaut du paramètre lorsqu’il est optionnel.
         *
         * @return valeur par défaut ou {@code null} si non applicable
         */
        public String getDefaultValue() {
            return defaultValue;
        }

        /**
         * Définit la valeur par défaut du paramètre lorsqu’il est optionnel.
         *
         * @param defaultValue valeur par défaut
         */
        public void setDefaultValue(String defaultValue) {
            this.defaultValue = defaultValue;
        }

        /**
         * Retourne la description du paramètre.
         *
         * @return description du paramètre
         */
        public String getDescription() {
            return description;
        }

        /**
         * Définit la description du paramètre.
         *
         * @param description description du paramètre
         */
        public void setDescription(String description) {
            this.description = description;
        }
    }

    /**
     * Constructeur par défaut.
     */
    public PenpotApiMethod() {
    }

    /**
     * Constructeur permettant de définir une méthode du SDK Penpot avec son nom, sa description et son type de retour.
     *
     * @param name        nom de la méthode
     * @param description description de la méthode
     * @param returnType  type de retour de la méthode
     */
    public PenpotApiMethod(String name, String description, String returnType) {
        this.name = name;
        this.description = description;
        this.returnType = returnType;
    }

    /**
     * Retourne le nom de la méthode.
     *
     * @return nom de la méthode
     */
    public String getName() {
        return name;
    }

    /**
     * Définit le nom de la méthode.
     *
     * @param name nom de la méthode
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Retourne la description de la méthode.
     *
     * @return description
     */
    public String getDescription() {
        return description;
    }

    /**
     * Définit la description de la méthode.
     *
     * @param description description
     */
    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * Retourne le type de retour de la méthode.
     *
     * @return type de retour
     */
    public String getReturnType() {
        return returnType;
    }

    /**
     * Définit le type de retour de la méthode.
     *
     * @param returnType type de retour
     */
    public void setReturnType(String returnType) {
        this.returnType = returnType;
    }

    /**
     * Retourne la liste des paramètres attendus par la méthode.
     *
     * @return liste des paramètres
     */
    public List<Parameter> getParameters() {
        return parameters;
    }

    /**
     * Définit la liste des paramètres attendus par la méthode.
     *
     * @param parameters liste des paramètres
     */
    public void setParameters(List<Parameter> parameters) {
        this.parameters = parameters;
    }

    /**
     * Retourne un exemple d'utilisation de la méthode.
     *
     * @return exemple d'utilisation
     */
    public String getExample() {
        return example;
    }

    /**
     * Définit un exemple d'utilisation de la méthode.
     *
     * @param example exemple à fournir
     */
    public void setExample(String example) {
        this.example = example;
    }

    /**
     * Retourne la catégorie fonctionnelle de la méthode (ex. : TEXT, SHAPE_CREATION).
     *
     * @return catégorie de la méthode
     */
    public String getCategory() {
        return category;
    }

    /**
     * Définit la catégorie fonctionnelle de la méthode (ex. : STYLING, COMPONENTS).
     *
     * @param category catégorie de la méthode
     */
    public void setCategory(String category) {
        this.category = category;
    }
}