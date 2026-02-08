import { LoggerComponent } from '../components/LoggerComponent';
import { WebSocketStatusComponent } from '../components/WebSocketStatusComponent';
import { ChatDisplayComponent } from '../components/ChatDisplayComponent';
import { WebSocketService } from '../services/WebSocketService';
import { ApiService } from '../services/ApiService';
import { ConversationManager } from '../managers/ConversationManager';
import { PluginMessageBridge } from '../bridges/PluginMessageBridge';

/**
 * UI Orchestrator (Facade pattern)
 * Coordinates all UI components and services
 * Single Responsibility: Compose and wire up the UI system
 */
export class UIOrchestrator {
    // Components
    private readonly logger: LoggerComponent;
    private readonly statusDisplay: WebSocketStatusComponent;
    private readonly chatDisplay: ChatDisplayComponent;

    // Services
    private readonly webSocketService: WebSocketService;
    private readonly apiService: ApiService;

    // Managers
    private readonly conversationManager: ConversationManager;
    private readonly messageBridge: PluginMessageBridge;

    constructor(
        private readonly wsUrl: string,
        private readonly apiBaseUrl: string
    ) {
        // Initialize components
        this.logger = new LoggerComponent('logs');
        this.statusDisplay = new WebSocketStatusComponent('wsStatusDot', 'connectBtn');
        this.chatDisplay = new ChatDisplayComponent('chatBox', 'chatMsg', 'sendChatBtn');

        // Initialize services with logger dependency injection
        this.webSocketService = new WebSocketService(wsUrl, this.logger);
        this.apiService = new ApiService(apiBaseUrl, this.logger);

        // Initialize managers
        this.conversationManager = new ConversationManager(
            this.apiService,
            this.chatDisplay,
            this.logger
        );

        // Initialize bridges
        this.messageBridge = new PluginMessageBridge(this.webSocketService, this.logger);
    }

    /**
     * Initialize the entire UI system
     */
    initialize(): void {
        this.initializeComponents();
        this.wireEventHandlers();
        this.setupStatusMonitoring();
        this.messageBridge.initialize();
        this.webSocketService.connect();
    }

    /**
     * Cleanup on shutdown
     */
    destroy(): void {
        this.logger.destroy();
        this.statusDisplay.destroy();
        this.chatDisplay.destroy();
        this.webSocketService.disconnect();
    }

    private initializeComponents(): void {
        this.logger.initialize();
        this.statusDisplay.initialize();
        this.chatDisplay.initialize();
    }

    private wireEventHandlers(): void {
        // WebSocket connection toggle
        this.statusDisplay.onConnectClick(() => {
            if (this.webSocketService.isConnected()) {
                this.webSocketService.disconnect();
            } else {
                this.webSocketService.connect();
            }
        });

        // Chat message sending
        this.chatDisplay.onSendMessage(async () => {
            const message = this.chatDisplay.getInputValue();
            if (message) {
                this.chatDisplay.clearInput();
                await this.conversationManager.sendMessage(message);
            }
        });

        // New conversation button
        const newConvBtn = document.getElementById('newConvBtn2');
        newConvBtn?.addEventListener('click', async (e) => {
            e.preventDefault();
            await this.conversationManager.createNew();
        });

        // Reset conversation button
        const deleteConvBtn = document.getElementById('deleteConvBtn2');
        deleteConvBtn?.addEventListener('click', (e) => {
            e.preventDefault();
            this.conversationManager.reset();
        });

        // Clear logs buttons
        const clearLogsBtn = document.getElementById('clearLogsBtn');
        const clearLogsBtn2 = document.getElementById('clearLogsBtn2');
        clearLogsBtn?.addEventListener('click', () => this.logger.clear());
        clearLogsBtn2?.addEventListener('click', () => this.logger.clear());

        this.setupModeSelection();
        this.setupJavaScriptExecution();
    }

    private setupStatusMonitoring(): void {
        this.webSocketService.onStatusChange((connected) => {
            this.statusDisplay.setConnected(connected);

            if (connected) {
                // Auto-initialize conversation on connect
                this.autoInitializeConversation();
            } else {
                // Reset conversation on disconnect
                this.conversationManager.reset();
            }
        });
    }

    private async autoInitializeConversation(): Promise<void> {
        try {
            await this.conversationManager.createNew();
        } catch (error: any) {
            this.logger.log(`✗ Auto-init conversation failed: ${error?.message || String(error)}`);
        }
    }

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

                if (result.logs) {
                    this.logger.log(`Logs:\n${result.logs}`);
                }

                if (result.result !== undefined) {
                    this.logger.log(`Résultat: ${JSON.stringify(result.result, null, 2)}`);
                }
            } catch (error: any) {
                this.logger.log(`✗ Erreur: ${error?.message || String(error)}`);
            }
        });
    }
}