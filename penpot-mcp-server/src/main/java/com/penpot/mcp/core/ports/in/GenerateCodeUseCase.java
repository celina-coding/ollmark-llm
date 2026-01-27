package com.penpot.mcp.core.ports.in;

import com.penpot.mcp.core.domain.GenerateCodeResult;

/**
 * Port d'entrée pour la génération et l'exécution de code via AI.
 * Use case suivant le principe de ségrégation des interfaces (ISP).
 */
public interface GenerateCodeUseCase {

    /**
     * Génère du code JavaScript via AI puis l'exécute dans le plugin.
     * 
     * @param task la description de la tâche à accomplir
     * @param context le contexte additionnel pour la génération
     * @param userToken token optionnel pour le mode multi-utilisateur
     * @param executeImmediately true pour exécuter le code généré immédiatement
     * @return le résultat contenant le code généré et optionnellement le résultat d'exécution
     */
    GenerateCodeResult generate(
        String task, 
        String context, 
        String userToken,
        boolean executeImmediately
    );
}