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
 * **Structure HTML typique** :
 * ```html
 * <div id="chatBox" class="chat-container">
 *   <div class="bubble me">
 *     <div class="bubble-head">Vous • 14:32</div>
 *     <div class="bubble-body">Bonjour !</div>
 *   </div>
 *   <div class="bubble ai">
 *     <div class="bubble-head">IA • 14:32</div>
 *     <div class="bubble-body">Bonjour ! Comment puis-je vous aider ?</div>
 *   </div>
 * </div>
 * <input id="chatMsg" type="text" placeholder="Votre message...">
 * <button id="sendChatBtn">Envoyer</button>
 * ```
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
 * 
 * @example
 * ```typescript
 * // Initialisation
 * const chat = new ChatDisplayComponent('chatBox', 'chatMsg', 'sendChatBtn');
 * chat.initialize();
 * 
 * // Configuration du handler d'envoi
 * chat.onSendMessage(async () => {
 *   const message = chat.getInputValue();
 *   if (message) {
 *     chat.clearInput();
 *     chat.setInputEnabled(false);
 *     
 *     // Afficher message utilisateur
 *     chat.appendMessage('user', message);
 *     
 *     // Envoyer à l'API
 *     const response = await api.sendMessage(message);
 *     
 *     // Afficher réponse IA
 *     chat.appendMessage('ai', response);
 *     
 *     chat.setInputEnabled(true);
 *   }
 * });
 * 
 * // Effacer la conversation
 * chat.clear();
 * ```
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
     * @protected
     * @override
     */
    protected setupElement(): void {
        this.chatBox = this.getElement();
        this.inputElement = document.getElementById(this.inputId) as HTMLInputElement | null;
        this.sendButton = document.getElementById(this.sendButtonId) as HTMLButtonElement | null;
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
     * 
     * @example
     * ```typescript
     * // Message utilisateur
     * chat.appendMessage('user', 'Quelle est la capitale de la France ?');
     * // → Bulle bleue à droite avec "Vous • 14:32"
     * 
     * // Message IA
     * chat.appendMessage('ai', 'La capitale de la France est Paris.');
     * // → Bulle grise à gauche avec "IA • 14:32"
     * 
     * // Flux de conversation
     * chat.appendMessage('user', 'Bonjour !');
     * chat.appendMessage('ai', 'Bonjour ! Comment puis-je vous aider ?');
     * chat.appendMessage('user', 'Explique-moi les Promises');
     * chat.appendMessage('ai', 'Une Promise est un objet représentant...');
     * ```
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
     * **Styles CSS typiques** :
     * ```css
     * .bubble {
     *   margin: 8px 0;
     *   padding: 8px 12px;
     *   border-radius: 12px;
     *   max-width: 70%;
     * }
     * 
     * .bubble.me {
     *   margin-left: auto;
     *   background: #007bff;
     *   color: white;
     * }
     * 
     * .bubble.ai {
     *   margin-right: auto;
     *   background: #f1f3f4;
     *   color: black;
     * }
     * 
     * .bubble-head {
     *   font-size: 0.75rem;
     *   opacity: 0.7;
     *   margin-bottom: 4px;
     * }
     * 
     * .bubble-body {
     *   white-space: pre-wrap;
     *   word-wrap: break-word;
     * }
     * ```
     * 
     * @param sender - Émetteur ('user' ou 'ai')
     * @param message - Contenu du message
     * @returns Élément DOM de la bulle
     * 
     * @private
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
     * 
     * @example
     * ```typescript
     * // Avant nouvelle conversation
     * chat.clear();
     * chat.appendMessage('ai', 'Nouvelle conversation démarrée.');
     * 
     * // Reset complet
     * conversationManager.reset(); // Appelle chat.clear() en interne
     * ```
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
     * 
     * @example
     * ```typescript
     * // Désactiver pendant traitement
     * chat.setInputEnabled(false);
     * const response = await api.sendMessage(message);
     * chat.setInputEnabled(true);
     * 
     * // Désactiver si pas de conversation
     * if (!conversationManager.isReady()) {
     *   chat.setInputEnabled(false);
     * }
     * ```
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
     * 
     * @example
     * ```typescript
     * const message = chat.getInputValue();
     * if (message) {
     *   console.log(`User typed: "${message}"`);
     * } else {
     *   console.log('Empty message');
     * }
     * ```
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
     * 
     * @example
     * ```typescript
     * // Workflow d'envoi
     * const message = chat.getInputValue();
     * if (message) {
     *   chat.clearInput();  // Efface immédiatement
     *   await sendMessage(message);
     * }
     * ```
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
     * **Note** : Shift+Enter permet les retours à la ligne (non implémenté ici).
     * 
     * @param handler - Fonction appelée lors de l'envoi
     * 
     * @example
     * ```typescript
     * chat.onSendMessage(async () => {
     *   const message = chat.getInputValue();
     *   
     *   if (!message) {
     *     console.log('Empty message ignored');
     *     return;
     *   }
     *   
     *   // Nettoyer et désactiver
     *   chat.clearInput();
     *   chat.setInputEnabled(false);
     *   
     *   // Afficher message utilisateur
     *   chat.appendMessage('user', message);
     *   
     *   try {
     *     // Envoyer et attendre réponse
     *     const response = await conversationManager.sendMessage(message);
     *     chat.appendMessage('ai', response);
     *   } catch (error) {
     *     chat.appendMessage('ai', `Erreur: ${error.message}`);
     *   } finally {
     *     // Réactiver
     *     chat.setInputEnabled(true);
     *   }
     * });
     * ```
     */
    onSendMessage(handler: () => void): void {
        if (this.sendButton) this.sendButton.addEventListener('click', handler);

        if (this.inputElement) {
            this.inputElement.addEventListener('keydown', (e) => {
                if (e.key === 'Enter' && !e.shiftKey) {
                    e.preventDefault();
                    handler();
                }
            });
        }
    }
}