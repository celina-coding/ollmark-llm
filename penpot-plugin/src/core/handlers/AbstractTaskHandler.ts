import { ITaskHandler, ITaskContext } from '../interfaces/ITaskHandler';

/**
 * Abstract base class for task handlers (Template Method pattern)
 * Provides common structure while allowing specific implementations
 */
export abstract class AbstractTaskHandler<TParams = any, TResult = any> 
    implements ITaskHandler<TParams, TResult> {
    
    abstract readonly taskType: string;

    /**
     * Template method: Can be overridden for complex matching logic
     */
    canHandle(taskType: string): boolean {
        return this.taskType === taskType;
    }

    /**
     * Template method: Main execution flow
     * Handles validation, execution, and error handling
     */
    async handle(params: TParams): Promise<TResult> {
        // Hook: Pre-execution validation
        this.validateParams(params);

        // Core execution (must be implemented by subclasses)
        return await this.execute(params);
    }

    /**
     * Hook method: Override to add parameter validation
     */
    protected validateParams(params: TParams): void {
        // Default: no validation
    }

    /**
     * Abstract method: Must be implemented by subclasses
     */
    protected abstract execute(params: TParams): Promise<TResult>;
}