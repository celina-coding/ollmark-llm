import { ITaskContext, IResponseSender } from '../interfaces/ITaskHandler';

/**
 * Task execution context following Single Responsibility Principle
 * Responsibilities:
 * - Store task metadata
 * - Send responses through the configured sender
 * - Prevent duplicate responses
 */
export class TaskContext implements ITaskContext {
    private _isResponseSent: boolean = false;

    constructor(
        public readonly requestId: string,
        public readonly taskType: string,
        private readonly responseSender: IResponseSender
    ) {}

    /**
     * Template Method pattern: Common response sending logic
     */
    private sendResponse(success: boolean, data?: any, error?: string): void {
        if (this._isResponseSent) {
            console.error(`Response already sent for task: ${this.requestId}`);
            return;
        }

        this.responseSender.send({
            id: this.requestId,
            success,
            data,
            error
        });

        this._isResponseSent = true;
    }

    sendSuccess(data?: any): void {
        this.sendResponse(true, data);
    }

    sendError(error: string): void {
        this.sendResponse(false, undefined, error);
    }

    get isResponseSent(): boolean {
        return this._isResponseSent;
    }
}