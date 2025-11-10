package com.example.aiPoc.services.penpot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.example.aiPoc.config.PenpotSdkConfig;
import com.example.aiPoc.exceptions.PenpotSdkException;
import com.example.aiPoc.models.PenpotApiMethod;

import jakarta.annotation.PostConstruct;

/**
 * Service responsable de la gestion et de la documentation du SDK Penpot.
 *
 * <p>
 * Ce service a plusieurs responsabilités :
 * <ul>
 *   <li>Charger la documentation de l'API Penpot</li>
 *   <li>Fournir des résumés textuels des méthodes disponibles</li>
 *   <li>Gérer des exemples de code utilisables pour les prompts IA</li>
 * </ul>
 * </p>
 *
 * <p>
 * Les données sont actuellement codées en dur mais pourront être chargées
 * dynamiquement depuis un fichier JSON à l’avenir.
 * </p>
 *
 * @see PenpotApiMethod
 */
@Service
public class PenpotSdkService {

    /** Logger utilisé pour le suivi et le débogage du service. */
    private static final Logger logger = LoggerFactory.getLogger(PenpotSdkService.class);

    /** Configuration du SDK Penpot. */
    private final PenpotSdkConfig config;

    /** Map des méthodes API disponibles, indexées par leur nom. */
    private Map<String, PenpotApiMethod> apiMethods = new HashMap<>();

    /** Liste d’exemples de code illustrant l’usage du SDK. */
    private List<String> codeExamples = new ArrayList<>();

    /**
     * Constructeur du service {@code PenpotSdkService}.
     *
     * @param config la configuration du SDK Penpot injectée par Spring.
     */
    public PenpotSdkService(PenpotSdkConfig config) {
        this.config = config;
    }

    /**
     * Méthode d’initialisation appelée automatiquement après l’injection des dépendances.
     * <p>
     * Elle charge la documentation de l’API et les exemples de code.
     * </p>
     */
    @PostConstruct
    public void initialize() {
        logger.info("Initialisation du service SDK Penpot");
        loadApiDocumentation();
        loadCodeExamples();
    }

    /**
     * Charge la documentation de l’API Penpot.
     * <p>
     * Les définitions des méthodes sont pour l’instant écrites en dur. Une évolution future
     * permettra de charger ces données depuis un fichier JSON.
     * </p>
     */
    private void loadApiDocumentation() {
        logger.debug("Chargement de la documentation API Penpot");

        // Méthode : createRectangle
        PenpotApiMethod createRect = new PenpotApiMethod(
            "createRectangle",
            "Crée un rectangle sur le canevas",
            "Shape"
        );
        createRect.setCategory("SHAPE_CREATION");
        createRect.setExample("const rect = penpot.createRectangle(100, 100, 200, 150);");
        createRect.getParameters().add(new PenpotApiMethod.Parameter("x", "number", true));
        createRect.getParameters().add(new PenpotApiMethod.Parameter("y", "number", true));
        createRect.getParameters().add(new PenpotApiMethod.Parameter("width", "number", true));
        createRect.getParameters().add(new PenpotApiMethod.Parameter("height", "number", true));
        apiMethods.put("createRectangle", createRect);

        // Méthode : createText
        PenpotApiMethod createText = new PenpotApiMethod(
            "createText",
            "Crée un élément texte sur le canevas",
            "TextNode"
        );
        createText.setCategory("TEXT");
        createText.setExample("const text = penpot.createText(50, 50, 'Hello World');");
        createText.getParameters().add(new PenpotApiMethod.Parameter("x", "number", true));
        createText.getParameters().add(new PenpotApiMethod.Parameter("y", "number", true));
        createText.getParameters().add(new PenpotApiMethod.Parameter("content", "string", true));
        apiMethods.put("createText", createText);

        // Méthode : createCircle
        PenpotApiMethod createCircle = new PenpotApiMethod(
            "createCircle",
            "Crée un cercle sur le canevas",
            "Shape"
        );
        createCircle.setCategory("SHAPE_CREATION");
        createCircle.setExample("const circle = penpot.createCircle(150, 150, 75);");
        createCircle.getParameters().add(new PenpotApiMethod.Parameter("x", "number", true));
        createCircle.getParameters().add(new PenpotApiMethod.Parameter("y", "number", true));
        createCircle.getParameters().add(new PenpotApiMethod.Parameter("radius", "number", true));
        apiMethods.put("createCircle", createCircle);

        logger.info("Documentation API chargée: {} méthodes", apiMethods.size());
    }

