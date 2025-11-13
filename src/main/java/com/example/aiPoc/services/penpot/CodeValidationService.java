package com.example.aiPoc.services.penpot;

import java.util.*;
import java.util.regex.*;

import org.slf4j.*;
import org.springframework.stereotype.Service;

import com.example.aiPoc.models.ValidationError;
import com.example.aiPoc.utils.CodeUtils;

/**
 * Service responsable de la validation des scripts JavaScript générés automatiquement
 * pour l’API Penpot.
 *
 * <p>Ce service applique plusieurs niveaux de vérification :</p>
 * <ul>
 *   <li>Validation syntaxique des structures de code</li>
 *   <li>Contrôle de l’utilisation correcte de l’API Penpot</li>
 *   <li>Détection d’erreurs et d’incohérences fréquentes</li>
 * </ul>
 *
 * @see PenpotSdkService
 * @see ValidationError
 */
@Service
public class CodeValidationService {

    /** Logger utilisé pour le suivi et le débogage du service. */
    private static final Logger logger = LoggerFactory.getLogger(CodeValidationService.class);

    /** Service de gestion du SDK Penpot (injection de dépendance) */
    private final PenpotSdkService sdkService;

    /**
     * Constructeur du service de validation.
     *
     * @param sdkService le service gérant la documentation et les méthodes du SDK Penpot
     */
    public CodeValidationService(PenpotSdkService sdkService) {
        this.sdkService = sdkService;
    }

    /**
     * Valide un code JavaScript généré par l’IA.
     * <p>
     * Cette méthode combine plusieurs niveaux de vérification :
     * </p>
     * <ul>
     *   <li>Validation syntaxique (balises et symboles équilibrés)</li>
     *   <li>Contrôle de l’usage des méthodes de l’API Penpot</li>
     *   <li>Détection d’erreurs communes ou de structures suspectes</li>
     * </ul>
     *
     * @param code le code JavaScript brut à valider
     * @return une liste d’objets {@link ValidationError} décrivant les erreurs détectées ;
     *         la liste est vide si le code est valide
     */
    public List<ValidationError> validate(String code) {
        List<ValidationError> errors = new ArrayList<>();

        if (code == null || code.trim().isEmpty()) {
            errors.add(new ValidationError("EMPTY_CODE", "Le code est vide"));
            return errors;
        }

        logger.debug("Validation du code: {} caractères", code.length());

        // Vérifie que la documentation API est chargée
        if (sdkService.getMethodCount() == 0) {
            logger.error("La documentation API Penpot n'est pas chargée !");
            errors.add(new ValidationError("API_DOC_ERROR", 
                "Impossible de valider : documentation API non disponible"));
            return errors;
        }

        // Validations dans l'ordre de priorité
        errors.addAll(validateSyntax(code));
        errors.addAll(validatePenpotApiUsage(code));
        errors.addAll(validateCommonMistakes(code));

        logger.debug("Validation terminée: {} erreur(s)", errors.size());

        return errors;
    }

    /**
     * Effectue une validation syntaxique basique du code JavaScript.
     * <p>
     * Cette méthode détecte notamment :
     * </p>
     * <ul>
     *   <li>Les parenthèses, crochets ou accolades non équilibrés</li>
     *   <li>L’utilisation de mots-clés interdits ou obsolètes</li>
     * </ul>
     *
     * @param code le code JavaScript à analyser
     * @return la liste des erreurs syntaxiques détectées
     */
    private List<ValidationError> validateSyntax(String code) {
        List<ValidationError> errors = new ArrayList<>();

        if (!areBracketsBalanced(code)) {
            errors.add(new ValidationError("SYNTAX", 
                "Parenthèses, accolades ou crochets non équilibrés"));
        }

        // Validation des mots-clés problématiques (goto strictement interdit)
        Pattern invalidKeywordsStrict = Pattern.compile("\\bgoto\\b");
        if (invalidKeywordsStrict.matcher(code).find()) {
            errors.add(new ValidationError("SYNTAX", 
                "Utilisation de mots-clés JavaScript déconseillés"));
        }

        // Vérifier 'with' uniquement en tant que statement, pas dans les noms de méthodes
        Pattern withStatement = Pattern.compile("\\bwith\\s*\\(");
        if (withStatement.matcher(code).find()) {
            errors.add(new ValidationError("SYNTAX", 
                "Utilisation du mot-clé 'with' déconseillée"));
        }

        return errors;
    }

