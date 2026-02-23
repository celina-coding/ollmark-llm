package com.penpot.ai.application.router;

import com.penpot.ai.core.domain.ToolCategory;

import java.util.Set;

/**
 * Interface de résolution des tools Spring AI à partir de catégories.
 *
 * <h2>Contrat</h2>
 * La méthode retourne toujours un tableau non-null. Si {@code categories}
 * est vide ou ne correspond à aucun tool connu, retourne un tableau vide
 * (l'appelant doit gérer ce cas avec un fallback).
 *
 * @see PenpotToolRegistry Implémentation principale
 */
public interface ToolCategoryResolver {

    /**
     * Résout et retourne les instances de tools Spring AI correspondant
     * aux catégories fournies.
     *
     * <p>Les outils sont dédupliqués : si deux catégories référencent le même
     * bean (ex: {@code SHAPE_CREATION} et {@code SHAPE_MODIFICATION} partagent
     * {@code PenpotShapeTools}), il n'apparaîtra qu'une seule fois dans le tableau.</p>
     *
     * @param categories ensemble de catégories déterminées par le router
     * @return tableau d'instances de tools à passer à {@code ChatClient.tools(...)}
     */
    Object[] resolveTools(Set<ToolCategory> categories);
}