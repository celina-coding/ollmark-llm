package com.example.aiPoc.services.penpot;

import com.example.aiPoc.dto.*;
import org.slf4j.*;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Service responsable de l'agrégation intelligente de la documentation Penpot
 * afin d’optimiser les prompts envoyés aux modèles d’intelligence artificielle.
 *
 * <p>Ce service analyse le texte fourni par l'utilisateur pour identifier
 * les concepts ou catégories pertinentes et ne conserver que les portions
 * utiles de la documentation. L'objectif est de réduire la consommation
 * de tokens tout en maximisant la pertinence contextuelle.</p>
 *
 * <p><b>Responsabilités principales :</b></p>
 * <ul>
 *   <li>Analyser le prompt utilisateur pour détecter les thèmes associés aux API Penpot.</li>
 *   <li>Filtrer les méthodes de l’API pertinentes selon les catégories détectées.</li>
 *   <li>Sélectionner et condenser les exemples de code appropriés.</li>
 *   <li>Fournir des résumés optimisés, compacts ou riches selon le mode d’usage.</li>
 * </ul>
 */
@Service
public class DocumentationAggregatorService {

    /** Logger utilisé pour le suivi et le débogage du service. */
    private static final Logger logger = LoggerFactory.getLogger(DocumentationAggregatorService.class);

    /** Service d'accès au SDK Penpot, fournissant la documentation et les exemples disponibles. */
    private final PenpotSdkService penpotSdkService;

    /**
     * Constructeur injectant le service SDK Penpot.
     *
     * @param penpotSdkService le service permettant d’accéder à la documentation et aux exemples du SDK Penpot
     */
    public DocumentationAggregatorService(PenpotSdkService penpotSdkService) {
        this.penpotSdkService = penpotSdkService;
    }

    /**
     * Génère un résumé de la documentation Penpot en fonction du prompt utilisateur.
     *
     * @param userPrompt le texte fourni par l'utilisateur
     * @return un résumé synthétique et pertinent de la documentation Penpot
     */
    public String generateSummary(String userPrompt) {
        logger.debug("Génération du résumé pour: {}", userPrompt);
        StringBuilder summary = new StringBuilder();

        // 1. Détection des catégories pertinentes
        List<String> relevantCategories = detectRelevantCategories(userPrompt);
        if (relevantCategories.isEmpty()) {
            logger.debug("Aucune catégorie spécifique détectée, résumé général");
            return penpotSdkService.getCompactApiSummary();
        }

        logger.debug("Catégories détectées: {}", relevantCategories);

        // 2. Filtrage des méthodes selon les catégories pertinentes
        summary.append("SDK Penpot - Méthodes pertinentes:\n\n");

        for (String category : relevantCategories) {
            List<PenpotApiDocumentation.ApiMethod> methods = 
                penpotSdkService.getMethodsByCategory(category);

            logger.debug("Méhtodes détectées: {}", methods);

            if (!methods.isEmpty()) {
                summary.append(String.format("=== %s ===\n", category));
            }

            for (PenpotApiDocumentation.ApiMethod method : methods) {
                summary.append(formatMethodSummary(method));
            }
        }

        // 3. Ajout des propriétés communes si pertinentes
        if (shouldIncludeCommonProperties(userPrompt)) {
            summary.append(getCommonPropertiesSummary());
        }

        // 4. Ajout des utilitaires si pertinents
        if (shouldIncludeUtilities(userPrompt)) {
            summary.append(getUtilitiesSummary());
        }

        // 5. Ajout des exemples de code pertinents
        String examplesSummary = getRelevantExamples(userPrompt, relevantCategories);
        if (!examplesSummary.isEmpty()) {
            summary.append("\nExemples de code:\n").append(examplesSummary);
        }

        return summary.toString();
    }

