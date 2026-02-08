import { ConsoleCapture } from './ConsoleCapture';
import { PenpotUtils } from '../../utils/PenpotUtils';

/**
 * Execution environment for JavaScript code
 * Single Responsibility: Manage code execution context and safety
 */
export class CodeExecutionEnvironment {
    private readonly context: Record<string, any>;

    constructor(consoleCapture: ConsoleCapture) {
        this.context = {
            penpot: penpot,
            storage: {},
            console: consoleCapture,
            penpotUtils: PenpotUtils
        };
    }

    /**
     * Execute code in the isolated context
     */
    async execute(code: string): Promise<any> {
        const fn = new Function(
            ...Object.keys(this.context),
            `return (async () => { ${code} })();`
        );
        
        return await fn(...Object.values(this.context));
    }

    /**
     * Get current context (for inspection/testing)
     */
    getContext(): Readonly<Record<string, any>> {
        return { ...this.context };
    }

    /**
     * Update storage in context
     */
    updateStorage(storage: Record<string, any>): void {
        this.context.storage = storage;
    }
}