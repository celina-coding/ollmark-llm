import { ITaskHandler } from '../interfaces/ITaskHandler';

/**
 * Registry pattern: Manages all available task handlers
 * Follows Open/Closed Principle - open for extension, closed for modification
 */
export class TaskHandlerRegistry {
    private handlers: Map<string, ITaskHandler> = new Map();

    /**
     * Register a new task handler
     */
    register(handler: ITaskHandler): void {
        if (this.handlers.has(handler.taskType)) {
            console.warn(`Handler for task type '${handler.taskType}' already registered. Overwriting.`);
        }
        this.handlers.set(handler.taskType, handler);
        console.log(`Registered handler for task type: ${handler.taskType}`);
    }

    /**
     * Register multiple handlers at once
     */
    registerAll(handlers: ITaskHandler[]): void {
        handlers.forEach(handler => this.register(handler));
    }

    /**
     * Find appropriate handler for a task type
     */
    getHandler(taskType: string): ITaskHandler | undefined {
        return this.handlers.get(taskType);
    }

    /**
     * Check if a handler exists for a task type
     */
    hasHandler(taskType: string): boolean {
        return this.handlers.has(taskType);
    }

    /**
     * Get all registered task types
     */
    getRegisteredTypes(): string[] {
        return Array.from(this.handlers.keys());
    }

    /**
     * Clear all handlers (useful for testing)
     */
    clear(): void {
        this.handlers.clear();
    }
}