package com.penpot.ai.core.ports.out;

import com.penpot.ai.core.domain.ToolCategory;

import java.util.Set;

/**
 * Port de sortie pour la classification d'intention utilisateur.
 *
 * Analyse un message et retourne l'ensemble des catégories de tools pertinentes.
 *
 * Les implémentations <b>ne doivent jamais lever d'exception</b> : en cas
 * d'échec de classification, elles retournent {@code Set.of(ToolCategory.INSPECTION)}
 * comme fallback minimal sûr.
 *
 * @see com.penpot.ai.application.router.IntentRouterService Implémentation phi3:mini
 */
public interface ToolRouterPort {

    /**
     * Analyse le message utilisateur et retourne l'ensemble des catégories
     * de tools à activer pour traiter la requête.
     *
     * <p>Le résultat est intentionnellement un {@link Set} (pas une {@link java.util.List})
     * pour garantir l'unicité des catégories et signifier qu'elles sont
     * non-ordonnées — le registry déterminera l'ordre de résolution.</p>
     *
     * @param userMessage message brut de l'utilisateur
     * @return ensemble non-null et non-vide des catégories pertinentes ;
     *         contient au minimum {@link com.penpot.ai.core.domain.ToolCategory#INSPECTION}
     *         en cas d'incertitude
     */
    Set<ToolCategory> route(String userMessage);
}