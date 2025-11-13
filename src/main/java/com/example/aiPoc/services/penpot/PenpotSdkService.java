package com.example.aiPoc.services.penpot;

import java.util.*;
import java.util.stream.Collectors;

import org.slf4j.*;
import org.springframework.stereotype.Service;

import com.example.aiPoc.config.PenpotSdkConfig;
import com.example.aiPoc.dto.*;
import com.example.aiPoc.services.JsonLoaderService;

import jakarta.annotation.PostConstruct;

/**
 * Service responsable de la gestion et de la documentation du SDK Penpot.
 *
 * <p>Ce service permet :</p>
 * <ul>
 *   <li>De charger la documentation de l’API Penpot depuis des fichiers JSON</li>
 *   <li>De fournir des résumés textuels des méthodes disponibles</li>
 *   <li>De gérer des exemples de code utilisables pour les prompts d’IA ou la documentation</li>
 * </ul>
 *
 * @see PenpotSdkConfig
 * @see PenpotApiDocumentation
 * @see CodeExamplesCollection
 * @see JsonLoaderService
 */
@Service
public class PenpotSdkService {

    /** Logger utilisé pour le suivi et le débogage du service. */
    private static final Logger logger = LoggerFactory.getLogger(PenpotSdkService.class);

    /** Configuration du SDK Penpot. */
    private final PenpotSdkConfig config;

    /** Service utilitaire de chargement des fichiers JSON. */
    private final JsonLoaderService jsonLoader;

    /** Documentation de l’API Penpot chargée depuis JSON. */
    private PenpotApiDocumentation apiDocumentation;

    /** Collection d’exemples de code chargée depuis JSON. */
    private CodeExamplesCollection codeExamplesCollection;

    /** Index des méthodes pour un accès rapide par leur nom. */
    private Map<String, PenpotApiDocumentation.ApiMethod> apiMethodsIndex = new HashMap<>();

    /**
     * Constructeur du service {@code PenpotSdkService}.
     *
     * @param config la configuration du SDK Penpot
     * @param jsonLoader le service de chargement des fichiers JSON
     */
    public PenpotSdkService(PenpotSdkConfig config, JsonLoaderService jsonLoader) {
        this.config = config;
        this.jsonLoader = jsonLoader;
    }

    /**
     * Méthode d’initialisation appelée automatiquement après l’injection des dépendances.
     * <p>
     * Elle charge la documentation de l’API et les exemples de code.
     * </p>
     */
    @PostConstruct
    public void initialize() {
        logger.info("Initialisation du service SDK Penpot depuis fichiers JSON");
        loadApiDocumentation();
        loadCodeExamples();
        buildIndex();
    }

    /**
     * Charge la documentation de l’API Penpot.
     * <p>
     * Les définitions des méthodes sont pour l’instant écrites en dur. Une évolution future
     * permettra de charger ces données depuis un fichier JSON.
     * </p>
     */
    private void loadApiDocumentation() {
        logger.debug("Chargement de la documentation API depuis JSON");

        String path = config.getApiSummaryPath();
        apiDocumentation = jsonLoader.loadJson(
            path,
            PenpotApiDocumentation.class
        );

        if (apiDocumentation == null) {
            logger.error("Impossible de charger la documentation API");
            apiDocumentation = createEmptyDocumentation();
        } else {
            logger.info("Documentation API chargée: {} méthodes (version {})", 
                       apiDocumentation.getMethods().size(),
                       apiDocumentation.getVersion());
        }
    }

    /**
     * Charge les exemples de code du SDK Penpot depuis un fichier JSON.
     * <p>Si le chargement échoue, une collection vide est créée par défaut.</p>
     */
    private void loadCodeExamples() {
        logger.debug("Chargement des exemples de code depuis JSON");

        String path = config.getCodeExamplesPath();
        codeExamplesCollection = jsonLoader.loadJson(
            path,
            CodeExamplesCollection.class
        );

        if (codeExamplesCollection == null) {
            logger.error("Impossible de charger les exemples de code");
            codeExamplesCollection = createEmptyExamples();
        } else {
            logger.info("Exemples de code chargés: {} exemples (version {})", 
                       codeExamplesCollection.getExamples().size(),
                       codeExamplesCollection.getVersion());
        }
    }

