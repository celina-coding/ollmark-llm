package com.penpot.mcp.core.ports.in;

import com.penpot.mcp.model.MarketingTemplate;
import java.util.List;

/**
 * Port d'entrée pour la recherche de templates marketing via RAG.
 * Use case suivant le principe de ségrégation des interfaces (ISP).
 */
public interface SearchTemplatesUseCase {

    /**
     * Recherche des templates par requête textuelle en utilisant RAG.
     * 
     * <h3>Recherche sémantique</h3>
     * Utilise les embeddings vectoriels pour trouver les templates
     * les plus similaires à la requête, même si les mots exacts ne correspondent pas.
     * 
     * @param query requête en langage naturel
     * @return liste des templates correspondants, triés par pertinence
     * @throws ValidationException    si la requête est invalide
     * @throws ToolExecutionException si la recherche échoue
     */
    List<MarketingTemplate> searchByQuery(String query);

    /**
     * Recherche des templates par type exact.
     * 
     * @param type type de template recherché
     * @return liste des templates de ce type
     * @throws ValidationException    si le type est invalide
     * @throws ToolExecutionException si la recherche échoue
     */
    List<MarketingTemplate> searchByType(String type);

    /**
     * Recherche des templates par tag.
     * 
     * @param tag tag recherché
     * @return liste des templates ayant ce tag
     * @throws ValidationException    si le tag est invalide
     * @throws ToolExecutionException si la recherche échoue
     */
    List<MarketingTemplate> searchByTag(String tag);

    /**
     * Récupère tous les templates disponibles.
     * 
     * <h3>Utilisation</h3>
     * Utile pour :
     * <ul>
     *     <li>Explorer le catalogue complet</li>
     *     <li>Construire des interfaces de sélection</li>
     *     <li>Obtenir des statistiques</li>
     * </ul>
     * 
     * @return liste de tous les templates
     * @throws ToolExecutionException si la récupération échoue
     */
    List<MarketingTemplate> getAllTemplates();
}