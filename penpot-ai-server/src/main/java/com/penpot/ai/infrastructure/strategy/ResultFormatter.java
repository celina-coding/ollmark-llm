package com.penpot.ai.infrastructure.strategy;

/**
 * Strategy Pattern pour le formatage des résultats.
 * Permet d'ajouter de nouveaux formats sans modifier le code existant (OCP).
 */
public interface ResultFormatter {

    /**
     * Formate un résultat dans une représentation textuelle.
     * 
     * @param result l'objet à formater
     * @return la représentation formatée
     */
    String format(Object result);

    /**
     * Vérifie si ce formatter supporte le type donné.
     * 
     * @param resultType le type de résultat
     * @return true si supporté
     */
    boolean supports(Class<?> resultType);

    /**
     * Priorité du formatter (plus élevé = vérifié en premier).
     * Utile quand plusieurs formatters supportent le même type.
     * 
     * @return la priorité (par défaut 0)
     */
    default int priority() {
        return 0;
    }
}