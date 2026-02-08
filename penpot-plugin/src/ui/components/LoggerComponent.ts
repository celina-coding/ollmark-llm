import { AbstractUIComponent } from './AbstractUIComponent';
import { ILogger } from '../interfaces/IUIComponents';

/**
 * Logger component (Single Responsibility: Display logs)
 */
export class LoggerComponent extends AbstractUIComponent implements ILogger {
    private logElement: HTMLPreElement | null = null;

    constructor(elementId: string = 'logs') {
        super(elementId);
    }

    protected setupElement(): void {
        this.logElement = this.getElement<HTMLPreElement>();
    }

    log(message: string): void {
        if (!this.logElement) return;

        const timestamp = new Date().toLocaleTimeString();
        this.logElement.textContent += `[${timestamp}] ${message}\n`;
        this.logElement.scrollTop = this.logElement.scrollHeight;
    }

    clear(): void {
        if (this.logElement) {
            this.logElement.textContent = '';
        }
    }
}