    /**
     * Construit un index interne pour un accès rapide aux méthodes par leur nom.
     */
    private void buildIndex() {
        if (apiDocumentation != null && apiDocumentation.getMethods() != null) {
            apiMethodsIndex = apiDocumentation.getMethods().stream()
                .collect(Collectors.toMap(
                    PenpotApiDocumentation.ApiMethod::getName,
                    method -> method,
                    (existing, replacement) -> existing
                ));
            logger.debug("Index construit: {} méthodes indexées", apiMethodsIndex.size());
        }
    }

    /**
     * Génère un résumé détaillé de l’API Penpot, incluant les noms, paramètres,
     * descriptions et exemples des méthodes disponibles.
     *
     * @return une chaîne de caractères décrivant toutes les méthodes du SDK.
     */
    public String getApiSummary() {
        StringBuilder summary = new StringBuilder();
        summary.append("SDK Penpot - Méthodes disponibles:\n\n");

        if (apiDocumentation == null || apiDocumentation.getMethods() == null) {
            return summary.append("Aucune documentation disponible").toString();
        }

        // Grouper par catégorie
        Map<String, List<PenpotApiDocumentation.ApiMethod>> methodsByCategory = 
            apiDocumentation.getMethods().stream()
                .collect(Collectors.groupingBy(m -> 
                    m.getCategories() != null && !m.getCategories().isEmpty() 
                        ? m.getCategories().get(0) 
                        : "AUTRES"
                ));

        for (Map.Entry<String, List<PenpotApiDocumentation.ApiMethod>> entry : methodsByCategory.entrySet()) {
            summary.append(String.format("=== %s ===\n", entry.getKey()));

            for (PenpotApiDocumentation.ApiMethod method : entry.getValue()) {
                summary.append(String.format("- %s\n", method.formatSignature()));
                summary.append(String.format("  Description: %s\n", method.getDescription()));
                summary.append(String.format("  Retour: %s\n", method.getReturnType()));

                if (method.getCommonUsage() != null && !method.getCommonUsage().isEmpty()) {
                    summary.append("  Exemple:\n");
                    method.getCommonUsage().stream().limit(2).forEach(usage -> 
                        summary.append(String.format("    %s\n", usage))
                    );
                }

                if (method.getNotes() != null) {
                    summary.append(String.format("  Note: %s\n", method.getNotes()));
                }

                summary.append("\n");
            }
        }

        // Ajout des propriétés communes
        if (apiDocumentation.getCommonProperties() != null && !apiDocumentation.getCommonProperties().isEmpty()) {
            summary.append("\n=== Propriétés communes ===\n");
            apiDocumentation.getCommonProperties().forEach((type, prop) -> {
                summary.append(String.format("\n%s: %s\n", type, prop.getDescription()));
                if (prop.getProperties() != null) {
                    prop.getProperties().forEach((key, detail) -> 
                        summary.append(String.format("  - %s (%s): %s\n", 
                            key, detail.getType(), detail.getDescription()))
                    );
                }
            });
        }

        // Ajout des utilitaires
        if (apiDocumentation.getUtilities() != null && !apiDocumentation.getUtilities().isEmpty()) {
            summary.append("\n=== Utilitaires ===\n");
            apiDocumentation.getUtilities().forEach((key, util) -> 
                summary.append(String.format("  - %s: %s\n", key, util.getDescription()))
            );
        }

        // Ajout des tips
        if (apiDocumentation.getTips() != null && !apiDocumentation.getTips().isEmpty()) {
            summary.append("\n=== Conseils ===\n");
            apiDocumentation.getTips().forEach(tip -> 
                summary.append(String.format("  - %s\n", tip))
            );
        }

        return summary.toString();
    }

