import { ITaskContext } from '../interfaces/ITaskHandler';
import { TaskHandlerRegistry } from '../registry/TaskHandlerRegistry';
import { PluginTaskRequest } from '../../common/types';

/**
 * Command pattern: Encapsulates task execution request
 */
export class TaskExecutionCommand {
    constructor(
        public readonly request: PluginTaskRequest,
        public readonly context: ITaskContext
    ) {}
}

/**
 * Facade pattern: Simplifies task execution orchestration
 * Single Responsibility: Execute tasks using registered handlers
 */
export class TaskExecutor {
    constructor(private readonly registry: TaskHandlerRegistry) {}

    /**
     * Execute a task command
     */
    async execute(command: TaskExecutionCommand): Promise<void> {
        const { request, context } = command;

        console.log(`[TaskExecutor] Executing task: ${request.task}`);

        const handler = this.registry.getHandler(request.task);

        if (!handler) {
            const availableTypes = this.registry.getRegisteredTypes();
            const errorMsg = `Unknown task type: ${request.task}. Available types: ${availableTypes.join(', ')}`;
            console.error(`[TaskExecutor] ${errorMsg}`);
            context.sendError(errorMsg);
            return;
        }

        try {
            console.log(`[TaskExecutor] Processing with handler: ${handler.taskType}`);
            const result = await handler.handle(request.params);

            // Only send success if handler hasn't already sent a response
            if (!context.isResponseSent) {
                context.sendSuccess(result);
            }

            console.log(`[TaskExecutor] Task completed: ${context.requestId}`);
        } catch (error) {
            const errorMessage = error instanceof Error ? error.message : 'Unknown error';
            console.error(`[TaskExecutor] Task execution error:`, error);

            if (!context.isResponseSent) {
                context.sendError(`Execution error: ${errorMessage}`);
            }
        }
    }
}