package com.penpot.ai.core.domain;

/**
 * Catégories fonctionnelles des tools Penpot.
 *
 * <h2>Rôle dans l'architecture du router</h2>
 * Ce enum est le <b>contrat central</b> entre le router (phi3:mini) et le registry.
 * Il constitue le point d'extension OCP : ajouter une nouvelle catégorie ne nécessite
 * que d'enrichir cet enum et d'enregistrer les tools correspondants dans
 * {@link com.penpot.ai.application.router.PenpotToolRegistry}.
 *
 * <h2>Principe de nommage</h2>
 * Chaque valeur reflète une <i>intention utilisateur</i> claire et exclusive,
 * pas un nom de classe Java. Cela permet au router LLM de classifier
 * correctement sans connaître l'implémentation.
 */
public enum ToolCategory {

    /**
     * Création de nouvelles formes géométriques.
     * Ex: rectangles, ellipses, boards, frames vides.
     */
    SHAPE_CREATION,

    /**
     * Modification structurelle de formes existantes.
     * Ex: déplacer, redimensionner, dupliquer, cloner.
     */
    SHAPE_MODIFICATION,

    /**
     * Apparence visuelle : couleurs, dégradés, contours, ombres, opacité.
     * Ex: changer la couleur de remplissage, appliquer un gradient.
     */
    COLOR_AND_STYLE,

    /**
     * Organisation spatiale des éléments sur la page.
     * Ex: aligner, distribuer, grouper, dégrouper.
     */
    LAYOUT_AND_ALIGNMENT,

    /**
     * Contenu textuel et médias.
     * Ex: créer un titre, un paragraphe, insérer une image.
     */
    CONTENT_AND_TEXT,

    /**
     * Gestion des assets partagés (composants, styles globaux).
     * Ex: créer un composant, lier un style de couleur.
     */
    ASSET_MANAGEMENT,

    /**
     * Inspection et lecture de l'état de la page.
     * Ex: lister les éléments, obtenir les propriétés d'une forme.
     * Toujours inclus comme fallback sémantique si le contexte est ambigu.
     */
    INSPECTION,

    /**
     * Suppression définitive d'éléments.
     * Ex: supprimer une forme, vider une sélection.
     */
    DELETION,

    /**
     * Recherche sémantique de templates marketing via RAG.
     * Ex: "cherche un template Instagram", "montre-moi des posters A3".
     */
    TEMPLATE_SEARCH
}