    /**
     * Valide les appels à l’API Penpot dans le code.
     * <p>
     * Elle vérifie que les méthodes invoquées via <code>penpot.method()</code>
     * existent bien et qu’elles respectent la casse et la nomenclature.
     * Des erreurs explicites sont retournées pour les méthodes inconnues ou
     * mal nommées.
     * </p>
     *
     * @param code le code contenant les appels à l’API Penpot
     * @return la liste des erreurs d’usage d’API détectées
     */
    private List<ValidationError> validatePenpotApiUsage(String code) {
        List<ValidationError> errors = new ArrayList<>();

        // Pattern pour capturer les appels à penpot.method()
        Pattern penpotCallPattern = Pattern.compile("penpot\\.([a-zA-Z]+)\\s*\\(");
        Matcher matcher = penpotCallPattern.matcher(code);

        while (matcher.find()) {
            String methodName = matcher.group(1);

            if (!sdkService.methodExists(methodName)) {
                List<String> suggestions = suggestSimilarMethods(methodName, 3);
                
                String errorMsg = "Méthode Penpot inconnue: penpot." + methodName + "()";
                if (!suggestions.isEmpty()) {
                    errorMsg += ". Suggestions: " + String.join(", ", suggestions);
                }
                
                errors.add(new ValidationError("API_USAGE", errorMsg));
            }
        }

        // Vérifier les erreurs de casse courantes
        if (code.contains("createTextBox")) {
            errors.add(new ValidationError(
                "API_USAGE",
                "createTextBox() n'existe pas. Utilisez createText() à la place"
            ));
        }

        // Vérifier uniquement "Penpot." (avec majuscule) en tant qu'objet
        Pattern penpotUpperCase = Pattern.compile("\\bPenpot\\.");
        if (penpotUpperCase.matcher(code).find()) {
            errors.add(new ValidationError(
                "API_USAGE",
                "Utilisez 'penpot' (minuscule) et non 'Penpot'"
            ));
        }

        return errors;
    }

    /**
     * Détecte les erreurs et incohérences courantes dans le code généré.
     * <p>
     * Cette méthode identifie des cas fréquents tels que :
     * </p>
     * <ul>
     *   <li>Variables non déclarées</li>
     *   <li>Fonctions sans <code>return</code></li>
     *   <li>Chaînes de caractères non fermées</li>
     * </ul>
     *
     * @param code le code JavaScript à vérifier
     * @return la liste des erreurs ou avertissements détectés
     */
    private List<ValidationError> validateCommonMistakes(String code) {
        List<ValidationError> errors = new ArrayList<>();

        // Validation des guillemets
        if (!areQuotesBalanced(code)) {
            ValidationError warning = new ValidationError(
                "SYNTAX",
                "Guillemets potentiellement non appariés (vérification recommandée)"
            );
            warning.setSeverity("WARNING");
            errors.add(warning);
        }

        // Vérifier les fonctions sans return (avec avertissement, pas erreur)
        int functionCount = countOccurrences(code, "function");
        int returnCount = countOccurrences(code, "return");

        if (functionCount > 0 && returnCount == 0) {
            ValidationError warning = new ValidationError(
                "SEMANTIC",
                "Fonction(s) sans instruction return (peut être intentionnel)"
            );
            warning.setSeverity("WARNING");
            errors.add(warning);
        }

        // Vérifier les variables potentiellement non déclarées
        if (hasPotentialUndeclaredVariables(code)) {
            ValidationError warning = new ValidationError(
                "SEMANTIC",
                "Variables potentiellement non déclarées (const/let/var manquant)"
            );
            warning.setSeverity("WARNING");
            errors.add(warning);
        }

        return errors;
    }