    /**
     * Génère un résumé compact listant uniquement les signatures des méthodes de l’API.
     *
     * @return une chaîne compacte contenant la liste des méthodes du SDK.
     */
    public String getCompactApiSummary() {
        if (apiDocumentation == null || apiDocumentation.getMethods() == null) {
            return "";
        }

        return apiDocumentation.getMethods().stream()
            .map(PenpotApiDocumentation.ApiMethod::formatSignature)
            .collect(Collectors.joining("\n"));
    }

    /**
     * Vérifie si une méthode existe dans la documentation de l’API.
     *
     * @param methodName le nom de la méthode recherchée
     * @return {@code true} si la méthode existe, sinon {@code false}
     */
    public boolean methodExists(String methodName) {
        return apiMethodsIndex.containsKey(methodName);
    }

    /**
     * Retourne la liste des méthodes appartenant à une catégorie donnée.
     *
     * @param category le nom de la catégorie
     * @return une liste d’objets {@link PenpotApiDocumentation.ApiMethod}
     */
    public List<PenpotApiDocumentation.ApiMethod> getMethodsByCategory(String category) {
        if (apiDocumentation == null || apiDocumentation.getMethods() == null) {
            return Collections.emptyList();
        }

        if (category == null) {
            return apiDocumentation.getMethods();
        }

        return apiDocumentation.getMethods().stream()
            .filter(m -> m.getCategories() != null && m.getCategories().contains(category))
            .collect(Collectors.toList());
    }

    /**
     * Retourne le nombre total de méthodes chargées.
     *
     * @return le nombre de méthodes disponibles
     */
    public int getMethodCount() {
        return apiMethodsIndex.size();
    }

    /**
     * Retourne la collection complète d’exemples de code.
     *
     * @return une liste d’objets {@link CodeExamplesCollection.CodeExample}
     */
    public List<CodeExamplesCollection.CodeExample> getCodeExamplesCollection() {
        if (codeExamplesCollection == null || codeExamplesCollection.getExamples() == null) {
            return Collections.emptyList();
        }
        return codeExamplesCollection.getExamples();
    }

    /**
     * Retourne les mots-clés associés à une catégorie.
     *
     * @param categoryName le nom de la catégorie
     * @return la liste des mots-clés ou une liste vide
     */
    public List<String> getCategoryKeywords(String categoryName) {
        if (apiDocumentation == null || apiDocumentation.getCategories() == null) {
            return Collections.emptyList();
        }

        PenpotApiDocumentation.Category category = apiDocumentation.getCategories().get(categoryName);
        return category != null && category.getKeywords() != null 
            ? category.getKeywords() 
            : Collections.emptyList();
    }

    /**
     * Retourne tous les patterns de code courants.
     *
     * @return map des patterns
     */
    public Map<String, PenpotApiDocumentation.CommonPattern> getCommonPatterns() {
        return apiDocumentation != null && apiDocumentation.getCommonPatterns() != null
            ? apiDocumentation.getCommonPatterns()
            : Collections.emptyMap();
    }

    /**
     * Crée une documentation vide en cas d’échec de chargement.
     *
     * @return un objet {@link PenpotApiDocumentation} vide
     */
    private PenpotApiDocumentation createEmptyDocumentation() {
        PenpotApiDocumentation doc = new PenpotApiDocumentation();
        doc.setVersion("error");
        doc.setMethods(new ArrayList<>());
        doc.setCategories(new HashMap<>());
        doc.setCommonProperties(new HashMap<>());
        doc.setUtilities(new HashMap<>());
        doc.setCommonPatterns(new HashMap<>());
        doc.setTips(new ArrayList<>());
        return doc;
    }

    /**
     * Crée une collection d’exemples vide en cas d’échec de chargement.
     *
     * @return un objet {@link CodeExamplesCollection} vide
     */
    private CodeExamplesCollection createEmptyExamples() {
        CodeExamplesCollection collection = new CodeExamplesCollection();
        collection.setVersion("error");
        collection.setExamples(new ArrayList<>());
        return collection;
    }
}