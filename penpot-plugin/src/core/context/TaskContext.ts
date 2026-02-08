import { ITaskContext, IResponseSender } from '../interfaces/ITaskHandler';

/**
 * Contexte d'exécution d'une tâche suivant le principe de responsabilité unique.
 * 
 * Responsabilités :
 * - Stocker les métadonnées de la tâche (ID, type)
 * - Envoyer les réponses via le sender configuré
 * - Empêcher l'envoi de réponses multiples (pattern State)
 * 
 * @example
 * const context = new TaskContext(
 *   "req-123",
 *   "executeCode",
 *   responseSender
 * );
 * 
 * context.sendSuccess({ result: 42 });
 * context.sendSuccess({ result: 43 }); // Ignoré - déjà envoyé
 */
export class TaskContext implements ITaskContext {
    private _isResponseSent: boolean = false;

    /**
     * @param requestId - Identifiant unique de la requête
     * @param taskType - Type de tâche (ex: "executeCode")
     * @param responseSender - Stratégie d'envoi de réponse
     */
    constructor(
        public readonly requestId: string,
        public readonly taskType: string,
        private readonly responseSender: IResponseSender
    ) {}

    /**
     * Méthode template : Logique commune d'envoi de réponse.
     * Garantit qu'une réponse n'est envoyée qu'une seule fois.
     * 
     * @param success - Indique si la tâche a réussi
     * @param data - Données de réponse (optionnel)
     * @param error - Message d'erreur (optionnel)
     * 
     * @private
     */
    private sendResponse(success: boolean, data?: any, error?: string): void {
        if (this._isResponseSent) {
            console.error(`Response already sent for task: ${this.requestId}`);
            return;
        }

        this.responseSender.send({
            id: this.requestId,
            success,
            data,
            error
        });

        this._isResponseSent = true;
    }

    /**
     * Envoie une réponse de succès.
     * 
     * @param data - Données de résultat (optionnel)
     * 
     * @example
     * context.sendSuccess({ result: [1, 2, 3] });
     */
    sendSuccess(data?: any): void {
        this.sendResponse(true, data);
    }

    /**
     * Envoie une réponse d'erreur.
     * 
     * @param error - Message d'erreur descriptif
     * 
     * @example
     * context.sendError("Invalid parameters provided");
     */
    sendError(error: string): void {
        this.sendResponse(false, undefined, error);
    }

    /**
     * Indique si une réponse a déjà été envoyée.
     * Utile pour éviter les doubles envois dans les handlers.
     * 
     * @returns true si une réponse a été envoyée
     */
    get isResponseSent(): boolean {
        return this._isResponseSent;
    }
}