/**
 * Represents a task received from the MCP server in the Penpot MCP plugin.
 * 
 * This class encapsulates a task request and provides methods to send
 * responses back to the server. It ensures that only one response is sent
 * per task to prevent duplicate responses.
 * 
 * @template TParams - The type of parameters this task expects
 * 
 * @example
 * ```typescript
 * const task = new Task<ExecuteCodeTaskParams>(
 *   "task-123",
 *   "executeCode",
 *   { code: "return 42;" }
 * );
 * 
 * // Execute and send success response
 * const result = eval(task.params.code);
 * task.sendSuccess({ result, log: "" });
 * ```
 */
export class Task<TParams = any> {
    /**
     * Flag indicating whether a response has already been sent for this task.
     * Prevents duplicate responses which could cause errors in the server.
     */
    public isResponseSent: boolean = false;

    /**
     * Creates a new Task instance.
     * 
     * @param requestId - Unique identifier for the task request
     * @param taskType - The type of the task to execute (e.g., "executeCode")
     * @param params - Task parameters/arguments specific to the task type
     */
    constructor(
        public requestId: string,
        public taskType: string,
        public params: TParams
    ) {}

    /**
     * Sends a task response back to the MCP server.
     * 
     * This is the internal method used by sendSuccess() and sendError().
     * It constructs a properly formatted response message and sends it
     * to the UI layer, which forwards it to the server via WebSocket.
     * 
     * @param success - Whether the task completed successfully
     * @param data - The result data (for successful execution)
     * @param error - The error message (for failed execution)
     * 
     * @throws Will log an error if called more than once for the same task
     */
    protected sendResponse(success: boolean, data: any = undefined, error: any = undefined): void {
        if (this.isResponseSent) {
            console.error("Response already sent for task:", this.requestId);
            return;
        }

        const response = {
            type: "task-response",
            response: {
                id: this.requestId,
                success: success,
                data: data,
                error: error,
            },
        };

        penpot.ui.sendMessage(response);
        console.log("Sent task response:", response);
        this.isResponseSent = true;
    }

    /**
     * Sends a success response with optional result data.
     * 
     * Use this method when the task completes successfully.
     * 
     * @param data - Optional result data to include in the response
     * 
     * @example
     * ```typescript
     * const result = { message: "Shape created", id: shape.id };
     * task.sendSuccess(result);
     * ```
     */
    public sendSuccess(data: any = undefined): void {
        this.sendResponse(true, data);
    }

    /**
     * Sends an error response with an error message.
     * 
     * Use this method when the task fails or encounters an error.
     * 
     * @param error - The error message describing what went wrong
     * 
     * @example
     * ```typescript
     * try {
     *   // execute task...
     * } catch (error) {
     *   task.sendError(error.message);
     * }
     * ```
     */
    public sendError(error: string): void {
        this.sendResponse(false, undefined, error);
    }
}

/**
 * Abstract base class for task handlers in the Penpot MCP plugin.
 * 
 * Each task handler is responsible for executing a specific type of task
 * (e.g., executeCode, importImage, exportImage). Handlers follow a common
 * pattern:
 * 
 * 1. Check if they can handle the task via `isApplicableTo()`
 * 2. Execute the task logic in `handle()`
 * 3. Send a response via the Task object
 * 
 * @template TParams - The type of parameters this handler expects
 * 
 * @example
 * ```typescript
 * class MyTaskHandler extends TaskHandler<MyParams> {
 *   readonly taskType = "myTask";
 *   
 *   async handle(task: Task<MyParams>): Promise<void> {
 *     const result = await doSomething(task.params);
 *     task.sendSuccess(result);
 *   }
 * }
 * ```
 */
export abstract class TaskHandler<TParams = any> {
    /**
     * The task type this handler is responsible for.
     * Must match the task type sent in requests from the server.
     * 
     * @example "executeCode", "importImage", "exportImage"
     */
    abstract readonly taskType: string;

    /**
     * Checks if this handler can process the given task.
     * 
     * Default implementation compares the task type, but can be
     * overridden for more complex matching logic.
     * 
     * @param task - The task to check
     * @returns True if this handler applies to the given task
     */
    isApplicableTo(task: Task): boolean {
        return this.taskType === task.taskType;
    }

    /**
     * Handles the task with the provided parameters.
     * 
     * Implementations should:
     * 1. Execute the task logic using task.params
     * 2. Call task.sendSuccess() or task.sendError() with results
     * 3. Handle any errors gracefully
     * 
     * @param task - The task to be handled
     * @throws Should catch and convert errors to task.sendError() calls
     */
    abstract handle(task: Task<TParams>): Promise<void>;
}