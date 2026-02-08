import { IWebSocketConnection } from '../interfaces/IUIComponents';

/**
 * Service de gestion de connexion WebSocket.
 * 
 * **Fonctionnalités** :
 * - Connexion/déconnexion WebSocket
 * - Envoi et réception de messages JSON
 * - Notification d'état (connecté/déconnecté)
 * - Pattern Observer pour les messages et changements d'état
 * - Logging optionnel des opérations
 * 
 * @implements {IWebSocketConnection}
 * 
 * @example
 * ```typescript
 * const logger = { log: (msg) => console.log(msg) };
 * const wsService = new WebSocketService('ws://localhost:8080', logger);
 * 
 * // S'abonner aux changements d'état
 * wsService.onStatusChange((connected) => {
 *   console.log(`Status: ${connected ? 'Connected' : 'Disconnected'}`);
 * });
 * 
 * // S'abonner aux messages
 * wsService.onMessage((data) => {
 *   console.log('Received:', data);
 * });
 * 
 * // Se connecter
 * wsService.connect();
 * 
 * // Envoyer un message
 * if (wsService.isConnected()) {
 *   wsService.send({ type: 'ping', timestamp: Date.now() });
 * }
 * ```
 */
export class WebSocketService implements IWebSocketConnection {
    /** Instance WebSocket native, null si déconnecté */
    private ws: WebSocket | null = null;

    /** Handlers pour les messages entrants */
    private messageHandlers: Array<(data: any) => void> = [];

    /** Handlers pour les changements d'état de connexion */
    private statusHandlers: Array<(connected: boolean) => void> = [];

    /**
     * Crée une instance du service WebSocket.
     * 
     * @param url - URL du serveur WebSocket (ex: 'ws://localhost:4401/plugin')
     * @param logger - Logger optionnel pour tracer les opérations
     * 
     * @example
     * ```typescript
     * const wsService = new WebSocketService(
     *   'ws://localhost:4401/plugin',
     *   { log: (msg) => console.log(`[WS] ${msg}`) }
     * );
     * ```
     */
    constructor(
        private readonly url: string,
        private readonly logger?: { log: (msg: string) => void }
    ) {}

    /**
     * Établit la connexion WebSocket au serveur.
     * 
     * **Comportement** :
     * - Vérifie si une connexion existe déjà (évite les doublons)
     * - Crée une nouvelle instance WebSocket
     * - Configure tous les event handlers
     * - Notifie les observers du changement d'état
     * 
     * **États gérés** :
     * - `CONNECTING` : Connexion en cours
     * - `OPEN` : Connexion établie avec succès
     * - `CLOSING` : Fermeture en cours
     * - `CLOSED` : Connexion fermée
     * 
     * **Event handlers configurés** :
     * - `onopen` : Connexion réussie
     * - `onerror` : Erreur de connexion
     * - `onclose` : Connexion fermée
     * - `onmessage` : Message reçu
     * 
     * @fires statusHandlers - Appelé avec `false` au début, `true` quand connecté
     * @fires messageHandlers - Appelé pour chaque message JSON reçu
     * 
     * @example
     * ```typescript
     * wsService.connect();
     * // Logs: "Connexion WebSocket: ws://localhost:4401/plugin"
     * // Puis: "✓ WebSocket connecté"
     * ```
     */
    connect(): void {
        // Protection contre les connexions multiples
        if (this.ws && (this.ws.readyState === WebSocket.OPEN || this.ws.readyState === WebSocket.CONNECTING)) {
            this.log('WebSocket déjà ouvert/en connexion');
            return;
        }

        this.log(`Connexion WebSocket: ${this.url}`);
        this.notifyStatusChange(false);

        // Création de la connexion WebSocket
        this.ws = new WebSocket(this.url);

        /**
         * Handler : Connexion établie avec succès.
         * Notifie tous les observers que la connexion est active.
         */
        this.ws.onopen = () => {
            this.log('✓ WebSocket connecté');
            this.notifyStatusChange(true);
        };

        /**
         * Handler : Erreur de connexion.
         * Log l'erreur et notifie les observers de la déconnexion.
         */
        this.ws.onerror = (error) => {
            this.log('✗ Erreur WebSocket');
            console.error('WebSocket error:', error);
            this.notifyStatusChange(false);
        };

        /**
         * Handler : Connexion fermée.
         * Capture le code de fermeture et notifie les observers.
         */
        this.ws.onclose = (event) => {
            this.log(`WebSocket fermé (code: ${event.code})`);
            this.notifyStatusChange(false);
        };

        /**
         * Handler : Message reçu du serveur.
         * Parse le JSON et notifie tous les message handlers.
         * Les messages non-JSON sont ignorés avec un log.
         */
        this.ws.onmessage = (event) => {
            this.log(`← Message reçu (${event.data.length} chars)`);

            try {
                const data = JSON.parse(event.data);
                this.notifyMessageReceived(data);
            } catch (e) {
                this.log('Message non-JSON ignoré');
            }
        };
    }