    /**
     * Charge les exemples de code d’utilisation du SDK.
     */
    private void loadCodeExamples() {
        logger.debug("Chargement des exemples de code");

        codeExamples.add(
            "// Créer un rectangle rouge\n" +
            "const rect = penpot.createRectangle(100, 100, 200, 150);\n" +
            "rect.fills = [{color: '#FF0000'}];"
        );

        codeExamples.add(
            "// Créer du texte stylisé\n" +
            "const text = penpot.createText(50, 50, 'Hello World');\n" +
            "text.fontSize = 24;\n" +
            "text.fontWeight = 'bold';"
        );

        codeExamples.add(
            "// Créer plusieurs formes alignées\n" +
            "for (let i = 0; i < 3; i++) {\n" +
            "  const rect = penpot.createRectangle(100 + i * 250, 100, 200, 150);\n" +
            "  rect.fills = [{color: `hsl(${i * 120}, 70%, 50%)`}];\n" +
            "}"
        );

        logger.info("Exemples de code chargés: {}", codeExamples.size());
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

        apiMethods.values().forEach(method -> {
            summary.append(String.format("- %s(%s): %s\n",
                method.getName(),
                formatParameters(method.getParameters()),
                method.getReturnType()
            ));
            summary.append(String.format("  Description: %s\n", method.getDescription()));
            if (method.getExample() != null) {
                summary.append(String.format("  Exemple: %s\n", method.getExample()));
            }
            summary.append("\n");
        });

        return summary.toString();
    }

    /**
     * Génère un résumé compact listant uniquement les signatures des méthodes de l’API.
     *
     * @return une chaîne compacte contenant la liste des méthodes du SDK.
     */
    public String getCompactApiSummary() {
        StringBuilder summary = new StringBuilder();

        apiMethods.values().forEach(method -> {
            summary.append(String.format("penpot.%s(%s)\n",
                method.getName(),
                formatParameters(method.getParameters())
            ));
        });

        return summary.toString();
    }

    /**
     * Retourne la liste complète des exemples de code disponibles.
     *
     * @return une liste de chaînes de caractères contenant des extraits de code.
     */
    public List<String> getCodeExamples() {
        return new ArrayList<>(codeExamples);
    }

    /**
     * Sélectionne et retourne un exemple de code aléatoire parmi ceux chargés.
     *
     * @return une chaîne contenant un extrait de code.
     * @throws PenpotSdkException si aucun exemple de code n’est disponible.
     */
    public String getRandomExample() {
        if (codeExamples.isEmpty()) throw new PenpotSdkException("Aucun exemple de code disponible");
        int index = (int) (Math.random() * codeExamples.size());
        return codeExamples.get(index);
    }

    /**
     * Vérifie si une méthode donnée existe dans la documentation de l’API.
     *
     * @param methodName le nom de la méthode à vérifier.
     * @return {@code true} si la méthode existe, {@code false} sinon.
     */
    public boolean methodExists(String methodName) {
        return apiMethods.containsKey(methodName);
    }

    /**
     * Récupère les détails d’une méthode spécifique de l’API.
     *
     * @param methodName le nom de la méthode à rechercher.
     * @return un objet {@link PenpotApiMethod} représentant la méthode, ou {@code null} si introuvable.
     */
    public PenpotApiMethod getMethod(String methodName) {
        return apiMethods.get(methodName);
    }

    /**
     * Retourne la liste des méthodes appartenant à une catégorie donnée.
     *
     * @param category le nom de la catégorie (ex. "SHAPE_CREATION", "TEXT").
     * @return une liste de méthodes appartenant à cette catégorie (peut être vide).
     */
    public List<PenpotApiMethod> getMethodsByCategory(String category) {
        return apiMethods.values().stream()
            .filter(m -> category.equals(m.getCategory()))
            .toList();
    }

    /**
     * Formate la liste des paramètres d’une méthode pour un affichage lisible.
     *
     * @param parameters la liste des paramètres de la méthode.
     * @return une chaîne formatée de la forme {@code "x: number, y: number"}.
     */
    private String formatParameters(List<PenpotApiMethod.Parameter> parameters) {
        return parameters.stream()
            .map(p -> p.getName() + ": " + p.getType())
            .reduce((a, b) -> a + ", " + b)
            .orElse("");
    }

    /**
     * Retourne le nombre total de méthodes API chargées.
     *
     * @return le nombre de méthodes actuellement disponibles dans la documentation.
     */
    public int getMethodCount() {
        return apiMethods.size();
    }
}