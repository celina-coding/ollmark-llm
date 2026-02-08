/**
 * UI Component Interfaces (Dependency Inversion Principle)
 */

/**
 * Base interface for UI components
 */
export interface IUIComponent {
    readonly elementId: string;
    initialize(): void;
    destroy(): void;
}

/**
 * Interface for status display components
 */
export interface IStatusDisplay {
    setConnected(connected: boolean): void;
}

/**
 * Interface for log management
 */
export interface ILogger {
    log(message: string): void;
    clear(): void;
}

/**
 * Interface for chat display
 */
export interface IChatDisplay {
    appendMessage(sender: 'user' | 'ai', message: string): void;
    clear(): void;
    setInputEnabled(enabled: boolean): void;
}

/**
 * Interface for WebSocket connection
 */
export interface IWebSocketConnection {
    connect(): void;
    disconnect(): void;
    send(data: any): void;
    isConnected(): boolean;
    onMessage(handler: (data: any) => void): void;
    onStatusChange(handler: (connected: boolean) => void): void;
}

/**
 * Interface for API service
 */
export interface IApiService {
    newConversation(): Promise<{ conversationId: string }>;
    sendMessage(conversationId: string, message: string): Promise<{ response: string }>;
    executeCode(code: string): Promise<{ success: boolean; logs?: string; result?: any }>;
}

/**
 * Interface for conversation management
 */
export interface IConversationManager {
    createNew(): Promise<void>;
    reset(): void;
    sendMessage(message: string): Promise<void>;
    getCurrentId(): string | null;
    isReady(): boolean;
}