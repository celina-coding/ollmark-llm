package com.penpot.mcp.model;

import com.fasterxml.jackson.annotation.*;
import lombok.*;
import java.util.*;

/**
 * Représente un template marketing pour Penpot.
 * Contient les métadonnées et la "design recipe" (recette de design)
 * qui sera interprétée pour générer le code JavaScript Penpot.
 * 
 * Structure JSON :
 * {
 *   "id": "template_001",
 *   "type": "social_media_post",
 *   "tags": ["social", "product"],
 *   "description": "Post Instagram...",
 *   "design_recipe": {
 *     "canvas_size": "1080x1920",
 *     "background_mode": "full_image",
 *     ...
 *   }
 * }
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class MarketingTemplate {
    
    /**
     * Identifiant unique du template (ex: "insta_story_bakery_morning_deal")
     */
    private String id;
    
    /**
     * Type de template (ex: "social_media_story", "poster_a3", "email")
     */
    private String type;
    
    /**
     * Tags pour faciliter la recherche sémantique
     */
    private List<String> tags;
    
    /**
     * Description détaillée du template
     */
    private String description;
    
    /**
     * Recette de design : structure déclarative qui sera interprétée
     * pour générer le code JavaScript Penpot correspondant.
     * 
     * Contient des propriétés comme :
     * - canvas_size, format
     * - background_mode, background_color
     * - layout_mode, layout_direction
     * - main_element, text_overlay
     * - sections (pour emails)
     * - etc.
     */
    @JsonProperty("design_recipe")
    private Map<String, Object> designRecipe;
    
    /**
     * Métadonnées additionnelles (optionnel)
     */
    private Map<String, Object> metadata;
}