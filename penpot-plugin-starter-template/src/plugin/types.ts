/**
 * Résultat générique d'une exécution métier ou technique.
 *
 * Cette interface est utilisée pour standardiser le retour
 * des opérations pouvant échouer, sans lever d'exception.
 */
export interface ExecutionResult {
    /**
     * Indique si l'opération s'est terminée avec succès.
     */
    success: boolean;

    /**
     * Message d'erreur lisible si l'opération a échoué.
     * Défini uniquement lorsque `success` vaut `false`.
     */
    error?: string;
}

/**
 * Résultat d'une opération d'export de page.
 *
 * Fournit soit les données exportées en cas de succès,
 * soit une description de l'erreur rencontrée.
 */
export interface PageExportResult {
    /**
     * Indique si l'export de la page s'est déroulé correctement.
     */
    success: boolean;

    /**
     * Données de la page exportée.
     */
    pageData?: any;

    /**
     * Message d'erreur en cas d'échec de l'export.
     */
    error?: string;
}

/**
 * Message échangé avec Penpot via le canal de communication intégré.
 *
 * Cette interface représente un message générique pouvant
 * couvrir plusieurs types d'événements ou de commandes.
 */
export interface PenpotMessage {
    /**
     * Type de message ou d'action demandée.
     *
     * Exemples : `EXPORT_PAGE`, `ERROR`, `READY`
     */
    type: string;

    /**
     * Code métier ou technique associé au message.
     *
     * Peut être utilisé pour identifier une erreur,
     * une action ou un statut spécifique.
     */
    code?: string;

    /**
     * Identifiant de la page concernée par le message.
     *
     * Présent uniquement pour les messages liés à une page.
     */
    pageId?: string;
}