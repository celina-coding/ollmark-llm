/**
 * Abstraction de communication avec le plugin Penpot parent.
 */
export interface IPenpotMessenger {
    /**
     * Demande l'état courant du plugin.
     */
    requestState(): void;

    /**
     * Demande l'exécution d'un code utilisateur.
     *
     * @param code Code à exécuter.
     */
    executeCode(code: string): void;

    /**
     * Enregistre un écouteur de messages entrants.
     *
     * @param callback Fonction appelée à la réception d'un message.
     */
    onMessage(callback: (event: MessageEvent) => void): void;
}

/**
 * Implémentation navigateur de la messagerie Penpot.
 */
export class PenpotMessenger implements IPenpotMessenger {
    private readonly listeners: Array<(event: MessageEvent) => void> = [];

    constructor() {
        window.addEventListener("message", event => {
            this.listeners.forEach(listener => listener(event));
        });
    }

    requestState(): void {
        try {
            parent.postMessage({ type: 'request-state' }, '*');
        } catch (error) {
            console.warn(
                "Impossible de demander l'état au plugin parent",
                error
            );
        }
    }

    executeCode(code: string): void {
        parent.postMessage(
            { type: 'execute-code', code },
            '*'
        );
    }

    onMessage(callback: (event: MessageEvent) => void): void {
        this.listeners.push(callback);
    }
}