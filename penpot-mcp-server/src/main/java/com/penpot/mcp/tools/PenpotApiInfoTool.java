package com.penpot.mcp.tools;

import com.penpot.mcp.model.*;
import com.penpot.mcp.service.PenpotAiService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Outil pour consulter la documentation API Penpot.
 * <p>
 * Cet outil permet d'interroger la documentation API pour:
 * </p>
 * <ul>
 *   <li>Obtenir la documentation complète d'un type</li>
 *   <li>Obtenir la documentation d'un membre spécifique</li>
 * </ul>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PenpotApiInfoTool {

    /** Service AI pour accéder à la documentation */
    private final PenpotAiService aiService;

    /**
     * Exécute l'outil pour obtenir des informations sur un type ou membre API.
     *
     * @param typeName le nom du type API à consulter
     * @param memberName le nom du membre optionnel (peut être null)
     * @return la documentation formatée en texte/markdown
     */
    public String execute(String typeName, String memberName) {
        try {
            log.info("Executing tool: penpot_api_info for type: {}, member: {}", 
                typeName, memberName);
            return aiService.getApiTypeInfo(typeName, memberName);
        } catch (Exception e) {
            log.error("Tool execution failed", e);
            return "Tool execution failed: " + e.getMessage();
        }
    }
}