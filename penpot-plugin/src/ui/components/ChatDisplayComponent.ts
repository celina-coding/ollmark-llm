import { AbstractUIComponent } from './AbstractUIComponent';
import { IChatDisplay } from '../interfaces/IUIComponents';

/**
 * Composant d'affichage de l'interface de chat.
 * 
 * **Éléments UI gérés** :
 * - **Chat Box** : Conteneur des messages (scroll automatique)
 * - **Input Element** : Champ de saisie du message
 * - **Send Button** : Bouton d'envoi
 * 
 * **Fonctionnalités** :
 * - Affichage de messages utilisateur et IA
 * - Auto-scroll vers le dernier message
 * - Horodatage automatique
 * - Support Enter pour envoyer
 * - Activation/désactivation pendant traitement
 * 
 * @extends AbstractUIComponent
 * @implements {IChatDisplay}
 */
export class ChatDisplayComponent extends AbstractUIComponent implements IChatDisplay {
    /** Conteneur des messages de chat */
    private chatBox: HTMLElement | null = null;

    /** Champ de saisie du message */
    private inputElement: HTMLInputElement | null = null;

    /** Bouton d'envoi du message */
    private sendButton: HTMLButtonElement | null = null;

    /**
     * Crée une instance du composant de chat.
     * 
     * @param chatBoxId - ID du conteneur de messages (défaut: 'chatBox')
     * @param inputId - ID du champ de saisie (défaut: 'chatMsg')
     * @param sendButtonId - ID du bouton d'envoi (défaut: 'sendChatBtn')
     * 
     * @example
     * ```typescript
     * // Avec IDs par défaut
     * const chat = new ChatDisplayComponent();
     * 
     * // Avec IDs personnalisés
     * const chat = new ChatDisplayComponent('myChat', 'myInput', 'myBtn');
     * ```
     */
    constructor(
        chatBoxId: string = 'chatBox',
        private readonly inputId: string = 'chatMsg',
        private readonly sendButtonId: string = 'sendChatBtn'
    ) {
        super(chatBoxId);
    }

    /**
     * Configure les références aux éléments DOM.
     * 
     * @override
     */
    protected setupElement(): void {
        this.chatBox      = this.getElement();
        this.inputElement = this.getDOMElement<HTMLInputElement>(this.inputId);
        this.sendButton   = this.getDOMElement<HTMLButtonElement>(this.sendButtonId);
    }

    /**
     * Ajoute un message à la conversation.
     * 
     * **Processus** :
     * 1. Crée une bulle de message avec horodatage
     * 2. Ajoute au conteneur de chat
     * 3. Scroll automatiquement vers le bas
     * 
     * **Styles de bulle** :
     * - `bubble me` : Message utilisateur (aligné à droite)
     * - `bubble ai` : Message IA (aligné à gauche)
     * 
     * @param sender - Émetteur du message ('user' ou 'ai')
     * @param message - Contenu textuel du message
     */
    appendMessage(sender: 'user' | 'ai', message: string): void {
        if (!this.chatBox) return;
        const bubble = this.createMessageBubble(sender, message);
        this.chatBox.appendChild(bubble);
        this.chatBox.scrollTop = this.chatBox.scrollHeight;
    }

    /**
     * Crée un élément DOM de bulle de message.
     * 
     * **Structure créée** :
     * ```html
     * <div class="bubble me/ai">
     *   <div class="bubble-head">Vous/IA • HH:MM:SS</div>
     *   <div class="bubble-body">Message content</div>
     * </div>
     * ```
     * 
     * @param sender - Émetteur ('user' ou 'ai')
     * @param message - Contenu du message
     * @returns Élément DOM de la bulle
     */
    private createMessageBubble(sender: 'user' | 'ai', message: string): HTMLElement {
        const div = document.createElement('div');
        div.className = `bubble ${sender === 'user' ? 'me' : 'ai'}`;

        const timestamp = new Date().toLocaleTimeString();
        const header = document.createElement('div');
        header.className = 'bubble-head';
        header.textContent = `${sender === 'user' ? 'Vous' : 'IA'} • ${timestamp}`;

        const body = document.createElement('div');
        body.className = 'bubble-body';
        body.textContent = message;

        div.appendChild(header);
        div.appendChild(body);

        return div;
    }

    /**
     * Efface tous les messages de la conversation.
     * 
     * **Usage** :
     * - Nouvelle conversation
     * - Réinitialisation du chat
     * - Nettoyage avant fermeture
     */
    clear(): void {
        if (this.chatBox) this.chatBox.innerHTML = '';
    }

    /**
     * Active ou désactive les contrôles de saisie.
     * 
     * **Usage** :
     * - `false` pendant le traitement d'un message (évite spam)
     * - `true` quand prêt à recevoir input
     * 
     * **Éléments affectés** :
     * - Champ de saisie (input)
     * - Bouton d'envoi
     * 
     * @param enabled - État d'activation
     */
    setInputEnabled(enabled: boolean): void {
        if (this.inputElement) this.inputElement.disabled = !enabled;
        if (this.sendButton) this.sendButton.disabled = !enabled;
    }

    /**
     * Récupère la valeur actuelle du champ de saisie.
     * 
     * **Nettoyage automatique** :
     * - Trim des espaces avant/après
     * - Retourne chaîne vide si null/undefined
     * 
     * @returns Valeur du champ nettoyée
     */
    getInputValue(): string {
        return this.inputElement?.value?.trim() ?? '';
    }

    /**
     * Efface le contenu du champ de saisie.
     * 
     * **Usage** :
     * - Après envoi d'un message
     * - Lors de la réinitialisation
     */
    clearInput(): void {
        if (this.inputElement) this.inputElement.value = '';
    }

    /**
     * Enregistre un handler pour l'envoi de messages.
     * 
     * **Déclencheurs** :
     * - Clic sur le bouton d'envoi
     * - Touche Enter (sans Shift) dans le champ
     * 
     * @param handler - Fonction appelée lors de l'envoi
     */
    onSendMessage(handler: () => void): void {
        this.sendButton?.addEventListener('click', handler);

        this.inputElement?.addEventListener('keydown', (e) => {
            if (e.key === 'Enter' && !e.shiftKey) {
                e.preventDefault();
                handler();
            }
        });
    }
}