import { AbstractUIComponent } from './AbstractUIComponent';
import { IStatusDisplay } from '../interfaces/IUIComponents';

/**
 * WebSocket status display (Single Responsibility: Show connection status)
 */
export class WebSocketStatusComponent extends AbstractUIComponent implements IStatusDisplay {
    private statusDot: HTMLElement | null = null;
    private connectButton: HTMLButtonElement | null = null;

    constructor(
        statusDotId: string = 'wsStatusDot',
        private readonly connectButtonId: string = 'connectBtn'
    ) {
        super(statusDotId);
    }

    protected setupElement(): void {
        this.statusDot = this.getElement();
        this.connectButton = document.getElementById(this.connectButtonId) as HTMLButtonElement | null;
    }

    setConnected(connected: boolean): void {
        this.updateStatusDot(connected);
        this.updateConnectButton(connected);
    }

    private updateStatusDot(connected: boolean): void {
        if (!this.statusDot) return;

        this.statusDot.classList.remove('connected', 'disconnected');
        this.statusDot.classList.add(connected ? 'connected' : 'disconnected');
        this.statusDot.setAttribute(
            'aria-label',
            connected ? 'Statut connecté' : 'Statut déconnecté'
        );
    }

    private updateConnectButton(connected: boolean): void {
        if (!this.connectButton) return;

        this.connectButton.textContent = connected
            ? 'Connecté au serveur'
            : 'Se connecter au serveur';
        this.connectButton.disabled = connected;
    }

    onConnectClick(handler: () => void): void {
        if (this.connectButton) {
            this.connectButton.addEventListener('click', handler);
        }
    }
}