/**
 * Requête envoyée au moteur de génération de code.
 *
 * Représente l'intention utilisateur et les options de génération associées.
 */
export interface CodeGenerationRequest {
    /**
     * Prompt utilisateur nettoyé et prêt à être interprété.
     */
    prompt: string;

    /**
     * Stratégie de génération appliquée.
     *
     * Exemples : `creation`, `modification`
     */
    strategy: string;

    /**
     * Indique si la validation doit être incluse dans la génération.
     */
    includeValidation: boolean;

    /**
     * Indique si le code généré doit être nettoyé / formaté.
     */
    cleanCode: boolean;

    /**
     * Contexte optionnel d'une page existante utilisé lors d'une modification.
     */
    pageContext?: any;
}

/**
 * Résultat de validation d'une stratégie de génération.
 */
export interface ValidationResult {
    /**
     * Indique si la validation est réussie.
     */
    valid: boolean;

    /**
     * Message expliquant l'échec de la validation, le cas échéant.
     */
    message?: string;
}

/**
 * Erreur issue du processus de validation du code généré.
 */
export interface ValidationError {
    /**
     * Type ou catégorie de l'erreur.
     */
    type: string;

    /**
     * Message descriptif de l'erreur.
     */
    message: string;

    /**
     * Ligne concernée par l'erreur, si applicable.
     */
    line?: number;

    /**
     * Colonne concernée par l'erreur, si applicable.
     */
    column?: number;

    /**
     * Niveau de gravité de l'erreur.
     *
     * Exemples : `ERROR`, `WARNING`
     */
    severity: string;
}

/**
 * Réponse complète du moteur de génération de code.
 */
export interface CodeGenerationResponse {
    /**
     * Stratégie utilisée pour cette génération.
     */
    strategy: string;

    /**
     * Prompt utilisateur initial.
     */
    userPrompt: string;

    /**
     * Code généré final.
     */
    generatedCode: string;

    /**
     * Indique si le code est considéré comme valide.
     */
    valid: boolean;

    /**
     * Liste des erreurs ou avertissements détectés.
     */
    validationErrors: ValidationError[];

    /**
     * Longueur du code généré (en caractères).
     */
    codeLength: number;

    /**
     * Temps de génération en millisecondes.
     */
    generationTimeMs: number;

    /**
     * Prompt enrichi utilisé en interne (optionnel).
     */
    enrichedPrompt?: string;

    /**
     * Réponse brute du moteur de génération (debug / audit).
     */
    rawResponse?: string;

    /**
     * Estimation du nombre de tokens consommés.
     */
    promptTokensEstimate?: number;
}

/**
 * Types d'état possibles pour l'affichage de statut utilisateur.
 */
export type StatusType = 'info' | 'success' | 'error' | 'warning';

/**
 * État global de l'application côté UI.
 */
export interface AppState {
    /**
     * Code actuellement affiché / édité.
     */
    currentCode: string;

    /**
     * Indique si une génération est en cours.
     */
    isGenerating: boolean;

    /**
     * Contexte de page courant, s'il existe.
     */
    currentPageContext: unknown;

    /**
     * Identifiant du fichier courant.
     */
    currentFileId: string;

    /**
     * Identifiant de la page courante.
     */
    currentPageId: string;
}