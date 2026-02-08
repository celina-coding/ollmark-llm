import { IWebSocketConnection } from '../interfaces/IUIComponents';

/**
 * Plugin message bridge (Single Responsibility: Bridge messages between plugin and WebSocket)
 * Handles bidirectional communication:
 * - Forward WebSocket messages to Penpot plugin via postMessage
 * - Forward plugin responses back to WebSocket
 */
export class PluginMessageBridge {
    constructor(
        private readonly webSocket: IWebSocketConnection,
        private readonly logger?: { log: (msg: string) => void }
    ) {}

    /**
     * Initialize the message bridge
     */
    initialize(): void {
        this.setupWebSocketToPluginBridge();
        this.setupPluginToWebSocketBridge();
    }

    /**
     * Forward WebSocket messages to Penpot plugin
     */
    private setupWebSocketToPluginBridge(): void {
        this.webSocket.onMessage((data) => {
            try {
                // Forward exact message to Penpot plugin runtime
                window.parent.postMessage(data, '*');
                this.log(`→ Message transféré au plugin (task: ${data.task || 'unknown'})`);
            } catch (error) {
                this.log(`✗ Erreur transfer vers plugin: ${error}`);
            }
        });
    }

    /**
     * Forward plugin responses back to WebSocket
     */
    private setupPluginToWebSocketBridge(): void {
        window.addEventListener('message', (event) => {
            const message = event.data;

            if (!this.isValidMessage(message)) {
                return;
            }

            this.handlePluginMessage(message);
        });
    }

    private isValidMessage(message: any): boolean {
        return message && typeof message === 'object';
    }

    private handlePluginMessage(message: any): void {
        // Handle task responses from plugin
        if (message.type === 'task-response') {
            this.forwardTaskResponse(message);
            return;
        }

        // Handle theme changes
        if (message.type === 'themechange') {
            this.log(`Thème changé: ${message.theme}`);
            return;
        }
    }

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

    private log(message: string): void {
        if (this.logger) {
            this.logger.log(message);
        }
    }
}