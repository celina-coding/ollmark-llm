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
 * 
 * @example
 * ```typescript
 * // HTML correspondant
 * <div id="wsStatusDot" class="status-dot"></div>
 * <button id="connectBtn">Se connecter au serveur</button>
 * 
 * // Initialisation
 * const statusDisplay = new WebSocketStatusComponent('wsStatusDot', 'connectBtn');
 * statusDisplay.initialize();
 * 
 * // Configuration du handler de connexion
 * statusDisplay.onConnectClick(() => {
 *   if (wsService.isConnected()) {
 *     wsService.disconnect();
 *   } else {
 *     wsService.connect();
 *   }
 * });
 * 
 * // Mise à jour de l'état
 * statusDisplay.setConnected(true);  // Affiche "connecté"
 * statusDisplay.setConnected(false); // Affiche "déconnecté"
 * ```
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
        this.statusDot = this.getElement();
        this.connectButton = document.getElementById(this.connectButtonId) as HTMLButtonElement | null;
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
     * 
     * @example
     * ```typescript
     * // Via l'orchestrateur
     * wsService.onStatusChange((connected) => {
     *   statusDisplay.setConnected(connected);
     * });
     * 
     * // Manuel
     * statusDisplay.setConnected(true);
     * // → Dot devient vert, bouton "Connecté au serveur" (disabled)
     * 
     * statusDisplay.setConnected(false);
     * // → Dot devient rouge, bouton "Se connecter au serveur" (enabled)
     * ```
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
     * **Modifications accessibilité** :
     * - Met à jour l'attribut aria-label
     * 
     * **Styles CSS typiques** :
     * ```css
     * .status-dot {
     *   width: 12px;
     *   height: 12px;
     *   border-radius: 50%;
     *   transition: background-color 0.3s;
     * }
     * 
     * .status-dot.connected {
     *   background-color: #4caf50; // Vert
     *   box-shadow: 0 0 8px #4caf50;
     * }
     * 
     * .status-dot.disconnected {
     *   background-color: #9e9e9e; // Gris
     * }
     * ```
     * 
     * @param connected - État de connexion
     * 
     * @private
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
     * **Note** : Le bouton pourrait aussi permettre la déconnexion
     * quand connecté (selon les besoins UX).
     * 
     * @param connected - État de connexion
     * 
     * @private
     * 
     * @example
     * ```typescript
     * // État connecté
     * updateConnectButton(true);
     * // → textContent: "Connecté au serveur"
     * // → disabled: true
     * 
     * // État déconnecté
     * updateConnectButton(false);
     * // → textContent: "Se connecter au serveur"
     * // → disabled: false
     * ```
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
     * 
     * @example
     * ```typescript
     * statusDisplay.onConnectClick(() => {
     *   console.log('User clicked connect button');
     *   
     *   if (wsService.isConnected()) {
     *     console.log('Disconnecting...');
     *     wsService.disconnect();
     *   } else {
     *     console.log('Connecting...');
     *     wsService.connect();
     *   }
     * });
     * ```
     * 
     * @example
     * ```typescript
     * // Avec confirmation
     * statusDisplay.onConnectClick(() => {
     *   if (wsService.isConnected()) {
     *     if (confirm('Déconnecter du serveur ?')) {
     *       wsService.disconnect();
     *     }
     *   } else {
     *     wsService.connect();
     *   }
     * });
     * ```
     */
    onConnectClick(handler: () => void): void {
        if (this.connectButton) this.connectButton.addEventListener('click', handler);
    }
}