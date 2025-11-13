package com.example.aiPoc.utils;

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
            if (!trimmed.startsWith("//") || 
                trimmed.contains("TODO") || 
                trimmed.contains("FIXME")) {
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
     * Calcule la distance de Levenshtein entre deux chaînes.
     *
     * @param s1 première chaîne
     * @param s2 seconde chaîne
     * @return nombre minimal d’opérations nécessaires pour transformer s1 en s2
     */
    public static int levenshteinDistance(String s1, String s2) {
        if (s1 == null || s2 == null) {
            return Math.max(
                s1 == null ? 0 : s1.length(),
                s2 == null ? 0 : s2.length()
            );
        }

        int[][] dp = new int[s1.length() + 1][s2.length() + 1];

        for (int i = 0; i <= s1.length(); i++) dp[i][0] = i;
        for (int j = 0; j <= s2.length(); j++) dp[0][j] = j;

        for (int i = 1; i <= s1.length(); i++) {
            for (int j = 1; j <= s2.length(); j++) {
                int cost = s1.charAt(i - 1) == s2.charAt(j - 1) ? 0 : 1;
                dp[i][j] = Math.min(Math.min(
                    dp[i - 1][j] + 1,
                    dp[i][j - 1] + 1),
                    dp[i - 1][j - 1] + cost
                );
            }
        }

        return dp[s1.length()][s2.length()];
    }

    /**
     * Calcule le pourcentage de similarité entre deux chaînes.
     * <p>
     * Utilise la distance de Levenshtein pour calculer un score de similarité
     * entre 0 (complètement différent) et 1 (identique).
     * </p>
     *
     * @param s1 première chaîne
     * @param s2 seconde chaîne
     * @return score de similarité entre 0.0 et 1.0
     */
    public static double similarityScore(String s1, String s2) {
        if (s1 == null || s2 == null) return 0.0;
        if (s1.equals(s2)) return 1.0;

        int maxLength = Math.max(s1.length(), s2.length());
        if (maxLength == 0) return 1.0;

        int distance = levenshteinDistance(s1, s2);
        return 1.0 - ((double) distance / maxLength);
    }

    /**
     * Supprime les phrases explicatives courantes en début de code.
     * <p>
     * Détecte et supprime les phrases comme "Let me create...", "Wait, maybe...", etc.
     * </p>
     */
    public static String removeExplanatoryText(String code) {
        if (code == null) return "";

        String[] problematicPhrases = {
            "(?i)^.*?wait,.*?$",
            "(?i)^.*?let me.*?$",
            "(?i)^.*?maybe.*?$",
            "(?i)^.*?perhaps.*?$",
            "(?i)^.*?for example.*?$",
            "(?i)^.*?first,.*?$",
            "(?i)^.*?then,.*?$"
        };

        String cleaned = code;
        for (String pattern : problematicPhrases) {
            cleaned = cleaned.replaceAll(pattern, "");
        }

        return cleaned.trim();
    }

    /**
     * Vérifie si le code contient du texte explicatif.
     */
    public static boolean containsExplanatoryText(String code) {
        if (code == null || code.isEmpty()) return false;

        String lowerCode = code.toLowerCase();
        String[] indicators = {
            "wait", "let me", "maybe", "perhaps", "for example",
            "voici", "alors", "d'abord", "ensuite", "maintenant",
            "i will", "i'll", "we can", "we need"
        };

        for (String indicator : indicators) {
            if (lowerCode.contains(indicator)) {
                return true;
            }
        }

        return false;
    }
}