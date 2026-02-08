import { TaskHandlerRegistry } from '../core/registry/TaskHandlerRegistry';
import { TaskExecutor } from '../core/executor/TaskExecutor';
import { TaskContext } from '../core/context/TaskContext';
import { PenpotUIResponseSender } from '../core/response/ResponseSender';
import { TaskExecutionCommand } from '../core/executor/TaskExecutor';
import { PluginTaskRequest } from '../common/types';
import { ITaskHandler } from '../core/interfaces/ITaskHandler';

/**
 * Facade pattern: Main orchestrator for the plugin
 * Coordinates all components and provides simple interface
 */
export class PluginOrchestrator {
    private readonly registry: TaskHandlerRegistry;
    private readonly executor: TaskExecutor;
    private readonly responseSender: PenpotUIResponseSender;

    constructor() {
        this.registry = new TaskHandlerRegistry();
        this.executor = new TaskExecutor(this.registry);
        this.responseSender = new PenpotUIResponseSender();
    }

    /**
     * Initialize the plugin with handlers
     */
    initialize(handlers: ITaskHandler[]): void {
        console.log("[PluginOrchestrator] Initializing with handlers:", handlers.map(h => h.taskType));
        this.registry.registerAll(handlers);
    }

    /**
     * Handle incoming task request
     */
    async handleTaskRequest(request: PluginTaskRequest): Promise<void> {
        console.log("[PluginOrchestrator] Received task request:", request.task);

        const context = new TaskContext(
            request.id,
            request.task,
            this.responseSender
        );

        const command = new TaskExecutionCommand(request, context);
        await this.executor.execute(command);
    }

    /**
     * Get information about registered handlers
     */
    getRegisteredHandlers(): string[] {
        return this.registry.getRegisteredTypes();
    }
}