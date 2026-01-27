package com.penpot.mcp.core.ports.in;

import com.penpot.mcp.model.MarketingTemplate;
import java.util.List;

/**
 * Port d'entrée pour la recherche de templates marketing via RAG.
 * Use case suivant le principe de ségrégation des interfaces (ISP).
 */
public interface SearchTemplatesUseCase {
    
    /**
     * Recherche des templates par requête sémantique (RAG).
     * Utilise la recherche vectorielle pour trouver les templates
     * les plus pertinents par rapport à la requête.
     *
     * @param query la requête de l'utilisateur
     * @return liste des templates correspondants, triés par pertinence
     */
    List<MarketingTemplate> searchByQuery(String query);
    
    /**
     * Recherche des templates par type.
     *
     * @param type le type recherché
     * @return liste des templates de ce type
     */
    List<MarketingTemplate> searchByType(String type);
    
    /**
     * Recherche des templates par tag.
     *
     * @param tag le tag recherché
     * @return liste des templates ayant ce tag
     */
    List<MarketingTemplate> searchByTag(String tag);
    
    /**
     * Récupère tous les templates disponibles.
     *
     * @return liste de tous les templates
     */
    List<MarketingTemplate> getAllTemplates();
}