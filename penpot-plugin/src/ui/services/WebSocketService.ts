import { IWebSocketConnection } from '../interfaces/IUIComponents';

/**
 * WebSocket service (Single Responsibility: Manage WebSocket connection)
 * Follows Dependency Inversion Principle
 */
export class WebSocketService implements IWebSocketConnection {
    private ws: WebSocket | null = null;
    private messageHandlers: Array<(data: any) => void> = [];
    private statusHandlers: Array<(connected: boolean) => void> = [];

    constructor(
        private readonly url: string,
        private readonly logger?: { log: (msg: string) => void }
    ) {}

    connect(): void {
        if (this.ws && (this.ws.readyState === WebSocket.OPEN || this.ws.readyState === WebSocket.CONNECTING)) {
            this.log('WebSocket déjà ouvert/en connexion');
            return;
        }

        this.log(`Connexion WebSocket: ${this.url}`);
        this.notifyStatusChange(false);

        this.ws = new WebSocket(this.url);

        this.ws.onopen = () => {
            this.log('✓ WebSocket connecté');
            this.notifyStatusChange(true);
        };

        this.ws.onerror = (error) => {
            this.log('✗ Erreur WebSocket');
            console.error('WebSocket error:', error);
            this.notifyStatusChange(false);
        };

        this.ws.onclose = (event) => {
            this.log(`WebSocket fermé (code: ${event.code})`);
            this.notifyStatusChange(false);
        };

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

    disconnect(): void {
        if (this.ws) {
            this.log('Fermeture WebSocket...');
            this.ws.close();
            this.ws = null;
        }
    }

    send(data: any): void {
        if (!this.ws || this.ws.readyState !== WebSocket.OPEN) {
            this.log('✗ WebSocket non ouvert');
            throw new Error('WebSocket not connected');
        }

        const json = JSON.stringify(data);
        this.ws.send(json);
        this.log(`→ Message envoyé (id=${data?.id ?? '?'})`);
    }

    isConnected(): boolean {
        return this.ws !== null && this.ws.readyState === WebSocket.OPEN;
    }

    onMessage(handler: (data: any) => void): void {
        this.messageHandlers.push(handler);
    }

    onStatusChange(handler: (connected: boolean) => void): void {
        this.statusHandlers.push(handler);
    }

    private notifyMessageReceived(data: any): void {
        this.messageHandlers.forEach(handler => handler(data));
    }

    private notifyStatusChange(connected: boolean): void {
        this.statusHandlers.forEach(handler => handler(connected));
    }

    private log(message: string): void {
        if (this.logger) {
            this.logger.log(message);
        }
    }
}