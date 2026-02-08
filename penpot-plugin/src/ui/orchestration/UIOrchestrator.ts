import { LoggerComponent } from '../components/LoggerComponent';
import { WebSocketStatusComponent } from '../components/WebSocketStatusComponent';
import { ChatDisplayComponent } from '../components/ChatDisplayComponent';
import { WebSocketService } from '../services/WebSocketService';
import { ApiService } from '../services/ApiService';
import { ConversationManager } from '../managers/ConversationManager';
import { PluginMessageBridge } from '../bridges/PluginMessageBridge';

/**
 * Orchestrateur de l'interface utilisateur du plugin.
 * 
 * **Cycle de vie** :
 * 1. **Construction** : Instanciation de tous les composants
 * 2. **initialize()** : Configuration et démarrage du système
 * 3. **Opération** : Gestion des événements utilisateur
 * 4. **destroy()** : Nettoyage des ressources
 * 
 * @example
 * ```typescript
 * // Initialisation de l'UI
 * const orchestrator = new UIOrchestrator(
 *   'ws://localhost:4401/plugin',
 *   'http://localhost:4401'
 * );
 * 
 * // Démarrage du système
 * orchestrator.initialize();
 * 
 * // L'UI est maintenant fonctionnelle :
 * // - WebSocket connecté
 * // - Conversation auto-initialisée
 * // - Tous les event handlers en place
 * 
 * // Nettoyage à la fermeture
 * window.addEventListener('beforeunload', () => {
 *   orchestrator.destroy();
 * });
 * ```
 */
export class UIOrchestrator {
    /** Composant d'affichage des logs */
    private readonly logger: LoggerComponent;

    /** Composant d'affichage du statut WebSocket */
    private readonly statusDisplay: WebSocketStatusComponent;

    /** Composant d'affichage du chat */
    private readonly chatDisplay: ChatDisplayComponent;

    /** Service de communication WebSocket */
    private readonly webSocketService: WebSocketService;

    /** Service d'appels API HTTP */
    private readonly apiService: ApiService;

    /** Gestionnaire de conversations */
    private readonly conversationManager: ConversationManager;

    /** Pont de messages plugin↔WebSocket */
    private readonly messageBridge: PluginMessageBridge;

    /**
     * Crée une instance de l'orchestrateur UI.
     * 
     * **URLs de configuration** :
     * - `wsUrl` : WebSocket pour communication temps-réel
     * - `apiBaseUrl` : Base URL pour les appels HTTP API
     * 
     * @param wsUrl - URL du WebSocket (ex: 'ws://localhost:4401/plugin')
     * @param apiBaseUrl - URL de base de l'API (ex: 'http://localhost:4401')
     * 
     * @example
     * ```typescript
     * // Configuration locale
     * const orchestrator = new UIOrchestrator(
     *   'ws://localhost:4401/plugin',
     *   'http://localhost:4401'
     * );
     * 
     * // Configuration production
     * const orchestrator = new UIOrchestrator(
     *   'wss://api.example.com/plugin',
     *   'https://api.example.com'
     * );
     * ```
     */
    constructor(
        private readonly wsUrl: string,
        private readonly apiBaseUrl: string
    ) {
        // Initialisation des composants UI
        this.logger = new LoggerComponent('logs');
        this.statusDisplay = new WebSocketStatusComponent('wsStatusDot', 'connectBtn');
        this.chatDisplay = new ChatDisplayComponent('chatBox', 'chatMsg', 'sendChatBtn');

        // Initialisation des services avec injection du logger
        this.webSocketService = new WebSocketService(wsUrl, this.logger);
        this.apiService = new ApiService(apiBaseUrl, this.logger);

        // Initialisation du manager avec injection de dépendances
        this.conversationManager = new ConversationManager(
            this.apiService,
            this.chatDisplay,
            this.logger
        );

        // Initialisation du bridge
        this.messageBridge = new PluginMessageBridge(this.webSocketService, this.logger);
    }

