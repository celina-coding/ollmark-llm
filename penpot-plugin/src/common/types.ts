/**
 * Result of a plugin task execution.
 * 
 * Contains the outcome status of a task and any additional result data.
 * This interface is used for type-safe communication between the plugin
 * and the server.
 * 
 * @template T - The type of data returned by the task
 * 
 * @example
 * ```typescript
 * const result: PluginTaskResult<string> = {
 *   data: "Task completed successfully"
 * };
 * ```
 */
export interface PluginTaskResult<T> {
    /**
     * Optional result data from the task execution.
     * Contains the actual output or return value of the executed task.
     */
    data?: T;
}

/**
 * Request message sent from server to plugin via WebSocket.
 * 
 * Contains a unique identifier, task name, and parameters for execution.
 * This message is deserialized from JSON received through the WebSocket
 * connection and forwarded to the appropriate task handler.
 * 
 * @example
 * ```typescript
 * const request: PluginTaskRequest = {
 *   id: "task-123",
 *   task: "executeCode",
 *   params: { code: "console.log('Hello');" }
 * };
 * ```
 */
export interface PluginTaskRequest {
    /**
     * Unique identifier for request/response correlation.
     * Used to match responses with their corresponding requests
     * in asynchronous communication.
     */
    id: string;

    /**
     * The name of the task to execute.
     * Must match one of the registered task handlers in the plugin.
     * 
     * @example "executeCode", "importImage", "exportImage"
     */
    task: string;

    /**
     * The parameters for task execution.
     * Type varies based on the task being executed.
     */
    params: any;
}

/**
 * Response message sent from plugin back to server via WebSocket.
 * 
 * Contains the original request ID and the execution result, allowing
 * the server to correlate the response with the pending request.
 * 
 * @template T - The type of data being returned
 * 
 * @example
 * ```typescript
 * const response: PluginTaskResponse<string> = {
 *   id: "task-123",
 *   success: true,
 *   data: { result: "Execution completed", log: "..." }
 * };
 * ```
 */
export interface PluginTaskResponse<T> {
    /**
     * Unique identifier matching the original request.
     * This ID is used by the server to resolve the CompletableFuture
     * waiting for this response.
     */
    id: string;

    /**
     * Whether the task completed successfully.
     * - `true`: Task executed without errors
     * - `false`: Task failed, check the error field
     */
    success: boolean;

    /**
     * Optional error message if the task failed.
     * Only present when success is false.
     */
    error?: string;

    /**
     * The result of the task execution.
     * Only present when success is true.
     */
    data?: T;
}

/**
 * Parameters for the executeCode task.
 * 
 * Defines the structure of parameters required to execute JavaScript
 * code in the Penpot plugin context.
 */
export interface ExecuteCodeTaskParams {
    /**
     * The JavaScript code to be executed.
     * This code will run in the plugin's execution context with access to:
     * - `penpot`: The Penpot API object
     * - `penpotUtils`: Utility functions from PenpotUtils class
     * - Standard JavaScript globals
     */
    code: string;
}

/**
 * Result data for the executeCode task.
 * 
 * Contains both the execution result and any console output captured
 * during code execution.
 * 
 * @template T - The type of the execution result
 */
export interface ExecuteCodeTaskResultData<T> {
    /**
     * The result of the executed code, if any.
     * This is the value returned by the last expression or return statement
     * in the executed code.
     */
    result: T;

    /**
     * Captured console output during code execution.
     * Includes all console.log, console.warn, console.error calls
     * made during execution, concatenated with newlines.
     */
    log: string;
}