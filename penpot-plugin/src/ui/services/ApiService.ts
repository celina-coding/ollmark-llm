import { IApiService } from '../interfaces/IUIComponents';

/**
 * API service (Single Responsibility: Handle HTTP API calls)
 */
export class ApiService implements IApiService {
    constructor(
        private readonly baseUrl: string,
        private readonly logger?: { log: (msg: string) => void }
    ) {}

    async newConversation(): Promise<{ conversationId: string }> {
        this.log('Création nouvelle conversation...');

        const data = await this.postJson<{ conversationId?: string }>(
            `${this.baseUrl}/ai/chat/new`,
            {}
        );

        if (!data.conversationId) {
            throw new Error('Aucun conversationId dans la réponse');
        }

        this.log(`✓ Conversation créée: ${data.conversationId}`);
        return { conversationId: data.conversationId };
    }

    async sendMessage(conversationId: string, message: string): Promise<{ response: string }> {
        this.log(`Envoi message (${message.length} chars)...`);

        const data = await this.postJson<{ response?: string }>(
            `${this.baseUrl}/ai/chat`,
            { conversationId, message }
        );

        this.log('✓ Réponse reçue');
        return { response: data.response ?? '' };
    }

    async executeCode(code: string): Promise<{ success: boolean; logs?: string; result?: any }> {
        this.log(`Exécution JavaScript (${code.length} chars)...`);

        const data = await this.postJson<{ success?: boolean; logs?: string; result?: any }>(
            `${this.baseUrl}/ai/execute-code`,
            { code }
        );

        this.log(`✓ Exécution terminée (success=${data.success})`);
        return {
            success: data.success ?? false,
            logs: data.logs,
            result: data.result
        };
    }

    private async postJson<T>(url: string, body: any): Promise<T> {
        const response = await fetch(url, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(body)
        });

        const text = await response.text();
        let data: any = {};

        try {
            data = text ? JSON.parse(text) : {};
        } catch (e) {
            this.log(`Erreur parsing JSON: ${e}`);
        }

        if (!response.ok) {
            throw new Error(data?.error || `${response.status} ${response.statusText}`);
        }

        return data as T;
    }

    private log(message: string): void {
        if (this.logger) {
            this.logger.log(message);
        }
    }
}