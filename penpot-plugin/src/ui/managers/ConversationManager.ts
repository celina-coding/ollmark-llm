import { IConversationManager, IApiService, IChatDisplay } from '../interfaces/IUIComponents';

/**
 * Conversation manager (Single Responsibility: Manage conversation state and flow)
 */
export class ConversationManager implements IConversationManager {
    private currentConversationId: string | null = null;
    private ready: boolean = false;

    constructor(
        private readonly apiService: IApiService,
        private readonly chatDisplay: IChatDisplay,
        private readonly logger?: { log: (msg: string) => void }
    ) {}

    async createNew(): Promise<void> {
        this.reset();
        
        try {
            const { conversationId } = await this.apiService.newConversation();

            this.currentConversationId = conversationId;
            this.ready = true;

            this.chatDisplay.setInputEnabled(true);
            this.chatDisplay.appendMessage('ai', `Conversation prête. ID: ${conversationId.substring(0, 8)}...`);

            this.log(`Conversation ID: ${conversationId}`);
        } catch (error: any) {
            const message = error?.message || String(error);
            this.log(`✗ Erreur: ${message}`);
            this.chatDisplay.appendMessage('ai', `ERREUR: ${message}`);
        }
    }

    reset(): void {
        this.currentConversationId = null;
        this.ready = false;
        this.chatDisplay.clear();
        this.chatDisplay.setInputEnabled(false);
        this.log('Conversation réinitialisée');
    }

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

    getCurrentId(): string | null {
        return this.currentConversationId;
    }

    isReady(): boolean {
        return this.ready;
    }

    private log(message: string): void {
        if (this.logger) {
            this.logger.log(message);
        }
    }
}