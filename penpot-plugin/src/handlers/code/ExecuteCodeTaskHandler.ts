import { AbstractTaskHandler } from '../../core/handlers/AbstractTaskHandler';
import { ExecuteCodeTaskParams, ExecuteCodeTaskResultData } from '../../common/types';
import { ConsoleCapture } from '../utils/ConsoleCapture';
import { CodeExecutionEnvironment } from '../utils/CodeExecutionEnvironment';

/**
 * Handler for JavaScript code execution tasks
 * Follows Single Responsibility Principle - only handles code execution logic
 */
export class ExecuteCodeTaskHandler extends AbstractTaskHandler<
    ExecuteCodeTaskParams,
    ExecuteCodeTaskResultData<any>
> {
    readonly taskType = "executeCode";

    private readonly consoleCapture: ConsoleCapture;
    private readonly executionEnvironment: CodeExecutionEnvironment;

    constructor() {
        super();
        this.consoleCapture = new ConsoleCapture();
        this.executionEnvironment = new CodeExecutionEnvironment(this.consoleCapture);
    }

    /**
     * Validate that code parameter is provided
     */
    protected validateParams(params: ExecuteCodeTaskParams): void {
        if (!params || !params.code) {
            throw new Error("executeCode task requires 'code' parameter");
        }
    }

    /**
     * Execute the JavaScript code in isolated environment
     */
    protected async execute(params: ExecuteCodeTaskParams): Promise<ExecuteCodeTaskResultData<any>> {
        console.log("[ExecuteCodeTaskHandler] Starting code execution");
        this.consoleCapture.reset();

        try {
            const result = await this.executionEnvironment.execute(params.code);
            console.log("[ExecuteCodeTaskHandler] Code execution result:", result);

            return {
                result,
                log: this.consoleCapture.getLog()
            };
        } catch (error) {
            console.error("[ExecuteCodeTaskHandler] Execution error:", error);
            throw error;
        }
    }
}