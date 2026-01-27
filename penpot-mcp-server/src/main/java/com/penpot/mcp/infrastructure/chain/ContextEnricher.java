package com.penpot.mcp.infrastructure.chain;

import com.penpot.mcp.core.domain.AiContext;

/**
 * Chain of Responsibility Pattern pour l'enrichissement progressif du contexte AI.
 * Permet d'ajouter de nouveaux enrichisseurs sans modifier le code existant (OCP).
 */
public abstract class ContextEnricher {

    protected ContextEnricher next;

    /**
     * Configure le prochain enrichisseur dans la chaîne.
     * 
     * @param next le prochain enrichisseur
     */
    public void setNext(ContextEnricher next) {
        this.next = next;
    }

    /**
     * Enrichit le contexte et passe au suivant dans la chaîne.
     * Template Method Pattern : définit le squelette de l'algorithme.
     * 
     * @param context le contexte à enrichir
     * @return le contexte enrichi
     */
    public final AiContext enrich(AiContext context) {
        AiContext enriched = doEnrich(context);
        if (next != null) return next.enrich(enriched);
        return enriched;
    }

    /**
     * Méthode abstraite à implémenter par les enrichisseurs concrets.
     * Hook method du Template Method Pattern.
     * 
     * @param context le contexte à enrichir
     * @return le contexte enrichi
     */
    protected abstract AiContext doEnrich(AiContext context);

    /**
     * Vérifie si cet enrichisseur doit traiter le contexte.
     * Hook method optionnel.
     * 
     * @param context le contexte
     * @return true si l'enrichissement doit être appliqué
     */
    protected boolean shouldEnrich(AiContext context) {
        return true;
    }
}