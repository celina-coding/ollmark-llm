package com.example.aiPoc.utils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Classe utilitaire regroupant diverses méthodes pour analyser,
 * nettoyer et normaliser du code JavaScript généré automatiquement,
 * notamment par des modèles d'intelligence artificielle.
 * <p>
 * Elle permet notamment de :
 * <ul>
 *   <li>Supprimer les balises parasites (<code>&lt;think&gt;</code>, markdown...)</li>
 *   <li>Extraire le code JavaScript pur à partir d’un texte mixte</li>
 *   <li>Normaliser l’espacement et la mise en forme</li>
 *   <li>Évaluer la validité syntaxique du code</li>
 * </ul>
 * <p>
 * Cette classe ne doit pas être instanciée.
 * Toutes les méthodes sont statiques.
 */
public class CodeUtils {

    /**
     * Supprime les balises Markdown (par exemple <code>```javascript</code>,
     * <code>```js</code> ou <code>```</code>) présentes dans le code.
     *
     * @param code le contenu brut éventuellement encadré de balises Markdown
     * @return le code sans balises Markdown, ou une chaîne vide si l'entrée est nulle
     */
    public static String removeMarkdownFences(String code) {
        if (code == null) return "";
        code = code.replaceAll("```(?:javascript|js)?\\s*", "");
        code = code.replaceAll("```\\s*$", "");
        return code.trim();
    }

    /**
     * Supprime toutes les balises <code>&lt;think&gt;...&lt;/think&gt;</code>
     * utilisées par certains modèles de génération pour contenir du raisonnement interne.
     *
     * @param code le code source potentiellement contenant des balises <code>&lt;think&gt;</code>
     * @return le code sans balises <code>&lt;think&gt;</code>, ou une chaîne vide si l'entrée est nulle
     */
    public static String removeThinkTags(String code) {
        if (code == null) return "";
        return code.replaceAll("(?s)<think>.*?</think>", "").trim();
    }

    /**
     * Supprime les commentaires simples jugés excessifs tout en conservant :
     * <ul>
     *   <li>les commentaires de documentation JSDoc (<code>/** ... *&#47;</code>)</li>
     *   <li>les lignes contenant des mentions importantes (TODO, FIXME)</li>
     * </ul>
     *
     * @param code le code JavaScript à nettoyer
     * @return le code nettoyé de ses commentaires simples superflus
     */
    public static String removeExcessiveComments(String code) {
        if (code == null) return "";

        String[] lines = code.split("\n");
        StringBuilder result = new StringBuilder();

        for (String line : lines) {
            String trimmed = line.trim();
            if (!trimmed.startsWith("//") || trimmed.contains("TODO") || trimmed.contains("FIXME")) {
                result.append(line).append("\n");
            }
        }

        return result.toString().trim();
    }

    /**
     * Normalise l'espacement du code source en :
     * <ul>
     *   <li>Supprimant les lignes vides consécutives</li>
     *   <li>Éliminant les espaces ou tabulations en fin de ligne</li>
     * </ul>
     *
     * @param code le code à normaliser
     * @return le code au format homogène, ou une chaîne vide si l'entrée est nulle
     */
    public static String normalizeWhitespace(String code) {
        if (code == null) return "";
        code = code.replaceAll("\n{3,}", "\n\n");
        code = code.replaceAll("[ \t]+\n", "\n");
        return code.trim();
    }

    /**
     * Extrait uniquement la portion de texte correspondant à du code JavaScript.
     * <p>
     * Le code est détecté dès la première ligne correspondant à un mot-clé typique
     * (par exemple <code>const</code>, <code>function</code>, <code>class</code>, etc.)
     * et toutes les lignes suivantes sont considérées comme du code.
     *
     * @param text le texte d'entrée, pouvant contenir du code mélangé à des explications
     * @return la partie JavaScript extraite, ou le texte original si aucune correspondance n'est trouvée
     */
    public static String extractJavaScriptCode(String text) {
        if (text == null) return "";
        String[] lines = text.split("\n");
        StringBuilder code = new StringBuilder();
        boolean inCode = false;

        for (String line : lines) {
            String trimmed = line.trim();

            if (trimmed.matches("^(const|let|var|function|class|import|export|penpot\\.|//|/\\*).*")) {
                inCode = true;
            }
            if (inCode) {
                code.append(line).append("\n");
            }
        }

        return code.length() > 0 ? code.toString().trim() : text;
    }

    /**
     * Vérifie si le texte contient des balises Markdown (```).
     *
     * @param code le texte à analyser
     * @return {@code true} si des balises Markdown sont présentes, {@code false} sinon
     */
    public static boolean containsMarkdown(String code) {
        if (code == null) return false;
        return code.contains("```");
    }

    /**
     * Vérifie si le texte contient des balises <code>&lt;think&gt;</code>.
     *
     * @param code le texte à analyser
     * @return {@code true} si des balises <code>&lt;think&gt;</code> sont détectées, {@code false} sinon
     */
    public static boolean containsThinkTags(String code) {
        if (code == null) return false;
        return code.contains("<think>");
    }

    /**
     * Compte le nombre total de lignes présentes dans une chaîne.
     *
     * @param code le code source ou texte brut
     * @return le nombre de lignes, ou 0 si l'entrée est nulle ou vide
     */
    public static int countLines(String code) {
        if (code == null || code.isEmpty()) return 0;
        return code.split("\n").length;
    }

    /**
     * Vérifie si un texte semble correspondre à du JavaScript
     * en recherchant certains motifs syntaxiques communs
     * (comme <code>const</code>, <code>function</code>, <code>=&gt;</code>, etc.).
     *
     * @param code le texte à analyser
     * @return {@code true} si le texte ressemble à du code JavaScript, {@code false} sinon
     */
    public static boolean looksLikeJavaScript(String code) {
        if (code == null || code.isEmpty()) return false;

        Pattern jsPattern = Pattern.compile(
            "(const|let|var|function|class|=>|\\{|\\}|\\(|\\)|;)",
            Pattern.MULTILINE
        );

        Matcher matcher = jsPattern.matcher(code);
        return matcher.find();
    }

    /**
     * Effectue un nettoyage complet du code généré :
     * <ol>
     *   <li>Suppression des balises <code>&lt;think&gt;</code></li>
     *   <li>Suppression des balises Markdown</li>
     *   <li>Extraction du code JavaScript</li>
     *   <li>Normalisation des espaces</li>
     * </ol>
     *
     * @param code le code brut généré par un modèle d’IA
     * @return le code nettoyé et normalisé
     */
    public static String cleanGeneratedCode(String code) {
        if (code == null) return "";

        code = removeThinkTags(code);
        code = removeMarkdownFences(code);
        code = extractJavaScriptCode(code);
        code = normalizeWhitespace(code);

        return code;
    }

    /**
     * Extrait le nom d'une fonction JavaScript à partir de sa définition.
     * <p>
     * Exemple :
     * <pre>{@code
     *   function myFunction(param) {
     *       // ...
     *   }
     * }</pre>
     * renverra <code>"myFunction"</code>.
     *
     * @param functionDef la définition complète de la fonction
     * @return le nom de la fonction, ou {@code null} si aucun nom n'est détecté
     */
    public static String extractFunctionName(String functionDef) {
        if (functionDef == null) return null;

        Pattern pattern = Pattern.compile("function\\s+([a-zA-Z_$][a-zA-Z0-9_$]*)");
        Matcher matcher = pattern.matcher(functionDef);

        if (matcher.find()) return matcher.group(1);

        return null;
    }
}