    /**
     * Suggère des méthodes similaires pour une méthode invalide.
     * <p>
     * Utilise la distance de Levenshtein pour trouver les méthodes les plus proches
     * parmi celles disponibles dans la documentation API.
     * </p>
     *
     * @param invalidMethod la méthode invalide
     * @param maxSuggestions nombre maximum de suggestions
     * @return liste des suggestions (triées par similarité)
     */
    private List<String> suggestSimilarMethods(String invalidMethod, int maxSuggestions) {
        return sdkService.getMethodsByCategory(null).stream()
            .map(method -> new java.util.AbstractMap.SimpleEntry<>(
                method.getName(),
                CodeUtils.levenshteinDistance(
                    invalidMethod.toLowerCase(), 
                    method.getName().toLowerCase()
                )
            ))
            .sorted(java.util.Map.Entry.comparingByValue())
            .limit(maxSuggestions)
            .filter(entry -> entry.getValue() <= 3)
            .map(java.util.Map.Entry::getKey)
            .toList();
    }

    /**
     * Vérifie l'équilibre des guillemets de manière intelligente.
     * 
     * @param code le code à analyser
     * @return {@code true} si les symboles sont équilibrés, {@code false} sinon
     */
    private boolean areQuotesBalanced(String code) {
        int singleQuotes = 0;
        int doubleQuotes = 0;
        int backticks = 0;
        boolean escaped = false;

        for (int i = 0; i < code.length(); i++) {
            char c = code.charAt(i);

            if (escaped) {
                escaped = false;
                continue;
            }

            if (c == '\\') {
                escaped = true;
                continue;
            }

            switch (c) {
                case '\'' -> singleQuotes++;
                case '"' -> doubleQuotes++;
                case '`' -> backticks++;
            }
        }

        // Tolérance de ±1 pour tenir compte des commentaires ou du code partiel
        return (singleQuotes % 2 <= 1) && (doubleQuotes % 2 <= 1) && (backticks % 2 == 0);
    }

    /**
     * Détecte les variables non déclarées dans le code.
     *
     * @param code le code à analyser
     * @return {@code true} si des variables non déclarées sont suspectées
     */
    private boolean hasPotentialUndeclaredVariables(String code) {
        Pattern undeclaredPattern = Pattern.compile(
            "^\\s*[a-z_][a-zA-Z0-9_]*\\s*=",
            Pattern.MULTILINE
        );
        Matcher matcher = undeclaredPattern.matcher(code);

        if (!matcher.find()) return false;

        return !code.contains("const ") && !code.contains("let ") && !code.contains("var ");
    }

    /**
     * Vérifie l’équilibre des parenthèses, crochets et accolades dans le code.
     * <p>
     * Les chaînes de caractères sont correctement ignorées afin d’éviter
     * les faux positifs liés à la présence de symboles dans des littéraux.
     * </p>
     *
     * @param code le code à analyser
     * @return {@code true} si les symboles sont équilibrés, {@code false} sinon
     */
    private boolean areBracketsBalanced(String code) {
        int parentheses = 0;
        int braces = 0;
        int brackets = 0;
        boolean inString = false;
        char stringChar = 0;

        for (int i = 0; i < code.length(); i++) {
            char c = code.charAt(i);

            // Gérer les guillemets et chaînes de caractères
            if ((c == '"' || c == '\'' || c == '`') && (i == 0 || code.charAt(i - 1) != '\\')) {
                if (!inString) {
                    inString = true;
                    stringChar = c;
                } else if (c == stringChar) {
                    inString = false;
                }
                continue;
            }

            if (inString) continue;

            switch (c) {
                case '(' -> parentheses++;
                case ')' -> parentheses--;
                case '{' -> braces++;
                case '}' -> braces--;
                case '[' -> brackets++;
                case ']' -> brackets--;
            }

            if (parentheses < 0 || braces < 0 || brackets < 0) {
                return false;
            }
        }

        return parentheses == 0 && braces == 0 && brackets == 0;
    }

    /**
     * Compte le nombre d’occurrences d’une sous-chaîne dans une chaîne donnée.
     *
     * @param text      la chaîne complète à parcourir
     * @param substring la sous-chaîne à rechercher
     * @return le nombre d’occurrences trouvées
     */
    private int countOccurrences(String text, String substring) {
        if (text == null || substring == null || substring.isEmpty()) {
            return 0;
        }
        return (text.length() - text.replace(substring, "").length()) / substring.length();
    }
}