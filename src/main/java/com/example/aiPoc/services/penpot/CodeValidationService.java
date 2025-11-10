package com.example.aiPoc.services.penpot;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.example.aiPoc.models.ValidationError;

/**
 * Service chargé de la validation du code JavaScript généré automatiquement,
 * notamment pour des scripts destinés à l'API Penpot.
 * <p>
 * Ce service effectue plusieurs niveaux de validation :
 * </p>
 * <ul>
 *   <li>Validation syntaxique de base (équilibre des parenthèses, guillemets, etc.)</li>
 *   <li>Validation sémantique (usage correct des méthodes de l’API Penpot)</li>
 *   <li>Détection d’erreurs ou d’incohérences communes dans le code généré</li>
 * </ul>
 *
 * <p>
 * L’objectif est de garantir que le code produit par le modèle d’IA soit
 * exécutable, cohérent et conforme aux conventions de l’API cible.
 * </p>
 */
@Service
public class CodeValidationService {

    private static final Logger logger = LoggerFactory.getLogger(CodeValidationService.class);

    /**
     * Liste des méthodes valides exposées par l'API Penpot.
     * <p>
     * Cette liste est utilisée pour valider les appels d’API trouvés dans
     * le code JavaScript fourni.
     * </p>
     */
    private static final List<String> VALID_PENPOT_METHODS = List.of(
        "createRectangle",
        "createCircle",
        "createText",
        "createEllipse",
        "createPath",
        "group",
        "ungroup",
        "alignHorizontal",
        "alignVertical",
        "distributeHorizontal",
        "distributeVertical"
    );

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
            errors.add(new ValidationError("SYNTAX", "Parenthèses, accolades ou crochets non équilibrés"));
        }

        Pattern invalidKeywords = Pattern.compile("\\b(goto|with)\\b");
        if (invalidKeywords.matcher(code).find()) {
            errors.add(new ValidationError("SYNTAX", "Utilisation de mots-clés JavaScript déconseillés"));
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

        Pattern penpotCallPattern = Pattern.compile("penpot\\.([a-zA-Z]+)\\s*\\(");
        Matcher matcher = penpotCallPattern.matcher(code);

        while (matcher.find()) {
            String methodName = matcher.group(1);

            if (!VALID_PENPOT_METHODS.contains(methodName)) {
                errors.add(new ValidationError(
                    "API_USAGE",
                    "Méthode Penpot inconnue: penpot." + methodName + "()"
                ));
            }
        }

        if (code.contains("createTextBox")) {
            errors.add(new ValidationError(
                "API_USAGE",
                "createTextBox() n'existe pas. Utilisez createText() à la place"
            ));
        }

        if (code.contains("Penpot.")) {
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

        if (code.matches(".*\\b[a-z]+\\s*=.*") && !code.contains("const") && !code.contains("let") && !code.contains("var")) {
            errors.add(new ValidationError(
                "SEMANTIC",
                "Certaines variables semblent non déclarées (manque const/let/var)",
                null,
                null
            ));
        }

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

        int singleQuotes = countOccurrences(code, "'");
        int doubleQuotes = countOccurrences(code, "\"");
        int backticks = countOccurrences(code, "`");

        if (singleQuotes % 2 != 0) {
            errors.add(new ValidationError("SYNTAX", "Guillemets simples non appariés"));
        }
        if (doubleQuotes % 2 != 0) {
            errors.add(new ValidationError("SYNTAX", "Guillemets doubles non appariés"));
        }
        if (backticks % 2 != 0) {
            errors.add(new ValidationError("SYNTAX", "Backticks non appariés"));
        }

        return errors;
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
        return (text.length() - text.replace(substring, "").length()) / substring.length();
    }

    /**
     * Indique si un code est entièrement valide.
     * <p>
     * Cette méthode exécute la validation complète et renvoie {@code true}
     * uniquement si aucune erreur ou avertissement n’a été détecté.
     * </p>
     *
     * @param code le code à vérifier
     * @return {@code true} si le code est valide, {@code false} sinon
     */
    public boolean isValid(String code) {
        List<ValidationError> errors = validate(code);
        return errors.isEmpty();
    }

    /**
     * Retourne uniquement les erreurs critiques détectées dans le code.
     * <p>
     * Les avertissements (warnings) sont exclus du résultat.
     * </p>
     *
     * @param code le code à analyser
     * @return la liste des erreurs critiques (sévérité {@code ERROR})
     */
    public List<ValidationError> getCriticalErrors(String code) {
        return validate(code).stream()
            .filter(error -> "ERROR".equals(error.getSeverity()))
            .toList();
    }
}