    /**
     * Détecte les catégories pertinentes à partir du prompt utilisateur
     * en utilisant les mots-clés définis dans le JSON.
     *
     * @param userPrompt le texte saisi par l'utilisateur
     * @return une liste des catégories correspondantes
     */
    private List<String> detectRelevantCategories(String userPrompt) {
        String promptLower = userPrompt.toLowerCase();
        List<String> categories = new ArrayList<>();

        if (penpotSdkService.getApiSummary() == null) {
            return categories;
        }

        // SHAPE_CREATION - Mots-clés associés aux formes géométriques.
        if (containsAny(promptLower, new String[]{
            "rectangle", "ellipse", "cercle", "badge", "path", "bouton", "board",
            "carré", "forme", "crée", "créer", "rond", "ovale", "tracé", "cercle"
        })) {
            categories.add("SHAPE_CREATION");
        }

        // STYLING - Mots-clés associés aux couleurs et styles
        if (containsAny(promptLower, new String[]{
            "couleur", "color", "rouge", "red", "bleu", "blue", "vert", "green",
            "jaune", "yellow", "noir", "black", "blanc", "white", "orange",
            "fill", "remplissage", "stroke", "contour", "bordure", "border",
            "opacity", "opacité", "transparence", "transparent", "rotation",
            "pivoter", "rotate", "flip", "miroir", "arrondi", "radius"
        })) {
            categories.add("STYLING");
        }

        // TEXT - Mots-clés associés au texte et à la typographie.
        if (containsAny(promptLower, new String[]{
            "text", "texte", "label", "titre", "title", "paragraphe", "paragraph",
            "font", "police", "écriture", "caractère", "typographie"
        })) {
            categories.add("TEXT");
        }

        // ORGANIZATION - Mots-clés associés à l'organisation d'éléments.
        if (containsAny(promptLower, new String[]{
            "groupe", "group", "aligner", "align", "distribuer", "distribute",
            "centre", "center", "gauche", "left", "droite", "right",
            "haut", "top", "bas", "bottom", "coin", "corner",
            "position", "placer", "place", "déplacer", "move"
        })) {
            categories.add("ORGANIZATION");
        }

        // MEDIA
        if (containsAny(promptLower, new String[]{
            "image", "media", "upload", "télécharger", "photo", "picture"
        })) {
            categories.add("MEDIA");
        }

        // INTERACTION
        if (containsAny(promptLower, new String[]{
            "interaction", "clic", "click", "hover", "souris", "mouse",
            "prototype", "navigation", "lien", "link", "bouton", "button"
        })) {
            categories.add("INTERACTION");
        }

        // EXPORT
        if (containsAny(promptLower, new String[]{
            "export", "exporter", "télécharger", "download", "sauvegarder", "save"
        })) {
            categories.add("EXPORT");
        }

        // Si toujours aucune catégorie, utiliser la logique du JSON
        if (categories.isEmpty()) {
            for (String categoryName : List.of("SHAPE_CREATION", "TEXT", "ORGANIZATION", 
                                               "STYLING", "MEDIA", "INTERACTION", "EXPORT")) {
                List<String> keywords = penpotSdkService.getCategoryKeywords(categoryName);
                                            
                if (keywords != null && containsAny(promptLower, keywords.toArray(new String[0]))) {
                    categories.add(categoryName);
                }
            }
        }

        logger.debug("Catégories détectées: {}", categories);
        return categories;
    }

    /**
     * Vérifie si une chaîne contient au moins un mot-clé d’une liste.
     *
     * @param text le texte à analyser
     * @param keywords tableau de mots-clés à rechercher
     * @return {@code true} si au moins un mot-clé est trouvé, sinon {@code false}
     */
    private boolean containsAny(String text, String[] keywords) {
        for (String keyword : keywords) {
            if (text.contains(keyword)) return true;
        }
        return false;
    }

    /**
     * Détermine si les propriétés communes doivent être incluses dans le résumé.
     *
     * @param userPrompt le texte utilisateur
     * @return {@code true} si les propriétés communes sont pertinentes
     */
    private boolean shouldIncludeCommonProperties(String userPrompt) {
        String promptLower = userPrompt.toLowerCase();
        List<String> styleKeywords = penpotSdkService.getCategoryKeywords("STYLING");

        return (styleKeywords != null && containsAny(promptLower, styleKeywords.toArray(new String[0]))) || 
               promptLower.contains("propriété") || 
               promptLower.contains("property") ||
               promptLower.contains("attribut");
    }

    /**
     * Détermine si les utilitaires doivent être inclus dans le résumé.
     *
     * @param userPrompt le texte utilisateur
     * @return {@code true} si les utilitaires sont pertinents
     */
    private boolean shouldIncludeUtilities(String userPrompt) {
        String promptLower = userPrompt.toLowerCase();
        return promptLower.contains("sélection") || 
               promptLower.contains("selection") ||
               promptLower.contains("viewport") ||
               promptLower.contains("page") ||
               promptLower.contains("theme") ||
               promptLower.contains("bibliothèque") ||
               promptLower.contains("library");
    }

    /**
     * Génère un résumé des propriétés communes des formes.
     *
     * @return une chaîne décrivant les propriétés communes disponibles
     */
    private String getCommonPropertiesSummary() {
        StringBuilder sb = new StringBuilder();
        sb.append("\n=== Propriétés communes ===\n");
        sb.append("Shape: id, type, name, x, y, width, height, rotation, opacity\n");
        sb.append("       fills, strokes, bounds, center, parent\n");
        sb.append("       flipX, flipY, blocked, hidden, blendMode\n");
        sb.append("Fill: fillColor, fillOpacity\n");
        sb.append("Stroke: strokeColor, strokeWidth, strokeStyle, strokeAlignment\n");
        sb.append("Bounds: {x, y, width, height}\n");
        sb.append("Point: {x, y}\n\n");
        return sb.toString();
    }

