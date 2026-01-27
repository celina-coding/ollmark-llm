package com.penpot.mcp.core.ports.out;

import java.util.*;

/**
 * Port de sortie pour accéder à la documentation de l'API Penpot.
 * Abstraction suivant le principe d'inversion de dépendances (DIP).
 * Permet de découpler le domaine de l'implémentation concrète.
 */
public interface ApiDocumentationPort {

    /**
     * Obtient la documentation d'un type API.
     * 
     * @param typeName le nom du type (ex: "Shape", "Board")
     * @param memberName le nom du membre optionnel (propriété ou méthode)
     * @return la documentation si trouvée, Optional.empty() sinon
     */
    Optional<String> getTypeInfo(String typeName, String memberName);

    /**
     * Obtient la vue d'ensemble de l'API Penpot.
     * 
     * @return la documentation générale de l'API
     */
    String getOverview();

    /**
     * Obtient la liste de tous les noms de types disponibles.
     * 
     * @return la liste des noms de types
     */
    List<String> getAllTypeNames();

    /**
     * Recherche des types contenant un mot-clé.
     * 
     * @param keyword le mot-clé à rechercher
     * @return la liste des types correspondants
     */
    List<String> searchTypes(String keyword);

    /**
     * Vérifie si un type existe dans la documentation.
     * 
     * @param typeName le nom du type
     * @return true si le type existe
     */
    boolean typeExists(String typeName);
}