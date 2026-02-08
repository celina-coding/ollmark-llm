/**
 * Interface principale pour tous les gestionnaires de tâches.
 * 
 * @template TParams - Type des paramètres d'entrée de la tâche
 * @template TResult - Type du résultat de sortie de la tâche
 * 
 * @example
 * // Implémentation concrète
 * class ImageHandler implements ITaskHandler<ImageParams, ImageResult> {
 *   readonly taskType = "generateImage";
 *   canHandle(type: string) { return type === this.taskType; }
 *   async handle(params: ImageParams): Promise<ImageResult> { ... }
 * }
 */
export interface ITaskHandler<TParams = any, TResult = any> {
    /**
     * Identifiant unique du type de tâche.
     * Utilisé pour le routing des requêtes.
     */
    readonly taskType: string;

    /**
     * Détermine si ce handler peut traiter un type de tâche donné.
     * 
     * @param taskType - Type de tâche à vérifier
     * @returns true si compatible
     */
    canHandle(taskType: string): boolean;

    /**
     * Traite une tâche avec les paramètres fournis.
     * 
     * @param params - Paramètres de la tâche
     * @returns Promise du résultat
     * @throws Error en cas d'échec
     */
    handle(params: TParams): Promise<TResult>;
}

/**
 * Interface pour le contexte d'exécution d'une tâche.
 * 
 * Fournit les métadonnées et les méthodes pour envoyer des réponses.
 * 
 * @example
 * function myTask(context: ITaskContext) {
 *   console.log(`Processing ${context.taskType} - ${context.requestId}`);
 *   context.sendSuccess({ done: true });
 * }
 */
export interface ITaskContext {
    /** Identifiant unique de la requête */
    readonly requestId: string;

    /** Type de tâche en cours d'exécution */
    readonly taskType: string;

    /**
     * Envoie une réponse de succès.
     * @param data - Données de résultat (optionnel)
     */
    sendSuccess(data?: any): void;

    /**
     * Envoie une réponse d'erreur.
     * @param error - Message d'erreur
     */
    sendError(error: string): void;
}

/**
 * Interface pour la stratégie d'envoi de réponse.
 * 
 * Permet de changer la méthode d'envoi sans modifier le code métier.
 * 
 * @example
 * // Implémentation WebSocket
 * class WSResponseSender implements IResponseSender {
 *   send(response: any) { this.ws.send(JSON.stringify(response)); }
 * }
 * 
 * // Implémentation HTTP
 * class HttpResponseSender implements IResponseSender {
 *   send(response: any) { this.httpClient.post('/response', response); }
 * }
 */
export interface IResponseSender {
    /**
     * Envoie une réponse au client.
     * 
     * @param response - Objet de réponse à envoyer
     */
    send(response: any): void;
}