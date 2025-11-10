package com.example.aiPoc.services.evaluation;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.example.aiPoc.dto.response.CodeGenerationResponse;
import com.example.aiPoc.models.EvaluationResult;
import com.example.aiPoc.models.ValidationError;
import com.example.aiPoc.utils.TokenEstimator;

/**
 * Service chargé d'évaluer automatiquement les performances des modèles IA
 * selon les critères définis dans le POC OllMark.
 * <p>
 * Chaque évaluation produit un {@link EvaluationResult} contenant des scores
 * qualitatifs et quantitatifs tels que la stabilité, la cohérence,
 * la créativité ou la richesse structurelle du code généré.
 */
@Service
public class EvaluationService {

    private static final Logger logger = LoggerFactory.getLogger(EvaluationService.class);

    /**
     * Évalue une réponse de génération de code selon plusieurs critères.
     *
     * @param modelName     le nom du modèle IA utilisé
     * @param promptId      l'identifiant du prompt d'origine
     * @param userPrompt    le texte du prompt saisi par l'utilisateur
     * @param response      la réponse principale générée par le modèle
     * @param stabilityTests les réponses issues de plusieurs exécutions identiques pour évaluer la stabilité
     * @return un objet {@link EvaluationResult} contenant les scores et observations
     */
    public EvaluationResult evaluate(
            String modelName,
            String promptId,
            String userPrompt,
            CodeGenerationResponse response,
            List<CodeGenerationResponse> stabilityTests) {

        logger.info("Évaluation pour modèle={}, prompt={}", modelName, promptId);

        EvaluationResult result = new EvaluationResult();
        result.setModeleIA(modelName);
        result.setIdPrompt(promptId);
        result.setPromptTexte(userPrompt);
        result.setTimestamp(LocalDateTime.now());

        // Critères quantitatifs
        result.setTempsReponseS(response.getGenerationTimeMs() / 1000.0);
        result.setTokensMoyens(estimateTokens(response));

        // Critères qualitatifs
        result.setStabilite(calculateStability(stabilityTests));
        result.setCoherence(evaluateCoherence(response));
        result.setRespectSujet(evaluateRespectSujet(response, userPrompt));
        result.setRichesseStructurelle(evaluateRichesse(response));
        result.setCreativite(evaluateCreativite(response));

        // Moyenne et réalisabilité
        result.calculateMoyenneGlobale();
        result.setRealisable(isRealisable(response));

        // Observations textuelles
        result.setObservations(generateObservations(result, response));

        logger.info("Évaluation terminée: moyenne={}/5, réalisable={}", 
                   result.getMoyenneGlobale(), result.isRealisable());

        return result;
    }

    /**
     * Évalue la qualité du temps de réponse d'un modèle IA.
     *
     * @param tempsSecondes durée de génération en secondes
     * @return une appréciation textuelle : « Excellent », « Acceptable », etc.
     */
    public String evaluateTempsReponse(double tempsSecondes) {
        if (tempsSecondes < 3.0) return "Excellent";
        if (tempsSecondes < 8.0) return "Acceptable";
        if (tempsSecondes < 15.0) return "Lent mais exploitable";
        return "Problématique";
    }

    /**
     * Estime le nombre de tokens consommés à partir du code généré.
     *
     * @param response la réponse contenant le code généré
     * @return estimation du nombre de tokens utilisés
     */
    private int estimateTokens(CodeGenerationResponse response) {
        String code = response.getGeneratedCode();
        if (code == null) return 0;

        return TokenEstimator.estimateTokensForCode(code);
    }

    /**
     * Calcule un score de stabilité entre plusieurs générations identiques.
     *
     * @param responses la liste de réponses obtenues à partir du même prompt
     * @return un score entre 1 et 5 selon la similarité moyenne des codes
     */
    private int calculateStability(List<CodeGenerationResponse> responses) {
        if (responses == null || responses.size() < 2) {
            logger.warn("Pas assez de réponses pour calculer la stabilité");
            return 3;
        }

        List<String> codes = responses.stream()
            .map(CodeGenerationResponse::getGeneratedCode)
            .toList();

        double similarityScore = calculateAverageSimilarity(codes);

        if (similarityScore > 0.95) return 5;
        if (similarityScore > 0.80) return 4;
        if (similarityScore > 0.60) return 3;
        if (similarityScore > 0.40) return 2;
        return 1;
    }

