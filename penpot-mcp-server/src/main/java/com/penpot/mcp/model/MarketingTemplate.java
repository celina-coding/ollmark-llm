package com.penpot.mcp.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import java.util.List;

@Data
public class MarketingTemplate {
    private String id;
    private String type;
    private List<String> tags;
    private String description;

    @JsonProperty("design_recipe")
    private DesignRecipe designRecipe;

    @Data
    public static class DesignRecipe {
        @JsonProperty("canvas_size")
        private String canvasSize;
        
        @JsonProperty("background_mode")
        private String backgroundMode;
        
        @JsonProperty("overlay_gradient")
        private String overlayGradient;
        
        @JsonProperty("main_element")
        private MainElement mainElement;
        
        @JsonProperty("price_badge")
        private PriceBadge priceBadge;
    }

    @Data
    public static class MainElement {
        private String type;
        private String alignment;
        
        @JsonProperty("font_weight")
        private String fontWeight;
        
        @JsonProperty("font_size")
        private String fontSize;
    }

    @Data
    public static class PriceBadge {
        private String shape;
        private String position;
        private String contrast;
    }
}