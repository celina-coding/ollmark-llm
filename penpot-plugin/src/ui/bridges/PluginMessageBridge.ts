import { IWebSocketConnection } from '../interfaces/IUIComponents';

/**
 * Pont de communication bidirectionnel entre le plugin Penpot et le WebSocket.
 * 
 * **Architecture de communication** :
 * ```
 * WebSocket Server ←→ UI (iframe) ←→ Plugin Runtime (sandbox)
 *                      ↑         ↑
 *                      └─────────┘
 *                   PluginMessageBridge
 * ```
 * 
 * **Flux de messages** :
 * 
 * **Direction 1 - WebSocket → Plugin** :
 * ```
 * WebSocket → onMessage → window.parent.postMessage → Plugin Runtime
 * ```
 * 
 * **Direction 2 - Plugin → WebSocket** :
 * ```
 * Plugin Runtime → window.postMessage → event listener → WebSocket.send
 * ```
 * 
 * **Types de messages gérés** :
 * - `task-response` : Réponses de tâches du plugin
 * - `themechange` : Changements de thème Penpot
 * - Messages de tâche : Requêtes vers le plugin
 * 
 * @example
 * ```typescript
 * const logger = { log: (msg) => console.log(`[Bridge] ${msg}`) };
 * const bridge = new PluginMessageBridge(webSocketService, logger);
 * 
 * // Initialisation des ponts bidirectionnels
 * bridge.initialize();
 * 
 * // Le bridge transfère automatiquement tous les messages
 * // dans les deux directions
 * ```
 */
export class PluginMessageBridge {
    /**
     * Crée une instance du pont de messages.
     * 
     * @param webSocket - Service WebSocket pour la communication serveur
     * @param logger - Logger optionnel pour tracer les opérations
     * 
     * @example
     * ```typescript
     * const wsService = new WebSocketService('ws://localhost:4401');
     * const bridge = new PluginMessageBridge(
     *   wsService,
     *   { log: (msg) => console.log(`[MessageBridge] ${msg}`) }
     * );
     * ```
     */
    constructor(
        private readonly webSocket: IWebSocketConnection,
        private readonly logger?: { log: (msg: string) => void }
    ) {}

    /**
     * Initialise les ponts de communication bidirectionnels.
     * 
     * **Doit être appelé une fois** après la construction.
     * 
     * **Configuration** :
     * 1. Configure le pont WebSocket → Plugin
     * 2. Configure le pont Plugin → WebSocket
     * 
     * **Event listeners** :
     * - WebSocket.onMessage : Messages serveur → plugin
     * - window.addEventListener('message') : Messages plugin → serveur
     * 
     * @example
     * ```typescript
     * const bridge = new PluginMessageBridge(webSocket, logger);
     * bridge.initialize();
     * 
     * // Le bridge est maintenant actif et transfère tous les messages
     * // dans les deux directions automatiquement
     * ```
     */
    initialize(): void {
        this.setupWebSocketToPluginBridge();
        this.setupPluginToWebSocketBridge();
    }

    /**
     * Configure le transfert des messages WebSocket vers le plugin.
     * 
     * **Direction** : WebSocket Server → UI → Plugin Runtime
     * 
     * **Processus** :
     * 1. S'abonne aux messages WebSocket
     * 2. Transfère chaque message au plugin via `postMessage`
     * 3. Log l'opération
     * 4. Capture les erreurs de transfert
     * 
     * **PostMessage** :
     * - `window.parent` : Référence à l'iframe parent (plugin runtime)
     * - `'*'` : Accepte tout origin (sécurisé par Penpot)
     * 
     * **Format de message** :
     * Le message est transféré tel quel, sans modification.
     * Typiquement :
     * ```json
     * {
     *   "id": "req-123",
     *   "task": "executeCode",
     *   "params": { "code": "..." }
     * }
     * ```
     * 
     * @private
     * 
     * @example
     * ```typescript
     * // Message WebSocket reçu
     * {
     *   id: 'req-abc',
     *   task: 'executeCode',
     *   params: { code: 'return 42;' }
     * }
     * 
     * // Transféré au plugin via postMessage
     * // Log: "→ Message transféré au plugin (task: executeCode)"
     * ```
     */
    private setupWebSocketToPluginBridge(): void {
        this.webSocket.onMessage((data) => {
            try {
                window.parent.postMessage(data, '*');
                this.log(`→ Message transféré au plugin (task: ${data.task || 'unknown'})`);
            } catch (error) {
                this.log(`✗ Erreur transfer vers plugin: ${error}`);
            }
        });
    }