    /**
     * Évalue la cohérence logique et syntaxique du code généré.
     *
     * @param response la réponse du modèle à évaluer
     * @return un score de cohérence compris entre 1 et 5
     */
    private int evaluateCoherence(CodeGenerationResponse response) {
        int score = 5;

        if (!response.isValid()) {
            score -= 2;
        }

        List<ValidationError> errors = response.getValidationErrors();
        if (errors != null) {
            long syntaxErrors = errors.stream()
                .filter(e -> "SYNTAX".equals(e.getType()))
                .count();
            score -= (int) Math.min(syntaxErrors, 2);

            long semanticErrors = errors.stream()
                .filter(e -> "SEMANTIC".equals(e.getType()))
                .count();
            score -= (int) Math.min(semanticErrors, 1);
        }

        String code = response.getGeneratedCode();
        if (code != null) {
            if (code.length() < 20) score -= 2;

            if (!code.contains("penpot.") &&
                !code.contains("const") &&
                !code.contains("let")
            ) {
                score -= 1;
            }
        }

        return Math.max(1, Math.min(5, score));
    }

    /**
     * Évalue la pertinence du code généré par rapport au sujet du prompt.
     *
     * @param response   la réponse du modèle à évaluer
     * @param userPrompt le texte du prompt fourni par l'utilisateur
     * @return un score entre 1 et 5 selon le respect du sujet
     */
    private int evaluateRespectSujet(CodeGenerationResponse response, String userPrompt) {
        String code = response.getGeneratedCode();
        if (code == null || code.isEmpty()) return 1;

        int score = 3;
        String promptLower = userPrompt.toLowerCase();
        String codeLower = code.toLowerCase();

        List<String> elements = new ArrayList<>();

        if (promptLower.contains("rectangle") || promptLower.contains("carré")) elements.add("rectangle");
        if (promptLower.contains("cercle") || promptLower.contains("rond")) elements.add("circle");
        if (promptLower.contains("texte") || promptLower.contains("text")) elements.add("text");
        if (promptLower.contains("ligne")) elements.add("line");
        if (promptLower.contains("ellipse")) elements.add("ellipse");

        int found = 0;
        for (String element : elements) {
            if (codeLower.contains("create" + element)) found++;
        }

        if (elements.isEmpty()) return response.isValid() ? 4 : 3;

        double respectRatio = (double) found / elements.size();

        if (respectRatio >= 1.0) return 5;
        if (respectRatio >= 0.8) return 4;
        if (respectRatio >= 0.5) return 3;
        if (respectRatio >= 0.3) return 2;
        return 1;
    }

    /**
     * Évalue la richesse structurelle du code généré.
     *
     * @param response la réponse du modèle à évaluer
     * @return un score entre 1 et 5 selon la complexité et la diversité du code
     */
    private int evaluateRichesse(CodeGenerationResponse response) {
        String code = response.getGeneratedCode();
        if (code == null || code.isEmpty()) return 1;

        int score = 0;

        if (code.contains("penpot.")) score++;
        if (code.contains("const") || code.contains("let")) score++;
        if (code.contains("fills") || code.contains("strokes") || code.contains("color")) score++;

        int positionCount = 0;
        if (code.contains("x:") || code.contains("x =")) positionCount++;
        if (code.contains("y:") || code.contains("y =")) positionCount++;
        if (code.contains("width") || code.contains("height")) positionCount++;
        if (positionCount >= 2) score++;

        int createCount = countOccurrences(code, "create");
        if (createCount >= 2) score++;

        return Math.max(1, Math.min(5, score));
    }

    /**
     * Évalue la créativité et la variété du code généré.
     *
     * @param response la réponse du modèle à évaluer
     * @return un score de créativité entre 1 et 5
     */
    private int evaluateCreativite(CodeGenerationResponse response) {
        String code = response.getGeneratedCode();
        if (code == null || code.isEmpty()) return 1;

        int score = 3;

        if (code.contains("for") || code.contains("while")) score++;
        int colorCount = countOccurrences(code, "#") + 
                        countOccurrences(code, "rgb") +
                        countOccurrences(code, "hsl");
        if (colorCount >= 3) score++;
        if (code.contains("//") || code.contains("/*")) {
            if (countOccurrences(code, "//") <= 5) score++;
        }
        if (code.length() < 100) score--;
        if (containsDescriptiveNames(code)) score++;

        return Math.max(1, Math.min(5, score));
    }

