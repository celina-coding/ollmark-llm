/**
 * Core interface for task handling (Dependency Inversion Principle)
 * All task handlers must implement this interface
 */
export interface ITaskHandler<TParams = any, TResult = any> {
    readonly taskType: string;
    canHandle(taskType: string): boolean;
    handle(params: TParams): Promise<TResult>;
}

/**
 * Interface for task execution context
 */
export interface ITaskContext {
    readonly requestId: string;
    readonly taskType: string;
    sendSuccess(data?: any): void;
    sendError(error: string): void;
}

/**
 * Interface for response sending (Strategy pattern)
 */
export interface IResponseSender {
    send(response: any): void;
}