    /**
     * Initialise et démarre l'ensemble du système UI.
     * 
     * **Séquence d'initialisation** :
     * 1. **initializeComponents()** : Configure les composants DOM
     * 2. **wireEventHandlers()** : Connecte les event handlers
     * 3. **setupStatusMonitoring()** : Configure le monitoring WebSocket
     * 4. **messageBridge.initialize()** : Active le pont de messages
     * 5. **webSocketService.connect()** : Établit la connexion
     * 
     * **Auto-initialisation** :
     * - Connexion WebSocket automatique
     * - Création de conversation lors de la connexion
     * - Activation de l'UI quand prêt
     * 
     * **Doit être appelé une fois** après la construction.
     * 
     * @example
     * ```typescript
     * const orchestrator = new UIOrchestrator(wsUrl, apiUrl);
     * orchestrator.initialize();
     * 
     * // Séquence de logs :
     * // [Logger] Initializing...
     * // [StatusDisplay] Initializing...
     * // [ChatDisplay] Initializing...
     * // [WebSocket] Connexion WebSocket: ws://...
     * // [WebSocket] ✓ WebSocket connecté
     * // [API] Création nouvelle conversation...
     * // [API] ✓ Conversation créée: conv_abc123
     * // [Chat] Conversation prête. ID: conv_abc1...
     * ```
     */
    initialize(): void {
        this.initializeComponents();
        this.wireEventHandlers();
        this.setupStatusMonitoring();
        this.messageBridge.initialize();
        this.webSocketService.connect();
    }

    /**
     * Nettoie toutes les ressources lors de l'arrêt.
     * 
     * **Actions de nettoyage** :
     * 1. Destruction des composants UI
     * 2. Déconnexion WebSocket
     * 3. Libération des event listeners
     * 
     * **Doit être appelé** avant la fermeture de la fenêtre.
     * 
     * @example
     * ```typescript
     * window.addEventListener('beforeunload', () => {
     *   orchestrator.destroy();
     * });
     * ```
     */
    destroy(): void {
        this.logger.destroy();
        this.statusDisplay.destroy();
        this.chatDisplay.destroy();
        this.webSocketService.disconnect();
    }

    /**
     * Initialise tous les composants UI.
     * 
     * **Appelle** la méthode `initialize()` de chaque composant
     * qui configure le DOM et les références d'éléments.
     * 
     * @private
     */
    private initializeComponents(): void {
        this.logger.initialize();
        this.statusDisplay.initialize();
        this.chatDisplay.initialize();
    }

    /**
     * Configure tous les gestionnaires d'événements de l'UI.
     * 
     * **Event handlers configurés** :
     * - Bouton de connexion WebSocket (toggle connect/disconnect)
     * - Envoi de messages chat (Enter ou bouton)
     * - Nouvelle conversation
     * - Réinitialisation conversation
     * - Effacement des logs
     * - Sélection de mode (chat/JavaScript)
     * - Exécution de code JavaScript
     * 
     * @private
     */
    private wireEventHandlers(): void {
        // === WebSocket connection toggle ===
        this.statusDisplay.onConnectClick(() => {
            if (this.webSocketService.isConnected()) {
                this.webSocketService.disconnect();
            } else {
                this.webSocketService.connect();
            }
        });

        // === Chat message sending ===
        this.chatDisplay.onSendMessage(async () => {
            const message = this.chatDisplay.getInputValue();
            if (message) {
                this.chatDisplay.clearInput();
                await this.conversationManager.sendMessage(message);
            }
        });

        // === New conversation button ===
        const newConvBtn = document.getElementById('newConvBtn2');
        newConvBtn?.addEventListener('click', async (e) => {
            e.preventDefault();
            await this.conversationManager.createNew();
        });

        // === Reset conversation button ===
        const deleteConvBtn = document.getElementById('deleteConvBtn2');
        deleteConvBtn?.addEventListener('click', (e) => {
            e.preventDefault();
            this.conversationManager.reset();
        });

        // === Clear logs buttons ===
        const clearLogsBtn = document.getElementById('clearLogsBtn');
        const clearLogsBtn2 = document.getElementById('clearLogsBtn2');
        clearLogsBtn?.addEventListener('click', () => this.logger.clear());
        clearLogsBtn2?.addEventListener('click', () => this.logger.clear());

        // === Additional UI features ===
        this.setupModeSelection();
        this.setupJavaScriptExecution();
    }

