export interface IPenpotMessenger {
    requestState(): void;
    executeCode(code: string): void;
    onMessage(callback: (event: MessageEvent) => void): void;
}

export class PenpotMessenger implements IPenpotMessenger {
    private listeners: ((event: MessageEvent) => void)[] = [];

    constructor() {
        window.addEventListener("message", (event) => {
            this.listeners.forEach(listener => listener(event));
        });
    }

    requestState(): void {
        try {
            parent.postMessage({ type: 'request-state' }, '*');
        } catch (e) {
            console.warn('Impossible de demander l\'état au plugin parent', e);
        }
    }

    executeCode(code: string): void {
        parent.postMessage({
            type: 'execute-code',
            code: code
        }, "*");
    }

    onMessage(callback: (event: MessageEvent) => void): void {
        this.listeners.push(callback);
    }
}