    /**
     * Vérifie si le code généré est réalisable dans Penpot.
     *
     * @param response la réponse du modèle
     * @return {@code true} si le code est exécutable et contient un appel Penpot
     */
    private boolean isRealisable(CodeGenerationResponse response) {
        return response.isValid() &&
                response.getGeneratedCode() != null &&
                response.getGeneratedCode().contains("penpot.");
    }

    /**
     * Génère des observations textuelles synthétiques à partir des résultats.
     *
     * @param result   le résultat global de l'évaluation
     * @param response la réponse du modèle
     * @return une chaîne contenant des remarques analytiques
     */
    private String generateObservations(
        EvaluationResult result,
        CodeGenerationResponse response
    ) {
        StringBuilder obs = new StringBuilder();
        String tempsEval = evaluateTempsReponse(result.getTempsReponseS());
        obs.append("Temps: ").append(tempsEval).append(". ");

        if (!response.isValid()) {
            obs.append("Erreurs de validation: ")
               .append(response.getValidationErrors().size())
               .append(". ");
        }

        if (result.getMoyenneGlobale() >= 4.0) obs.append("Excellente qualité globale. ");
        else if (result.getMoyenneGlobale() < 2.5) obs.append("Qualité insuffisante. ");

        int codeLength = response.getCodeLength();
        if (codeLength < 50) obs.append("Code très court. ");
        else if (codeLength > 1000) obs.append("Code très long. ");

        return obs.toString().trim();
    }

    /**
     * Calcule la similarité moyenne entre plusieurs codes.
     *
     * @param codes la liste des codes à comparer
     * @return une valeur entre 0 et 1 représentant la similarité moyenne
     */
    private double calculateAverageSimilarity(List<String> codes) {
        if (codes.size() < 2) return 1.0;

        double totalSimilarity = 0.0;
        int comparisons = 0;

        for (int i = 0; i < codes.size() - 1; i++) {
            for (int j = i + 1; j < codes.size(); j++) {
                totalSimilarity += calculateSimilarity(codes.get(i), codes.get(j));
                comparisons++;
            }
        }

        return comparisons > 0 ? totalSimilarity / comparisons : 0.0;
    }

    /**
     * Calcule la similarité entre deux chaînes à l’aide de la distance de Levenshtein.
     *
     * @param s1 première chaîne
     * @param s2 seconde chaîne
     * @return une similarité entre 0 et 1
     */
    private double calculateSimilarity(String s1, String s2) {
        if (s1 == null || s2 == null) return 0.0;
        if (s1.equals(s2)) return 1.0;

        int maxLength = Math.max(s1.length(), s2.length());
        if (maxLength == 0) return 1.0;

        int distance = levenshteinDistance(s1, s2);
        return 1.0 - ((double) distance / maxLength);
    }

    /**
     * Calcule la distance de Levenshtein entre deux chaînes.
     *
     * @param s1 première chaîne
     * @param s2 seconde chaîne
     * @return nombre minimal d’opérations nécessaires pour transformer s1 en s2
     */
    private int levenshteinDistance(String s1, String s2) {
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
     * Compte le nombre d'occurrences d'une sous-chaîne donnée dans une chaîne.
     *
     * @param text      le texte source
     * @param substring la sous-chaîne à rechercher
     * @return le nombre d'occurrences trouvées
     */
    private int countOccurrences(String text, String substring) {
        int count = 0;
        int index = 0;
        while ((index = text.indexOf(substring, index)) != -1) {
            count++;
            index += substring.length();
        }
        return count;
    }

    /**
     * Vérifie la présence de noms de variables descriptifs dans le code.
     *
     * @param code le code à analyser
     * @return {@code true} si au moins un nom de variable descriptif est détecté
     */
    private boolean containsDescriptiveNames(String code) {
        String[] descriptivePatterns = {
            "rectangle", "circle", "text", "button", "card",
            "header", "footer", "container", "wrapper"
        };

        String codeLower = code.toLowerCase();
        for (String pattern : descriptivePatterns) {
            if (codeLower.contains(pattern)) return true;
        }

        return false;
    }
}