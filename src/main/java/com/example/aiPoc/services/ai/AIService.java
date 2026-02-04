package com.example.aiPoc.services.ai;

/**
 * Interface contractuelle pour les services d’intelligence artificielle (IA).
 *
 * <p>
 * Elle définit les opérations de base permettant d’interagir avec un modèle
 * conversationnel ou génératif via un prompt textuel.  
 * L’objectif est de fournir une abstraction indépendante du fournisseur,
 * facilitant ainsi la substitution ou l’extension du service
 * sans modifier le code métier.
 * </p>
 */
public interface AIService {

    /**
     * Envoie un prompt textuel au service d’IA et retourne la réponse générée.
     *
     * <p>
     * Cette méthode constitue l’entrée principale pour interagir avec le modèle.
     * La nature exacte de la réponse (texte brut, JSON, contenu enrichi) dépend
     * de l’implémentation spécifique du fournisseur.
     * </p>
     *
     * @param prompt le texte représentant la requête à transmettre à l’IA
     * @return la réponse textuelle générée par le modèle d’IA
     * @throws RuntimeException si la communication avec le service échoue ou
     *                          si une erreur de traitement survient côté API
     */
    String chat(String prompt);

    /**
     * Envoie un prompt au service d’IA avec des paramètres de génération spécifiques.
     *
     * <p>
     * Cette surcharge permet d’ajuster la taille maximale de la réponse et
     * le niveau de créativité (température) selon le besoin de la tâche.
     * </p>
     *
     * <ul>
     *   <li><b>maxTokens</b> : limite le nombre maximum de tokens dans la réponse générée.</li>
     *   <li><b>temperature</b> : contrôle la variabilité des résultats (0.0 pour des réponses déterministes, jusqu’à 1.0 pour plus de diversité).</li>
     * </ul>
     *
     * <p>
     * Par défaut, cette implémentation délègue à {@link #chat(String)}.
     * Les classes concrètes peuvent redéfinir cette méthode pour tirer parti
     * de paramètres avancés fournis par leur API respective.
     * </p>
     *
     * @param prompt       le texte représentant la requête à transmettre
     * @param maxTokens    le nombre maximum de tokens à générer dans la réponse
     * @param temperature  la température de génération (comprise entre 0.0 et 1.0)
     * @return la réponse textuelle générée par l’IA
     * @throws RuntimeException si l’appel au service échoue ou si une erreur d’exécution est détectée
     */
    default String chat(String prompt, int maxTokens, double temperature) {
        return chat(prompt);
    }


}