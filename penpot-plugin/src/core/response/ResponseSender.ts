import { IResponseSender } from '../interfaces/ITaskHandler';
import { PluginTaskResponse } from '../../common/types';

/**
 * Classe abstraite de base pour les stratégies d'envoi de réponses.
 * 
 * @abstract
 * @implements {IResponseSender}
 * 
 * @example
 * ```typescript
 * // Créer une stratégie personnalisée
 * class WebSocketResponseSender extends BaseResponseSender {
 *   constructor(private ws: WebSocket) {
 *     super();
 *   }
 *   
 *   send(response: any): void {
 *     this.ws.send(JSON.stringify(response));
 *   }
 * }
 * 
 * // Utilisation
 * const sender = new WebSocketResponseSender(myWebSocket);
 * sender.send({ id: 'req-123', success: true, data: 'done' });
 * ```
 */
export abstract class BaseResponseSender implements IResponseSender {
    /**
     * Envoie une réponse au destinataire approprié.
     * 
     * **Doit être implémenté** par toutes les sous-classes concrètes.
     * 
     * @param response - Réponse à envoyer
     * @abstract
     */
    abstract send(response: any): void;
}

/**
 * Stratégie concrète pour l'envoi de réponses via l'UI Penpot.
 * 
 * **Mécanisme** :
 * - Utilise l'API `penpot.ui.sendMessage()` 
 * - Encapsule la réponse dans un message typé
 * - Log l'opération pour debugging
 * 
 * **Format du message** :
 * ```json
 * {
 *   "type": "task-response",
 *   "response": {
 *     "id": "req-123",
 *     "success": true,
 *     "data": { ... },
 *     "error": null
 *   }
 * }
 * ```
 * 
 * **Usage dans le plugin** :
 * - Créé par le `PluginOrchestrator`
 * - Injecté dans le `TaskContext`
 * - Utilisé automatiquement lors de l'envoi de réponses
 * 
 * @extends BaseResponseSender
 * 
 * @example
 * ```typescript
 * const sender = new PenpotUIResponseSender();
 * 
 * // Réponse de succès
 * sender.send({
 *   id: 'req-123',
 *   success: true,
 *   data: { result: 42, logs: '[LOG] Done\n' }
 * });
 * 
 * // Logs:
 * // Sent task response: {
 * //   type: 'task-response',
 * //   response: { id: 'req-123', success: true, data: { ... } }
 * // }
 * 
 * // Réponse d'erreur
 * sender.send({
 *   id: 'req-456',
 *   success: false,
 *   error: 'Invalid parameters'
 * });
 * ```
 * 
 * @see penpot.ui.sendMessage Documentation API Penpot
 */
export class PenpotUIResponseSender extends BaseResponseSender {
    /**
     * Envoie une réponse de tâche à l'UI Penpot.
     * 
     * **Processus** :
     * 1. Encapsule la réponse dans un message typé
     * 2. Envoie via `penpot.ui.sendMessage()`
     * 3. Log le message envoyé
     * 
     * **Type de message** : `"task-response"`
     * Ce type est intercepté par le `PluginMessageBridge`
     * côté UI pour être redirigé vers le WebSocket.
     * 
     * @param response - Réponse de tâche à envoyer
     * 
     * @example
     * ```typescript
     * const response: PluginTaskResponse<CodeResult> = {
     *   id: 'req-789',
     *   success: true,
     *   data: {
     *     result: { value: 42 },
     *     log: '[LOG] Execution completed\n'
     *   }
     * };
     * 
     * sender.send(response);
     * // Message reçu côté UI pour transfert WebSocket
     * ```
     */
    send(response: PluginTaskResponse<any>): void {
        const message = {
            type: "task-response",
            response
        };
        penpot.ui.sendMessage(message);
        console.log("Sent task response:", message);
    }
}