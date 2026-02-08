import { IUIComponent } from '../interfaces/IUIComponents';

/**
 * Abstract base class for UI components (Template Method pattern)
 * Provides common structure while allowing specific implementations
 */
export abstract class AbstractUIComponent implements IUIComponent {
    protected element: HTMLElement | null = null;

    constructor(public readonly elementId: string) {}

    /**
     * Template method: Initialization flow
     */
    initialize(): void {
        this.element = document.getElementById(this.elementId);
        
        if (!this.element) {
            console.warn(`[${this.constructor.name}] Element not found: ${this.elementId}`);
            return;
        }

        this.setupElement();
        this.attachEventListeners();
    }

    /**
     * Template method: Cleanup
     */
    destroy(): void {
        this.removeEventListeners();
        this.element = null;
    }

    /**
     * Hook method: Setup element-specific properties
     */
    protected setupElement(): void {
        // Override in subclasses if needed
    }

    /**
     * Hook method: Attach event listeners
     */
    protected attachEventListeners(): void {
        // Override in subclasses if needed
    }

    /**
     * Hook method: Remove event listeners
     */
    protected removeEventListeners(): void {
        // Override in subclasses if needed
    }

    /**
     * Utility: Safe element access
     */
    protected getElement<T extends HTMLElement>(): T | null {
        return this.element as T | null;
    }
}