    /**
     * Configure le transfert des messages Plugin vers WebSocket.
     * 
     * **Direction** : Plugin Runtime → UI → WebSocket Server
     * 
     * **Processus** :
     * 1. Écoute les messages `window.postMessage`
     * 2. Valide le format du message
     * 3. Route selon le type de message
     * 4. Transfère vers WebSocket si nécessaire
     * 
     * **Types de messages traités** :
     * - `task-response` : Réponse de tâche → WebSocket
     * - `themechange` : Changement de thème → Log seulement
     * - Autres : Ignorés
     * 
     * **Sécurité** :
     * - Validation que le message est un objet
     * - Vérification de connexion WebSocket avant envoi
     * 
     * @private
     * 
     * @example
     * ```typescript
     * // Message du plugin runtime
     * {
     *   type: 'task-response',
     *   response: {
     *     id: 'req-abc',
     *     success: true,
     *     data: { result: 42 }
     *   }
     * }
     * 
     * // Transféré au WebSocket
     * // Log: "← task-response envoyé (id=req-abc, success=true)"
     * ```
     */
    private setupPluginToWebSocketBridge(): void {
        window.addEventListener('message', (event) => {
            const message = event.data;
            if (!this.isValidMessage(message)) return;
            this.handlePluginMessage(message);
        });
    }

    /**
     * Valide qu'un message est un objet valide.
     * 
     * **Critères de validation** :
     * - Message non null
     * - Message de type 'object'
     * 
     * @param message - Message à valider
     * @returns true si le message est valide
     * 
     * @private
     * 
     * @example
     * ```typescript
     * isValidMessage({ type: 'test' }) // true
     * isValidMessage('string')         // false
     * isValidMessage(null)             // false
     * isValidMessage(undefined)        // false
     * isValidMessage(42)               // false
     * ```
     */
    private isValidMessage(message: any): boolean {
        return message && typeof message === 'object';
    }

    /**
     * Gère un message provenant du plugin runtime.
     * 
     * **Routing des messages** :
     * - `task-response` → `forwardTaskResponse()`
     * - `themechange` → Log uniquement
     * - Autres → Ignorés
     * 
     * @param message - Message du plugin à traiter
     * 
     * @private
     * 
     * @example
     * ```typescript
     * // Message task-response
     * handlePluginMessage({
     *   type: 'task-response',
     *   response: { id: 'req-1', success: true }
     * });
     * // → Appelle forwardTaskResponse()
     * 
     * // Message themechange
     * handlePluginMessage({
     *   type: 'themechange',
     *   theme: 'dark'
     * });
     * // → Log: "Thème changé: dark"
     * ```
     */
    private handlePluginMessage(message: any): void {
        // Handler 1 : Réponses de tâches
        if (message.type === 'task-response') {
            this.forwardTaskResponse(message);
            return;
        }

        // Handler 2 : Changements de thème
        if (message.type === 'themechange') {
            this.log(`Thème changé: ${message.theme}`);
            return;
        }
    }

    /**
     * Transfère une réponse de tâche vers le WebSocket.
     * 
     * **Processus** :
     * 1. Vérifie que WebSocket est connecté
     * 2. Extrait le payload de réponse
     * 3. Envoie via WebSocket
     * 4. Log l'opération
     * 5. Gère les erreurs d'envoi
     * 
     * **Extraction du payload** :
     * Tente d'extraire `message.response`, sinon utilise le message entier.
     * 
     * **Gestion d'erreurs** :
     * - WebSocket déconnecté : Log + return
     * - Erreur d'envoi : Log l'erreur
     * 
     * @param message - Message contenant la réponse de tâche
     * 
     * @private
     * 
     * @example
     * ```typescript
     * // Message reçu du plugin
     * const message = {
     *   type: 'task-response',
     *   response: {
     *     id: 'req-123',
     *     success: true,
     *     data: { result: 42 }
     *   }
     * };
     * 
     * forwardTaskResponse(message);
     * 
     * // WebSocket reçoit :
     * // { id: 'req-123', success: true, data: { result: 42 } }
     * 
     * // Log: "← task-response envoyé (id=req-123, success=true)"
     * ```
     * 
     * @example
     * ```typescript
     * // WebSocket déconnecté
     * forwardTaskResponse(message);
     * // Log: "✗ WebSocket non connecté, impossible d'envoyer task-response"
     * // Aucun envoi effectué
     * ```
     */
    private forwardTaskResponse(message: any): void {
        if (!this.webSocket.isConnected()) {
            this.log('✗ WebSocket non connecté, impossible d\'envoyer task-response');
            return;
        }

        try {
            const payload = message.response ?? message;
            this.webSocket.send(payload);
            this.log(`← task-response envoyé (id=${payload?.id ?? '?'}, success=${payload?.success ?? '?'})`);
        } catch (error: any) {
            this.log(`✗ Erreur envoi task-response: ${error.message}`);
        }
    }

    /**
     * Enregistre un message via le logger optionnel.
     * 
     * @param message - Message à logger
     * @private
     */
    private log(message: string): void {
        if (this.logger) this.logger.log(message);
    }
}