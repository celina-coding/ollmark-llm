package com.penpot.mcp.tools;

import com.penpot.mcp.model.*;
import com.penpot.mcp.service.PenpotAiService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Outil pour obtenir une vue d'ensemble de l'API Penpot.
 * <p>
 * Cet outil fournit un accès simplifié à la documentation de haut niveau
 * de l'API Penpot, utile pour donner un contexte général aux utilisateurs
 * ou aux systèmes AI.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class HighLevelOverviewTool {

    /** Service AI pour accéder à la vue d'ensemble */
    private final PenpotAiService aiService;

    /**
     * Exécute l'outil pour obtenir la vue d'ensemble de l'API.
     *
     * @return la documentation d'ensemble au format texte/markdown
     */
    public String execute() {
        try {
            log.info("Executing tool: high_level_overview");
            return aiService.getPenpotOverview();
        } catch (Exception e) {
            log.error("Tool execution failed", e);
            return "Tool execution failed: " + e.getMessage();
        }
    }
}