    /**
     * Configure la surveillance du statut WebSocket.
     * 
     * **Comportements automatiques** :
     * - **Connexion** → Auto-initialise une conversation
     * - **Déconnexion** → Réinitialise l'état de la conversation
     * 
     * @private
     */
    private setupStatusMonitoring(): void {
        this.webSocketService.onStatusChange((connected) => {
            this.statusDisplay.setConnected(connected);
            if (connected) this.autoInitializeConversation();
            else this.conversationManager.reset();
        });
    }

    /**
     * Initialise automatiquement une conversation lors de la connexion.
     * 
     * **Gestion d'erreurs** :
     * - Capture toute erreur d'initialisation
     * - Log l'erreur pour debugging
     * - N'empêche pas le fonctionnement de l'UI
     * 
     * @private
     */
    private async autoInitializeConversation(): Promise<void> {
        try {
            await this.conversationManager.createNew();
        } catch (error: any) {
            this.logger.log(`✗ Auto-init conversation failed: ${error?.message || String(error)}`);
        }
    }

    /**
     * Configure la sélection de mode (Chat vs JavaScript).
     * 
     * **Modes disponibles** :
     * - `chat` : Interface de conversation (par défaut)
     * - `js` : Éditeur et exécuteur de code JavaScript
     * 
     * **Comportement** :
     * - Affiche/masque les panneaux correspondants
     * - Conserve l'état entre les changements
     * 
     * @private
     */
    private setupModeSelection(): void {
        const modeSelect = document.getElementById('modeSelect') as HTMLSelectElement | null;
        const chatPanel = document.getElementById('chatPanel');
        const jsPanel = document.getElementById('jsPanel');

        modeSelect?.addEventListener('change', () => {
            const mode = (modeSelect.value as 'chat' | 'js') || 'chat';

            if (mode === 'chat') {
                if (chatPanel) chatPanel.style.display = '';
                if (jsPanel) jsPanel.style.display = 'none';
            } else {
                if (chatPanel) chatPanel.style.display = 'none';
                if (jsPanel) jsPanel.style.display = '';
            }
        });
    }

    /**
     * Configure l'exécution de code JavaScript côté serveur.
     * 
     * **Fonctionnalités** :
     * - Éditeur de code (textarea)
     * - Bouton d'exécution
     * - Affichage des résultats et logs
     * 
     * **Validation** :
     * - Ignore le code vide
     * - Gère les erreurs d'exécution
     * - Affiche les logs et résultats séparément
     * 
     * @private
     */
    private setupJavaScriptExecution(): void {
        const runJsBtn = document.getElementById('runJsBtn');
        const jsCodeTextarea = document.getElementById('jsCode') as HTMLTextAreaElement | null;

        runJsBtn?.addEventListener('click', async () => {
            const code = jsCodeTextarea?.value ?? '';

            if (!code.trim()) {
                this.logger.log('Code vide ignoré');
                return;
            }

            try {
                const result = await this.apiService.executeCode(code);

                if (result.logs) this.logger.log(`Logs:\n${result.logs}`);
                if (result.result !== undefined) {
                    this.logger.log(`Résultat: ${JSON.stringify(result.result, null, 2)}`);
                }
            } catch (error: any) {
                this.logger.log(`✗ Erreur: ${error?.message || String(error)}`);
            }
        });
    }
}