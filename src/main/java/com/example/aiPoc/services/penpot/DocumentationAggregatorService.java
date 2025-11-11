package com.example.aiPoc.services.penpot;

import com.example.aiPoc.dto.CodeExamplesCollection;
import com.example.aiPoc.dto.PenpotApiDocumentation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.ArrayList;
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

    private static final Logger logger = LoggerFactory.getLogger(DocumentationAggregatorService.class);

    private final PenpotSdkService penpotSdkService;

    /** Liste des mots-clés associés aux formes géométriques. */
    private static final String[] SHAPE_KEYWORDS = {
        "rectangle", "carré", "cercle", "ellipse", "oval",
        "form", "chemin", "tracé", "vecteur", "triang",
        "créer", "dessin", "trace", "boolean", "booléen",
        "board", "page", "scène", "tableau", "affiche", "croix"
    };

    /** Liste des mots-clés associés au texte et à la typographie. */
    private static final String[] TEXT_KEYWORDS = {
        "text", "titre", "paragraphe", "mot", "phrase", 
        "écrire", "typo", "police", "caractère", "label",
        "étiquette"
    };

    /** Liste des mots-clés associés à l'organisation d'éléments. */
    private static final String[] ORGANIZATION_KEYWORDS = {
        "group", "dégrouper", "grouper", "milieu",
        "organis", "align", "distribu", "répartir", "espace",
        "horizontal", "vertical", "centre", "gauche", "coin",
        "droite", "composition", "haut", "bas", "plan",
    };

    /** Liste des mots-clés associés aux propriétés visuelles des formes. */
    private static final String[] STYLE_KEYWORDS = {
        "couleur", "color", "remplissage", "tour",
        "opacit", "rotation", "tourner", "pivoter", "petit",
        "taille", "dimension", "largeur", "hauteur", "grand",
        "redimension", "styl", "apparence", "px", "pixel"
    };

    /**
     * Constructeur injectant le service SDK Penpot.
     *
     * @param penpotSdkService le service permettant d’accéder à la documentation et aux exemples du SDK Penpot
     */
    public DocumentationAggregatorService(PenpotSdkService penpotSdkService) {
        this.penpotSdkService = penpotSdkService;
    }

    /**
     * Génère un résumé optimisé de la documentation Penpot en fonction du prompt utilisateur.
     *
     * @param userPrompt le texte fourni par l'utilisateur
     * @return un résumé synthétique et pertinent de la documentation Penpot
     */
    public String generateOptimizedSummary(String userPrompt) {
        logger.debug("Génération du résumé optimisé pour: {}", userPrompt);

        StringBuilder summary = new StringBuilder();

        // 1. Détection des catégories pertinentes
        List<String> relevantCategories = detectRelevantCategories(userPrompt);

        if (relevantCategories.isEmpty()) {
            logger.debug("Aucune catégorie spécifique détectée, résumé général");
            return penpotSdkService.getCompactApiSummary();
        }

        logger.debug("Catégories détectées: {}", relevantCategories);

        // 2. Filtrer les méthodes selon les catégories pertinentes
        summary.append("SDK Penpot - Méthodes pertinentes:\n\n");

        for (String category : relevantCategories) {
            List<PenpotApiDocumentation.ApiMethod> methods = 
                penpotSdkService.getMethodsByCategory(category);

            if (!methods.isEmpty()) {
                summary.append(String.format("=== %s ===\n", category));
            }

            for (PenpotApiDocumentation.ApiMethod method : methods) {
                summary.append(formatMethodSummary(method));
            }
        }

        // 3. Ajouter les propriétés communes si pertinentes
        if (shouldIncludeCommonProperties(userPrompt)) {
            summary.append(getCommonPropertiesSummary());
        }

        // 4. Ajouter les utilitaires si pertinents
        if (shouldIncludeUtilities(userPrompt)) {
            summary.append(getUtilitiesSummary());
        }

        // 5. Ajouter des exemples de code pertinents
        String examplesSummary = getRelevantExamples(userPrompt, relevantCategories);
        if (!examplesSummary.isEmpty()) {
            summary.append("\nExemples de code:\n").append(examplesSummary);
        }

        return summary.toString();
    }

    /**
     * Détecte les catégories pertinentes à partir du prompt utilisateur.
     *
     * @param userPrompt le texte saisi par l’utilisateur
     * @return une liste des catégories correspondantes (ex. SHAPE_CREATION, ORGANIZATION, etc.)
     */
    private List<String> detectRelevantCategories(String userPrompt) {
        String promptLower = userPrompt.toLowerCase();
        List<String> categories = new ArrayList<>();

        if (containsAny(promptLower, SHAPE_KEYWORDS)) {
            categories.add("SHAPE_CREATION");
        }
        if (containsAny(promptLower, TEXT_KEYWORDS)) {
            categories.add("TEXT");
        }
        if (containsAny(promptLower, ORGANIZATION_KEYWORDS)) {
            categories.add("ORGANIZATION");
        }

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
        return containsAny(promptLower, STYLE_KEYWORDS) || 
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
        
        sb.append(String.format("penpot.%s(%s) → %s\n",
            method.getName(),
            formatParameters(method.getParameters()),
            method.getReturnType()
        ));

        sb.append(String.format("  %s\n", method.getDescription()));

        if (method.getMethods() != null && !method.getMethods().isEmpty()) {
            sb.append("  Méthodes disponibles:\n");
            method.getMethods().forEach((key, value) -> 
                sb.append(String.format("    - %s: %s\n", key, value))
            );
        }

        if (method.getExample() != null && !method.getExample().isEmpty()) {
            String compactExample = compactExample(method.getExample());
            sb.append(String.format("  Ex: %s\n", compactExample));
        }

        if (method.getNotes() != null && !method.getNotes().isEmpty()) {
            sb.append(String.format("  %s\n", method.getNotes()));
        }

        sb.append("\n");
        return sb.toString();
    }

    /**
     * Formate les paramètres d’une méthode sous forme concise.
     *
     * @param parameters liste des paramètres de la méthode
     * @return une chaîne formatée listant les noms et types de paramètres
     */
    private String formatParameters(List<PenpotApiDocumentation.Parameter> parameters) {
        if (parameters == null || parameters.isEmpty()) return "";

        return parameters.stream()
            .map(p -> {
                String paramStr = p.getName() + ": " + p.getType();
                if (!p.isRequired()) paramStr += "?";
                if (p.getDefaultValue() != null) paramStr += " = " + p.getDefaultValue();
                return paramStr;
            })
            .collect(Collectors.joining(", "));
    }

    /**
     * Compacte un exemple de code en ne gardant que les premières lignes significatives.
     *
     * @param example le code source de l’exemple complet
     * @return une version abrégée de l’exemple
     */
    private String compactExample(String example) {
        if (example == null) return "";

        String[] lines = example.split("\n");
        if (lines.length == 1) return example;

        return java.util.Arrays.stream(lines)
            .limit(2)
            .filter(line -> !line.trim().isEmpty() && !line.trim().startsWith("//"))
            .collect(Collectors.joining("; "));
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

        if (relevantExamples.isEmpty()) {
            relevantExamples = allExamples.stream()
                .filter(ex -> "EASY".equalsIgnoreCase(ex.getDifficulty()) || 
                             "Débutant".equalsIgnoreCase(ex.getDifficulty()))
                .limit(1)
                .toList();
        }

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
     * @param userPrompt le texte fourni par l’utilisateur
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
                summary.append(String.format("penpot.%s(%s)\n",
                    method.getName(),
                    formatParameters(method.getParameters())
                ));
            }
        }

        return summary.toString();
    }

    /**
     * Génère un résumé riche en exemples pour un apprentissage par "few-shot".
     *
     * @param userPrompt le texte de l’utilisateur
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

    /**
     * Génère un résumé spécifique pour la création de formes.
     *
     * @return un résumé détaillé des méthodes de création de formes
     */
    public String generateShapeCreationSummary() {
        return generateOptimizedSummary("créer des formes rectangle ellipse path board");
    }

    /**
     * Génère un résumé spécifique pour l'organisation et l'alignement.
     *
     * @return un résumé détaillé des méthodes d'organisation
     */
    public String generateOrganizationSummary() {
        return generateOptimizedSummary("grouper aligner distribuer organisation");
    }

    /**
     * Estime le nombre de tokens nécessaires pour le résumé généré.
     * 
     * <p>Cette estimation est grossière, en considérant une moyenne
     * de quatre caractères par token, approximation basée sur les modèles GPT.</p>
     *
     * @param summary le texte à évaluer
     * @return le nombre approximatif de tokens utilisés
     */
    public int estimateTokenCount(String summary) {
        return summary.length() / 4;
    }

    /**
     * Retourne les catégories disponibles dans la documentation.
     *
     * @return une liste des catégories disponibles
     */
    public List<String> getAvailableCategories() {
        return List.of("SHAPE_CREATION", "ORGANIZATION", "TEXT");
    }
}