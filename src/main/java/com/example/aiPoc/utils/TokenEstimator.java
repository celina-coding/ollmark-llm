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
}