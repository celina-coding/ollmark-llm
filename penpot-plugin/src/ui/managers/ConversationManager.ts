import { IConversationManager, IApiService, IChatDisplay } from '../interfaces/IUIComponents';

/**
 * Gestionnaire d'état et de flux des conversations.
 * 
 * **États gérés** :
 * - `ready = false` : Pas de conversation active
 * - `ready = true` : Conversation active, peut envoyer des messages
 * 
 * **Responsabilités** :
 * - Créer de nouvelles conversations
 * - Gérer l'ID de conversation courante
 * - Envoyer des messages via l'API
 * - Afficher les messages dans le chat
 * - Gérer les états d'activation/désactivation de l'UI
 * - Logger les opérations
 * 
 * @implements {IConversationManager}
 * 
 * @example
 * ```typescript
 * const manager = new ConversationManager(
 *   apiService,
 *   chatDisplay,
 *   logger
 * );
 * 
 * // Créer une conversation
 * await manager.createNew();
 * 
 * // Envoyer un message
 * if (manager.isReady()) {
 *   await manager.sendMessage('Bonjour !');
 * }
 * 
 * // Réinitialiser
 * manager.reset();
 * ```
 */
export class ConversationManager implements IConversationManager {
    /** ID de la conversation courante, null si aucune conversation */
    private currentConversationId: string | null = null;

    /** Indique si une conversation est prête à recevoir des messages */
    private ready: boolean = false;

    /**
     * Crée une instance du gestionnaire de conversations.
     * 
     * @param apiService - Service API pour communiquer avec le backend
     * @param chatDisplay - Composant d'affichage du chat
     * @param logger - Logger optionnel pour tracer les opérations
     * 
     * @example
     * ```typescript
     * const manager = new ConversationManager(
     *   new ApiService('http://localhost:4401'),
     *   new ChatDisplayComponent('chatBox'),
     *   { log: (msg) => console.log(`[Conv] ${msg}`) }
     * );
     * ```
     */
    constructor(
        private readonly apiService: IApiService,
        private readonly chatDisplay: IChatDisplay,
        private readonly logger?: { log: (msg: string) => void }
    ) {}

    /**
     * Crée une nouvelle conversation.
     * 
     * **Processus** :
     * 1. Réinitialise l'état précédent (appelle `reset()`)
     * 2. Appelle l'API pour créer la conversation
     * 3. Stocke l'ID de conversation
     * 4. Marque comme prêt (`ready = true`)
     * 5. Active les inputs du chat
     * 6. Affiche un message de confirmation
     * 
     * **Gestion d'erreurs** :
     * - Capture toute erreur de l'API
     * - Affiche l'erreur dans le chat
     * - Log l'erreur
     * - Ne marque PAS comme prêt en cas d'erreur
     * 
     * @returns Promise résolue quand la conversation est créée
     * 
     * @example
     * ```typescript
     * try {
     *   await manager.createNew();
     *   console.log('ID:', manager.getCurrentId());
     *   // ID: "conv_abc123def456"
     * } catch (error) {
     *   // Erreur déjà affichée dans le chat
     *   console.error('Failed:', error);
     * }
     * ```
     */
    async createNew(): Promise<void> {
        this.reset();

        try {
            const { conversationId } = await this.apiService.newConversation();

            this.currentConversationId = conversationId;
            this.ready = true;

            this.chatDisplay.setInputEnabled(true);
            this.chatDisplay.appendMessage(
                'ai',
                `Conversation prête. ID: ${conversationId.substring(0, 8)}...`
            );

            this.log(`Conversation ID: ${conversationId}`);
        } catch (error: any) {
            const message = error?.message || String(error);
            this.log(`✗ Erreur: ${message}`);
            this.chatDisplay.appendMessage('ai', `ERREUR: ${message}`);
        }
    }

    /**
     * Réinitialise complètement l'état de la conversation.
     * 
     * **Actions effectuées** :
     * - Efface l'ID de conversation
     * - Marque comme non-prêt
     * - Vide l'affichage du chat
     * - Désactive les inputs du chat
     * - Log l'opération
     * 
     * **Usage** :
     * - Appelé automatiquement par `createNew()`
     * - Peut être appelé manuellement pour nettoyer
     * - Utilisé lors de la déconnexion WebSocket
     * 
     * @example
     * ```typescript
     * // Nettoyage manuel
     * manager.reset();
     * console.log(manager.isReady()); // false
     * console.log(manager.getCurrentId()); // null
     * ```
     */
    reset(): void {
        this.currentConversationId = null;
        this.ready = false;
        this.chatDisplay.clear();
        this.chatDisplay.setInputEnabled(false);
        this.log('Conversation réinitialisée');
    }

    /**
     * Envoie un message dans la conversation courante.
     * 
     * **Préconditions** :
     * - La conversation doit être prête (`ready === true`)
     * - Un ID de conversation doit exister
     * - Le message ne doit pas être vide
     * 
     * **Processus** :
     * 1. Valide les préconditions
     * 2. Affiche le message utilisateur immédiatement
     * 3. Désactive les inputs pendant le traitement
     * 4. Envoie le message à l'API
     * 5. Affiche la réponse de l'IA
     * 6. Réactive les inputs (dans finally)
     * 
     * **Gestion d'erreurs** :
     * - Affiche les erreurs dans le chat
     * - Log les erreurs
     * - Réactive toujours les inputs (finally)
     * 
     * **Pattern UX** :
     * - Feedback immédiat (affichage message utilisateur)
     * - Désactivation pendant traitement (évite spam)
     * - Réactivation garantie (finally)
     * 
     * @param message - Message utilisateur à envoyer
     * @returns Promise résolue quand l'envoi est terminé
     * 
     * @example
     * ```typescript
     * await manager.sendMessage('Explique les Promises');
     * 
     * // Chat affichera :
     * // Vous • 14:32
     * // Explique les Promises
     * //
     * // IA • 14:32
     * // Une Promise est un objet représentant...
     * ```
     */
    async sendMessage(message: string): Promise<void> {
        if (!this.ready || !this.currentConversationId) {
            this.log('✗ Conversation non prête');
            return;
        }

        if (!message.trim()) {
            this.log('Message vide ignoré');
            return;
        }

        this.chatDisplay.appendMessage('user', message);
        this.chatDisplay.setInputEnabled(false);

        try {
            const { response } = await this.apiService.sendMessage(
                this.currentConversationId,
                message
            );

            this.chatDisplay.appendMessage('ai', response || '(réponse vide)');
        } catch (error: any) {
            const errorMessage = error?.message || String(error);
            this.log(`✗ Erreur: ${errorMessage}`);
            this.chatDisplay.appendMessage('ai', `ERREUR: ${errorMessage}`);
        } finally {
            this.chatDisplay.setInputEnabled(true);
        }
    }

    /**
     * Récupère l'ID de la conversation courante.
     * 
     * @returns ID de conversation ou null si aucune conversation
     * 
     * @example
     * ```typescript
     * const id = manager.getCurrentId();
     * if (id) {
     *   console.log('Conversation active:', id);
     * } else {
     *   console.log('Aucune conversation');
     * }
     * ```
     */
    getCurrentId(): string | null {
        return this.currentConversationId;
    }

    /**
     * Vérifie si le manager est prêt à envoyer des messages.
     * 
     * @returns `true` si une conversation est active et prête
     * 
     * @example
     * ```typescript
     * if (manager.isReady()) {
     *   await manager.sendMessage('Hello');
     * } else {
     *   await manager.createNew();
     * }
     * ```
     */
    isReady(): boolean {
        return this.ready;
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