    /**
     * Génère un résumé des utilitaires disponibles.
     *
     * @return une chaîne décrivant les utilitaires Penpot
     */
    private String getUtilitiesSummary() {
        StringBuilder sb = new StringBuilder();
        sb.append("\n=== Utilitaires ===\n");
        sb.append("penpot.selection - Formes sélectionnées (Array<Shape>)\n");
        sb.append("penpot.currentPage - Page active\n");
        sb.append("penpot.viewport.center - Centre du viewport {x, y}\n");
        sb.append("penpot.theme - Thème actuel (dark|light)\n");
        sb.append("penpot.utils.types.isGroup(shape) - Vérifie si c'est un groupe\n");
        sb.append("penpot.library.local - Bibliothèque locale\n\n");
        return sb.toString();
    }

    /**
     * Formate une méthode de l’API Penpot de manière compacte et lisible.
     *
     * @param method l’objet représentant une méthode du SDK Penpot
     * @return une chaîne de caractères contenant la signature, la description et un exemple éventuel
     */
    private String formatMethodSummary(PenpotApiDocumentation.ApiMethod method) {
        StringBuilder sb = new StringBuilder();

        sb.append(String.format("%s → %s\n",
            method.formatSignature(),
            method.getReturnType()
        ));

        sb.append(String.format("  %s\n", method.getDescription()));

        // Affiche les méthodes chainables
        if (method.getChainableMethods() != null && !method.getChainableMethods().isEmpty()) {
            sb.append("  Méthodes chainables:\n");
            method.getChainableMethods().forEach((key, value) -> 
                sb.append(String.format("    - %s: %s\n", value.getSignature(), value.getDescription()))
            );
        }

        // Affiche les exemples d'utilisation
        if (method.getCommonUsage() != null && !method.getCommonUsage().isEmpty()) {
            String compactExample = method.getCommonUsage().stream()
                .limit(2)
                .collect(Collectors.joining("; "));
            sb.append(String.format("  Ex: %s\n", compactExample));
        }

        if (method.getNotes() != null && !method.getNotes().isEmpty()) {
            sb.append(String.format("  Note: %s\n", method.getNotes()));
        }

        sb.append("\n");
        return sb.toString();
    }

    /**
     * Récupère les exemples de code pertinents pour les catégories détectées.
     *
     * @param userPrompt le texte utilisateur
     * @param categories les catégories pertinentes
     * @return une chaîne contenant un ou plusieurs extraits de code
     */
    private String getRelevantExamples(String userPrompt, List<String> categories) {
        StringBuilder examples = new StringBuilder();

        List<CodeExamplesCollection.CodeExample> allExamples = 
            penpotSdkService.getCodeExamplesCollection();

        if (allExamples.isEmpty()) return "";

        List<CodeExamplesCollection.CodeExample> relevantExamples = allExamples.stream()
            .filter(ex -> categories.contains(ex.getCategory()))
            .toList();

        for (CodeExamplesCollection.CodeExample example : relevantExamples) {
            examples.append(String.format("\n// %s\n%s\n", 
                example.getTitle(), 
                example.getCode()
            ));
        }

        return examples.toString();
    }

    /**
     * Génère un résumé minimal des méthodes pertinentes (sans exemples).
     *
     * @param userPrompt le texte fourni par l'utilisateur
     * @return un résumé très concis listant uniquement les signatures de méthodes pertinentes
     */
    public String generateMinimalSummary(String userPrompt) {
        List<String> categories = detectRelevantCategories(userPrompt);
        if (categories.isEmpty()) return penpotSdkService.getCompactApiSummary();

        StringBuilder summary = new StringBuilder();

        for (String category : categories) {
            List<PenpotApiDocumentation.ApiMethod> methods = 
                penpotSdkService.getMethodsByCategory(category);

            for (PenpotApiDocumentation.ApiMethod method : methods) {
                summary.append(method.formatSignature()).append("\n");
            }
        }

        return summary.toString();
    }

    /**
     * Génère un résumé riche en exemples pour un apprentissage par "few-shot".
     *
     * @param userPrompt le texte de l'utilisateur
     * @return un résumé incluant les signatures de méthodes et plusieurs exemples de code
     */
    public String generateExampleRichSummary(String userPrompt) {
        StringBuilder summary = new StringBuilder();

        summary.append(generateMinimalSummary(userPrompt));
        summary.append("\n\nExemples de code:\n");

        List<String> categories = detectRelevantCategories(userPrompt);
        List<CodeExamplesCollection.CodeExample> examples = 
            penpotSdkService.getCodeExamplesCollection();

        if (!categories.isEmpty()) {
            examples = examples.stream()
                .filter(ex -> categories.contains(ex.getCategory()))
                .limit(3)
                .toList();
        } else {
            examples = examples.stream().limit(3).toList();
        }

        for (CodeExamplesCollection.CodeExample example : examples) {
            summary.append(String.format("\n// %s - %s\n%s\n",
                example.getTitle(),
                example.getDescription(),
                example.getCode()
            ));
        }

        return summary.toString();
    }
}