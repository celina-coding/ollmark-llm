package com.penpot.ai.application.tools.support;

import lombok.experimental.UtilityClass;

import java.util.List;

/**
 * Utilitaire de génération des fragments JavaScript Penpot récurrents.
 *
 * <p><b>DRY :</b> centralise les patterns JS dupliqués dans tous les tools
 * (lookup de forme, collection multi-formes, création de texte...).</p>
 * <p><b>SRP :</b> responsabilité unique — générer du code JS structurel Penpot.</p>
 */
@UtilityClass
public class PenpotJsSnippets {

    // ==================== LOOKUP D'UNE SEULE FORME ====================

    /**
     * Génère le code JS pour trouver une forme par ID, avec fallback sur la sélection.
     * <p>Variable résultante dans le scope JS : {@code shape}.</p>
     *
     * @param shapeId UUID de la forme Penpot
     */
    public static String findShapeOrFallback(String shapeId) {
        return String.format("""
            let shape = null;
            try {
                shape = penpot.currentPage.getShapeById('%s');
            } catch (e) {
                if (penpot.selection.length > 0) shape = penpot.selection[0];
            }
            if (!shape) throw new Error('Shape not found. ID: %s. Please select a shape in Penpot.');
            """, shapeId, shapeId);
    }

    /**
     * Génère le code JS pour trouver la <em>première</em> forme d'une liste d'IDs,
     * avec fallback sur le premier élément sélectionné.
     * <p>Variable résultante : {@code shape}.</p>
     *
     * @param shapeIds liste d'IDs (utilise uniquement le premier)
     */
    public static String findFirstShapeOrFallback(List<String> shapeIds) {
        if (shapeIds == null || shapeIds.isEmpty()) {
            return """
                const selShapes = penpot.selection;
                const shape = (selShapes && selShapes.length) ? selShapes[0] : null;
                if (!shape) throw new Error('No shape to operate on. Select a shape in Penpot.');
                """;
        }
        String firstId = shapeIds.get(0);
        return String.format("""
            let shape = null;
            try { shape = penpot.currentPage.getShapeById('%s'); } catch (e) {}
            if (!shape) {
                const sel = penpot.selection;
                shape = (sel && sel.length) ? sel[0] : null;
            }
            if (!shape) throw new Error('No shape found. ID: %s. Please select a shape.');
            """, firstId, firstId);
    }

    // ==================== COLLECTION DE PLUSIEURS FORMES ====================

    /**
     * Génère le code JS pour collecter plusieurs formes avec fallback sur la sélection.
     * <p>Variable résultante : {@code shapes} (tableau).</p>
     *
     * <p>Si {@code ids} est null ou vide, utilise directement {@code penpot.selection}.</p>
     *
     * @param ids      liste des UUIDs (null ou vide = utiliser la sélection courante)
     * @param toolName nom du tool pour les messages de console
     */
    public static String collectShapesOrFallback(List<String> ids, String toolName) {
        if (ids == null || ids.isEmpty()) {
            return String.format("""
                const shapes = [...penpot.selection];
                console.log('[%s] Using current selection:', shapes.length, 'shapes');
                """, toolName);
        }

        StringBuilder sb = new StringBuilder("const shapes = [];\n");
        for (int i = 0; i < ids.size(); i++) {
            String id = ids.get(i);
            sb.append(String.format("""
                try {
                    const _s%d = penpot.currentPage.getShapeById('%s');
                    if (_s%d) shapes.push(_s%d);
                } catch (e) {
                    console.log('[%s] Invalid ID or not found: %s');
                }
                """, i, id, i, i, toolName, id));
        }
        sb.append(String.format("""
            if (shapes.length === 0) {
                console.log('[%s] No valid IDs, falling back to current selection');
                shapes.push(...penpot.selection);
            }
            """, toolName));
        return sb.toString();
    }

    // ==================== CRÉATION DE TEXTE ====================

    /**
     * Génère le code JS pour créer un élément texte Penpot.
     *
     * <p>Extrait ici car dupliqué dans {@code PenpotShapeTools} et {@code PenpotContentTools}.
     * Variable résultante : {@code text}.</p>
     *
     * @param content    contenu textuel
     * @param x          position X
     * @param y          position Y
     * @param fontSize   taille de police (null = ignoré)
     * @param fontWeight graisse (null = ignoré)
     * @param fillColor  couleur hex (null = ignoré)
     * @param name       nom de l'élément (null = ignoré)
     */
    public static String createText(
        String content, int x, int y,
        Integer fontSize, String fontWeight, String fillColor, String name
    ) {
        String escaped = escapeJsString(content);
        StringBuilder code = new StringBuilder();
        code.append(String.format("const text = penpot.createText('%s');\n", escaped));
        code.append(String.format("text.x = %d;\n", x));
        code.append(String.format("text.y = %d;\n", y));
        if (fontSize != null && fontSize > 0)
            code.append(String.format("text.fontSize = %d;\n", fontSize));
        if (fontWeight != null && !fontWeight.isBlank())
            code.append(String.format("text.fontWeight = '%s';\n", fontWeight));
        if (fillColor != null && !fillColor.isBlank())
            code.append(String.format("text.fills = [{ fillColor: '%s' }];\n", fillColor));
        if (name != null && !name.isBlank())
            code.append(String.format("text.name = '%s';\n", escapeJsString(name)));
        code.append("return text.id;\n");
        return code.toString();
    }

    // ==================== UTILITAIRES ====================

    /**
     * Échappe une chaîne pour inclusion dans du code JavaScript entre single quotes.
     */
    public static String escapeJsString(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\")
                .replace("'", "\\'")
                .replace("\n", "\\n")
                .replace("\r", "\\r");
    }
}