package com.example.aiPoc.utils;

/**
 * Classe utilitaire permettant d'estimer le nombre de tokens dans un texte ou du code source.
 *
 * <p>
 * Ces estimations sont basées sur des moyennes statistiques simples :
 * <ul>
 *   <li>1 token ≈ 4 caractères pour le texte en anglais</li>
 *   <li>1 token ≈ 3.5 caractères pour le code source</li>
 * </ul>
 * </p>
 *
 * <p><b>Note :</b> Les valeurs obtenues sont approximatives.</p>
 */
public class TokenEstimator {

    /** Ratio moyen de caractères par token pour le texte (anglais). */
    private static final double CHARS_PER_TOKEN = 4.0;

    /** Ratio moyen de caractères par token pour le code source. */
    private static final double CHARS_PER_TOKEN_CODE = 3.5;

    /**
     * Estime le nombre de tokens contenus dans un texte.
     *
     * @param text le texte dont on veut estimer le nombre de tokens ; peut être {@code null}.
     * @return le nombre estimé de tokens (0 si le texte est nul ou vide).
     */
    public static int estimateTokens(String text) {
        if (text == null || text.isEmpty()) return 0;
        return (int) Math.ceil(text.length() / CHARS_PER_TOKEN);
    }

    /**
     * Estime le nombre de tokens contenus dans un extrait de code source.
     *
     * @param code le code dont on veut estimer le nombre de tokens ; peut être {@code null}.
     * @return le nombre estimé de tokens (0 si le code est nul ou vide).
     */
    public static int estimateTokensForCode(String code) {
        if (code == null || code.isEmpty()) return 0;
        return (int) Math.ceil(code.length() / CHARS_PER_TOKEN_CODE);
    }

    /**
     * Estimation avancée du nombre de tokens dans un texte, basée sur la segmentation
     * par espaces et ponctuation.
     *
     * <p>Cette méthode tient compte des mots longs qui peuvent compter comme plusieurs tokens.</p>
     *
     * @param text le texte à analyser ; peut être {@code null}.
     * @return une estimation affinée du nombre de tokens (0 si le texte est nul ou vide).
     */
    public static int estimateTokensAdvanced(String text) {
        if (text == null || text.isEmpty()) return 0;

        String[] words = text.split("\\s+|(?=[.,!?;:()\\[\\]{}])");

        int tokenCount = 0;
        for (String word : words) {
            if (word.isEmpty()) continue;

            if (word.length() > 10) {
                tokenCount += (int) Math.ceil(word.length() / 6.0);
            } else {
                tokenCount++;
            }
        }

        return tokenCount;
    }

    /**
     * Estime le coût total en tokens d'une requête comprenant un prompt et une réponse.
     *
     * @param prompt   le texte d'entrée de la requête.
     * @param response la réponse générée.
     * @return un objet {@link TokenCost} contenant les estimations d'entrée, sortie et total.
     */
    public static TokenCost estimateCost(String prompt, String response) {
        int inputTokens = estimateTokens(prompt);
        int outputTokens = estimateTokens(response);
        int totalTokens = inputTokens + outputTokens;

        return new TokenCost(inputTokens, outputTokens, totalTokens);
    }

    /**
     * Vérifie si un texte dépasse une limite maximale de tokens estimée.
     *
     * @param text      le texte à évaluer.
     * @param maxTokens le nombre maximal de tokens autorisés.
     * @return {@code true} si le texte dépasse la limite estimée, {@code false} sinon.
     */
    public static boolean exceedsLimit(String text, int maxTokens) {
        return estimateTokens(text) > maxTokens;
    }

    /**
     * Tronque un texte afin qu'il respecte une limite maximale estimée de tokens.
     *
     * <p>Le texte est coupé de manière proportionnelle à la limite et se termine par
     * des points de suspension ("...") si une troncature est effectuée.</p>
     *
     * @param text      le texte à tronquer.
     * @param maxTokens la limite maximale de tokens à ne pas dépasser.
     * @return le texte tronqué si nécessaire ; sinon le texte original.
     */
    public static String truncateToTokenLimit(String text, int maxTokens) {
        if (text == null || text.isEmpty()) return text;

        int estimatedTokens = estimateTokens(text);
        if (estimatedTokens <= maxTokens) return text;

        double ratio = (double) maxTokens / estimatedTokens;
        int targetLength = (int) (text.length() * ratio);
        if (targetLength >= text.length()) return text;

        return text.substring(0, targetLength) + "...";
    }

    /**
     * Classe interne représentant le coût estimé en tokens d'une requête.
     *
     * <p>Elle regroupe le nombre de tokens pour l'entrée, la sortie et le total.</p>
     */
    public static class TokenCost {

        /** Nombre estimé de tokens pour le prompt d'entrée. */
        private final int inputTokens;

        /** Nombre estimé de tokens pour la réponse générée. */
        private final int outputTokens;

        /** Nombre total de tokens estimés (entrée + sortie). */
        private final int totalTokens;

        /**
         * Construit un objet {@code TokenCost}.
         *
         * @param inputTokens  nombre de tokens estimés pour le texte d'entrée.
         * @param outputTokens nombre de tokens estimés pour la réponse générée.
         * @param totalTokens  nombre total de tokens (entrée + sortie).
         */
        public TokenCost(int inputTokens, int outputTokens, int totalTokens) {
            this.inputTokens = inputTokens;
            this.outputTokens = outputTokens;
            this.totalTokens = totalTokens;
        }

        /**
         * Retourne le nombre de tokens estimés pour le texte d'entrée.
         *
         * @return le nombre de tokens d'entrée.
         */
        public int getInputTokens() {
            return inputTokens;
        }

        /**
         * Retourne le nombre de tokens estimés pour la réponse générée.
         *
         * @return le nombre de tokens de sortie.
         */
        public int getOutputTokens() {
            return outputTokens;
        }

        /**
         * Retourne le nombre total de tokens (entrée + sortie).
         *
         * @return le total estimé de tokens.
         */
        public int getTotalTokens() {
            return totalTokens;
        }

        @Override
        public String toString() {
            return "TokenCost{" +
                    "input=" + inputTokens +
                    ", output=" + outputTokens +
                    ", total=" + totalTokens +
                    '}';
        }
    }
}