/**
 * Interface de base pour tous les composants UI.
 * 
 * **Responsabilités communes** :
 * - Initialisation avec accès au DOM
 * - Nettoyage des ressources
 * - Identification par elementId
 */
export interface IUIComponent {
    /** ID de l'élément DOM géré par ce composant */
    readonly elementId: string;

    /** Initialise le composant et configure le DOM */
    initialize(): void;

    /** Nettoie les ressources et event listeners */
    destroy(): void;
}

/**
 * Interface pour les composants d'affichage de statut.
 * 
 * @example
 * ```typescript
 * const statusDisplay: IStatusDisplay = new WebSocketStatusComponent();
 * statusDisplay.setConnected(true); // Affiche "connecté"
 * ```
 */
export interface IStatusDisplay {
    /**
     * Met à jour l'affichage du statut de connexion.
     * @param connected - État de connexion à afficher
     */
    setConnected(connected: boolean): void;
}

/**
 * Interface pour l'affichage du chat.
 * 
 * @example
 * ```typescript
 * const chat: IChatDisplay = new ChatDisplayComponent();
 * chat.appendMessage('user', 'Bonjour !');
 * chat.appendMessage('ai', 'Bonjour ! Comment puis-je vous aider ?');
 * chat.setInputEnabled(false); // Désactive pendant traitement
 * ```
 */
export interface IChatDisplay {
    /**
     * Ajoute un message à la conversation.
     * @param sender - Émetteur du message ('user' ou 'ai')
     * @param message - Contenu du message
     */
    appendMessage(sender: 'user' | 'ai', message: string): void;

    /**
     * Efface tous les messages affichés.
     */
    clear(): void;

    /**
     * Active/désactive les champs de saisie.
     * @param enabled - true pour activer, false pour désactiver
     */
    setInputEnabled(enabled: boolean): void;
}

/**
 * Interface pour la connexion WebSocket.
 * 
 * @example
 * ```typescript
 * const ws: IWebSocketConnection = new WebSocketService('ws://localhost:8080');
 * 
 * ws.onStatusChange((connected) => {
 *   console.log(connected ? 'Online' : 'Offline');
 * });
 * 
 * ws.onMessage((data) => {
 *   console.log('Received:', data);
 * });
 * 
 * ws.connect();
 * ws.send({ type: 'ping' });
 * ```
 */
export interface IWebSocketConnection {
    /** Établit la connexion WebSocket */
    connect(): void;

    /** Ferme la connexion WebSocket */
    disconnect(): void;

    /**
     * Envoie des données via WebSocket.
     * @param data - Données à envoyer (sera JSON.stringify)
     */
    send(data: any): void;

    /**
     * Vérifie si la connexion est active.
     * @returns true si connecté
     */
    isConnected(): boolean;

    /**
     * Enregistre un handler pour les messages entrants.
     * @param handler - Callback appelé à chaque message
     */
    onMessage(handler: (data: any) => void): void;

    /**
     * Enregistre un handler pour les changements d'état.
     * @param handler - Callback appelé à chaque changement
     */
    onStatusChange(handler: (connected: boolean) => void): void;
}

/**
 * Interface pour le service API.
 * 
 * @example
 * ```typescript
 * const api: IApiService = new ApiService('http://localhost:4401');
 * 
 * const { conversationId } = await api.newConversation();
 * const { response } = await api.sendMessage(conversationId, 'Hello');
 * const result = await api.executeCode('return 42;');
 * ```
 */
export interface IApiService {
    /**
     * Crée une nouvelle conversation.
     * @returns Promise avec l'ID de conversation
     */
    newConversation(): Promise<{ conversationId: string }>;

    /**
     * Envoie un message dans une conversation.
     * @param conversationId - ID de la conversation
     * @param message - Message à envoyer
     * @returns Promise avec la réponse de l'IA
     */
    sendMessage(conversationId: string, message: string): Promise<{ response: string }>;

    /**
     * Exécute du code JavaScript.
     * @param code - Code à exécuter
     * @returns Promise avec résultat, logs et statut
     */
    executeCode(code: string): Promise<{ success: boolean; logs?: string; result?: any }>;
}

/**
 * Interface pour la gestion des conversations.
 * 
 * @example
 * ```typescript
 * const manager: IConversationManager = new ConversationManager(api, chat);
 * 
 * await manager.createNew();
 * 
 * if (manager.isReady()) {
 *   await manager.sendMessage('Hello');
 *   console.log('ID:', manager.getCurrentId());
 * }
 * 
 * manager.reset();
 * ```
 */
export interface IConversationManager {
    /**
     * Crée une nouvelle conversation.
     * Réinitialise l'état précédent automatiquement.
     */
    createNew(): Promise<void>;

    /**
     * Réinitialise complètement l'état.
     * Efface la conversation courante et l'affichage.
     */
    reset(): void;

    /**
     * Envoie un message dans la conversation courante.
     * @param message - Message utilisateur
     */
    sendMessage(message: string): Promise<void>;

    /**
     * Récupère l'ID de la conversation courante.
     * @returns ID ou null si aucune conversation
     */
    getCurrentId(): string | null;

    /**
     * Vérifie si prêt à envoyer des messages.
     * @returns true si conversation active
     */
    isReady(): boolean;
}