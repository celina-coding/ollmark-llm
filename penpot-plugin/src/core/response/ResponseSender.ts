import { IResponseSender } from '../interfaces/ITaskHandler';
import { PluginTaskResponse } from '../../common/types';

/**
 * Strategy pattern: Base implementation for sending responses
 */
export abstract class BaseResponseSender implements IResponseSender {
    abstract send(response: any): void;
}

/**
 * Concrete strategy for sending responses via Penpot UI
 */
export class PenpotUIResponseSender extends BaseResponseSender {
    send(response: PluginTaskResponse<any>): void {
        const message = {
            type: "task-response",
            response
        };
        penpot.ui.sendMessage(message);
        console.log("Sent task response:", message);
    }
}

/**
 * Null Object pattern: For testing or when no actual sending is needed
 */
export class NullResponseSender extends BaseResponseSender {
    send(response: any): void {
        console.log("NullResponseSender: Response not sent (test mode):", response);
    }
}