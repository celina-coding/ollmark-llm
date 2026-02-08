import { AbstractUIComponent } from './AbstractUIComponent';
import { IChatDisplay } from '../interfaces/IUIComponents';

/**
 * Chat display component (Single Responsibility: Display chat messages)
 */
export class ChatDisplayComponent extends AbstractUIComponent implements IChatDisplay {
    private chatBox: HTMLElement | null = null;
    private inputElement: HTMLInputElement | null = null;
    private sendButton: HTMLButtonElement | null = null;

    constructor(
        chatBoxId: string = 'chatBox',
        private readonly inputId: string = 'chatMsg',
        private readonly sendButtonId: string = 'sendChatBtn'
    ) {
        super(chatBoxId);
    }

    protected setupElement(): void {
        this.chatBox = this.getElement();
        this.inputElement = document.getElementById(this.inputId) as HTMLInputElement | null;
        this.sendButton = document.getElementById(this.sendButtonId) as HTMLButtonElement | null;
    }

    appendMessage(sender: 'user' | 'ai', message: string): void {
        if (!this.chatBox) return;

        const bubble = this.createMessageBubble(sender, message);
        this.chatBox.appendChild(bubble);
        this.chatBox.scrollTop = this.chatBox.scrollHeight;
    }

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

    clear(): void {
        if (this.chatBox) {
            this.chatBox.innerHTML = '';
        }
    }

    setInputEnabled(enabled: boolean): void {
        if (this.inputElement) {
            this.inputElement.disabled = !enabled;
        }
        if (this.sendButton) {
            this.sendButton.disabled = !enabled;
        }
    }

    getInputValue(): string {
        return this.inputElement?.value?.trim() ?? '';
    }

    clearInput(): void {
        if (this.inputElement) {
            this.inputElement.value = '';
        }
    }

    onSendMessage(handler: () => void): void {
        if (this.sendButton) {
            this.sendButton.addEventListener('click', handler);
        }

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