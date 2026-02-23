package com.penpot.ai.application.router;

import com.penpot.ai.application.tools.*;
import com.penpot.ai.core.domain.ToolCategory;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Registry des tools Penpot organisés par catégorie fonctionnelle.
 *
 * <h2>Pattern Registry</h2>
 * Ce composant maintient une {@link Map} statique catégorie → liste de tools.
 * C'est le point central de configuration du routing : modifier les associations
 * ne nécessite de toucher qu'à cette classe.
 *
 * Pour ajouter un nouveau tool :
 * <ol>
 *   <li>Créer la classe tool ({@code @Component}).</li>
 *   <li>L'injecter dans le constructeur via {@code @RequiredArgsConstructor}.</li>
 *   <li>L'associer à une ou plusieurs {@link ToolCategory} dans {@link #buildRegistry()}.</li>
 * </ol>
 * Aucune autre classe ne doit être modifiée.
 *
 * <h2>Déduplication automatique</h2>
 * Un même bean peut apparaître dans plusieurs catégories (ex: {@code PenpotShapeTools}
 * couvre {@code SHAPE_CREATION} et {@code SHAPE_MODIFICATION}). La méthode
 * {@link ToolCategoryResolver#resolveTools(Set)} déduplique via {@code distinct()} sur les
 * références d'objet Java (identité, pas égalité structurelle).
 */
@Slf4j
@Component
public class PenpotToolRegistry implements ToolCategoryResolver {

    // ==================== INJECTIONS DES TOOLS ====================

    private final PenpotShapeTools    shapeTools;
    private final PenpotTransformTools transformTools;
    private final PenpotAssetTools    assetTools;
    private final PenpotLayoutTools   layoutTools;
    private final PenpotContentTools  contentTools;
    private final PenpotDeleteTools   deleteTools;
    private final PenpotInspectorTools inspectorTools;
    private final TemplateSearchTools  templateSearchTools;

    // Le registry immuable construit au démarrage
    private Map<ToolCategory, List<Object>> registry;

    public PenpotToolRegistry(
        PenpotShapeTools shapeTools,
        PenpotTransformTools transformTools,
        PenpotAssetTools assetTools,
        PenpotLayoutTools layoutTools,
        PenpotContentTools contentTools,
        PenpotDeleteTools deleteTools,
        PenpotInspectorTools inspectorTools,
        TemplateSearchTools templateSearchTools
    ) {
        this.shapeTools         = shapeTools;
        this.transformTools     = transformTools;
        this.assetTools         = assetTools;
        this.layoutTools        = layoutTools;
        this.contentTools       = contentTools;
        this.deleteTools        = deleteTools;
        this.inspectorTools     = inspectorTools;
        this.templateSearchTools = templateSearchTools;
    }

    // ==================== INITIALISATION ====================

    /**
     * Construit le registry au démarrage de l'application.
     *
     * <p>Utilisation de {@link Map#copyOf} pour rendre le registry immuable
     * après construction — aucune modification possible à l'exécution.</p>
     */
    @PostConstruct
    void buildRegistry() {
        Map<ToolCategory, List<Object>> mutable = new EnumMap<>(ToolCategory.class);

        // --- Création de formes ---
        mutable.put(ToolCategory.SHAPE_CREATION, List.of(shapeTools));

        // --- Modification de formes (transform + shape partagés) ---
        mutable.put(ToolCategory.SHAPE_MODIFICATION, List.of(shapeTools, transformTools));

        // --- Couleurs et styles (assets gère les palettes) ---
        mutable.put(ToolCategory.COLOR_AND_STYLE, List.of(assetTools));

        // --- Mise en page et alignement ---
        mutable.put(ToolCategory.LAYOUT_AND_ALIGNMENT, List.of(layoutTools));

        // --- Contenu textuel et médias ---
        mutable.put(ToolCategory.CONTENT_AND_TEXT, List.of(contentTools));

        // --- Gestion des assets partagés ---
        mutable.put(ToolCategory.ASSET_MANAGEMENT, List.of(assetTools));

        // --- Inspection (toujours inclus comme contexte) ---
        mutable.put(ToolCategory.INSPECTION, List.of(inspectorTools));

        // --- Suppression ---
        mutable.put(ToolCategory.DELETION, List.of(deleteTools, inspectorTools));

        // --- Recherche de templates RAG ---
        mutable.put(ToolCategory.TEMPLATE_SEARCH, List.of(templateSearchTools));

        this.registry = Map.copyOf(mutable);

        logRegistryStats();
    }

    // ==================== RÉSOLUTION ====================

    /**
     * {@inheritDoc}
     *
     * <p>Algorithme :
     * <ol>
     *   <li>Pour chaque catégorie, récupère la liste de tools du registry.</li>
     *   <li>Aplatit les listes en un seul stream.</li>
     *   <li>Déduplique sur l'identité d'objet (référence Java).</li>
     *   <li>Retourne un tableau {@code Object[]} attendu par {@code ChatClient.tools(...)}.</li>
     * </ol>
     */
    @Override
    public Object[] resolveTools(Set<ToolCategory> categories) {
        if (categories == null || categories.isEmpty()) {
            log.warn("[Registry] Empty categories set, returning empty tools array");
            return new Object[0];
        }

        Object[] tools = categories.stream()
            .flatMap(category -> {
                List<Object> toolsForCategory = registry.getOrDefault(category, List.of());
                if (toolsForCategory.isEmpty()) {
                    log.warn("[Registry] No tools registered for category: {}", category);
                }
                return toolsForCategory.stream();
            })
            .collect(Collectors.toCollection(() ->
                Collections.newSetFromMap(new IdentityHashMap<>())))
            .toArray();

        log.debug("[Registry] Resolved {} tool instance(s) for categories: {}",
            tools.length, categories);
        return tools;
    }

    // ==================== UTILITAIRES ====================

    /**
     * Retourne le nombre de tools enregistrés pour une catégorie.
     * Utile pour les tests et le monitoring.
     */
    public int toolCountForCategory(ToolCategory category) {
        return registry.getOrDefault(category, List.of()).size();
    }

    /**
     * Retourne une vue immuable du registry complet.
     * Exposé pour les endpoints de diagnostic (/actuator/router-stats, etc.).
     */
    public Map<ToolCategory, List<Object>> getRegistry() {
        return registry;
    }

    private void logRegistryStats() {
        int totalMappings = registry.values().stream()
            .mapToInt(List::size)
            .sum();
        log.info("[Registry] Initialized with {} categories, {} tool-category mappings",
            registry.size(), totalMappings);
        registry.forEach((cat, tools) ->
            log.debug("[Registry]   {} → {} tool(s): {}",
                cat,
                tools.size(),
                tools.stream()
                    .map(t -> t.getClass().getSimpleName())
                    .collect(Collectors.joining(", "))));
    }
}