    /**
     * Ferme la connexion WebSocket proprement.
     * 
     * **Comportement** :
     * - Appelle `close()` sur l'instance WebSocket
     * - Réinitialise l'instance à null
     * - Les handlers `onclose` seront automatiquement déclenchés
     * 
     * **Note** : Utilise le code de fermeture par défaut (1000 - Normal Closure).
     * 
     * @example
     * ```typescript
     * wsService.disconnect();
     * // Logs: "Fermeture WebSocket..."
     * // Puis: "WebSocket fermé (code: 1000)"
     * ```
     */
    disconnect(): void {
        if (this.ws) {
            this.log('Fermeture WebSocket...');
            this.ws.close();
            this.ws = null;
        }
    }


    /**
     * Envoie des données au serveur via WebSocket.
     * 
     * **Processus** :
     * 1. Vérifie que la connexion est active
     * 2. Sérialise les données en JSON
     * 3. Envoie via WebSocket
     * 4. Log l'opération
     * 
     * @param data - Données à envoyer (sera JSON.stringify)
     * @throws {Error} Si WebSocket n'est pas connecté
     * 
     * @example
     * ```typescript
     * try {
     *   wsService.send({
     *     id: 'req-123',
     *     task: 'executeCode',
     *     params: { code: 'return 42;' }
     *   });
     * } catch (error) {
     *   console.error('Failed to send:', error);
     * }
     * ```
     */
    send(data: any): void {
        if (!this.ws || this.ws.readyState !== WebSocket.OPEN) {
            this.log('✗ WebSocket non ouvert');
            throw new Error('WebSocket not connected');
        }

        const json = JSON.stringify(data);
        this.ws.send(json);
        this.log(`→ Message envoyé (id=${data?.id ?? '?'})`);
    }

    /**
     * Vérifie si la connexion WebSocket est active.
     * 
     * @returns `true` si WebSocket existe et est dans l'état OPEN
     * 
     * @example
     * ```typescript
     * if (wsService.isConnected()) {
     *   wsService.send({ type: 'heartbeat' });
     * } else {
     *   console.log('Not connected, attempting reconnect...');
     *   wsService.connect();
     * }
     * ```
     */
    isConnected(): boolean {
        return this.ws !== null && this.ws.readyState === WebSocket.OPEN;
    }

    /**
     * Enregistre un handler pour les messages entrants.
     * 
     * @param handler - Fonction appelée à chaque message reçu
     * 
     * @example
     * ```typescript
     * // Multiples observers
     * wsService.onMessage((data) => {
     *   console.log('Observer 1:', data);
     * });
     * 
     * wsService.onMessage((data) => {
     *   if (data.type === 'notification') {
     *     showNotification(data.message);
     *   }
     * });
     * ```
     */
    onMessage(handler: (data: any) => void): void {
        this.messageHandlers.push(handler);
    }

    /**
     * Enregistre un handler pour les changements d'état de connexion.
     * 
     * @param handler - Fonction appelée à chaque changement d'état
     * 
     * @example
     * ```typescript
     * wsService.onStatusChange((connected) => {
     *   statusIndicator.className = connected ? 'online' : 'offline';
     *   if (connected) {
     *     initializeSession();
     *   } else {
     *     cleanupSession();
     *   }
     * });
     * ```
     */
    onStatusChange(handler: (connected: boolean) => void): void {
        this.statusHandlers.push(handler);
    }

    /**
     * Notifie tous les message handlers d'un nouveau message.
     * 
     * @param data - Données parsées du message
     * @private
     */
    private notifyMessageReceived(data: any): void {
        this.messageHandlers.forEach(handler => handler(data));
    }

    /**
     * Notifie tous les status handlers d'un changement d'état.
     * 
     * @param connected - Nouvel état de connexion
     * @private
     */
    private notifyStatusChange(connected: boolean): void {
        this.statusHandlers.forEach(handler => handler(connected));
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