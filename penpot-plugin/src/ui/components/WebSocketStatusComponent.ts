import { AbstractUIComponent } from './AbstractUIComponent';
import { IStatusDisplay } from '../interfaces/IUIComponents';

/**
 * Composant d'affichage du statut de connexion WebSocket.
 * 
 * **Éléments UI gérés** :
 * - **Status Dot** : Indicateur visuel (point coloré)
 * - **Connect Button** : Bouton de connexion/déconnexion
 * 
 * **États visuels** :
 * 
 * **Connecté** :
 * - Dot : classe 'connected' (généralement vert)
 * - Button : "Connecté au serveur" (désactivé)
 * - Aria-label : "Statut connecté"
 * 
 * **Déconnecté** :
 * - Dot : classe 'disconnected' (généralement rouge/gris)
 * - Button : "Se connecter au serveur" (activé)
 * - Aria-label : "Statut déconnecté"
 * 
 * @extends AbstractUIComponent
 * @implements {IStatusDisplay}
 */
export class WebSocketStatusComponent extends AbstractUIComponent implements IStatusDisplay {
    /** Élément d'indicateur de statut (point coloré) */
    private statusDot: HTMLElement | null = null;

    /** Bouton de connexion/déconnexion */
    private connectButton: HTMLButtonElement | null = null;

    /**
     * Crée une instance du composant de statut WebSocket.
     * 
     * @param statusDotId - ID de l'élément indicateur (défaut: 'wsStatusDot')
     * @param connectButtonId - ID du bouton de connexion (défaut: 'connectBtn')
     * 
     * @example
     * ```typescript
     * // Avec IDs par défaut
     * const status = new WebSocketStatusComponent();
     * 
     * // Avec IDs personnalisés
     * const status = new WebSocketStatusComponent('myDot', 'myBtn');
     * ```
     */
    constructor(
        statusDotId: string = 'wsStatusDot',
        private readonly connectButtonId: string = 'connectBtn'
    ) {
        super(statusDotId);
    }

    /**
     * Configure les références aux éléments DOM.
     * 
     * Appelé automatiquement par `initialize()` après validation
     * de l'élément principal.
     * 
     * **Récupère** :
     * - `statusDot` : L'élément principal (déjà dans this.element)
     * - `connectButton` : Le bouton de connexion
     * 
     * @protected
     * @override
     */
    protected setupElement(): void {
        this.statusDot     = this.getElement();
        this.connectButton = this.getDOMElement<HTMLButtonElement>(this.connectButtonId);
    }

    /**
     * Met à jour l'affichage selon l'état de connexion.
     * 
     * **Opérations atomiques** :
     * 1. Met à jour l'indicateur visuel
     * 2. Met à jour le bouton de connexion
     * 
     * **Appelé par** :
     * - L'orchestrateur lors des changements d'état WebSocket
     * - Les observers du service WebSocket
     * 
     * @param connected - État de connexion (true = connecté)
     */
    setConnected(connected: boolean): void {
        this.updateStatusDot(connected);
        this.updateConnectButton(connected);
    }

    /**
     * Met à jour l'apparence de l'indicateur de statut.
     * 
     * **Modifications CSS** :
     * - Retire les classes 'connected' et 'disconnected'
     * - Ajoute la classe appropriée selon l'état
     * 
     * @param connected - État de connexion
     */
    private updateStatusDot(connected: boolean): void {
        if (!this.statusDot) return;

        this.statusDot.classList.remove('connected', 'disconnected');
        this.statusDot.classList.add(connected ? 'connected' : 'disconnected');
        this.statusDot.setAttribute(
            'aria-label',
            connected ? 'Statut connecté' : 'Statut déconnecté'
        );
    }

    /**
     * Met à jour le bouton de connexion.
     * 
     * **Modifications** :
     * - **Texte** : Change selon l'état
     * - **Disabled** : Désactive si connecté (pas d'action disponible)
     * 
     * **Logique UX** :
     * - Quand connecté : Bouton informatif, pas d'action
     * - Quand déconnecté : Bouton actif, permet connexion
     * 
     * @param connected - État de connexion
     */
    private updateConnectButton(connected: boolean): void {
        if (!this.connectButton) return;

        this.connectButton.textContent = connected
            ? 'Connecté au serveur'
            : 'Se connecter au serveur';
        this.connectButton.disabled = connected;
    }

    /**
     * Enregistre un handler pour les clics sur le bouton de connexion.
     * 
     * **Usage typique** :
     * - Toggle connexion/déconnexion WebSocket
     * - Afficher un dialogue de configuration
     * - Logger l'action utilisateur
     * 
     * @param handler - Fonction appelée lors du clic
     */
    onConnectClick(handler: () => void): void {
        this.connectButton?.addEventListener('click', handler);
    }
}