import { CodeGenerationRequest, CodeGenerationResponse } from "../types";

export interface IApiService {
    generateCode(request: CodeGenerationRequest): Promise<CodeGenerationResponse>;
    exportPageContext(fileId: string): Promise<any>;
}

export class ApiService implements IApiService {
    constructor(private baseUrl: string) {}

    async generateCode(request: CodeGenerationRequest): Promise<CodeGenerationResponse> {
        const response = await fetch(`${this.baseUrl}/generate`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
            },
            body: JSON.stringify(request)
        });

        if (!response.ok) {
            const errorData = await response.json().catch(() => null);
            throw new Error(errorData?.message || `Erreur HTTP: ${response.status}`);
        }

        return await response.json();
    }

    async exportPageContext(fileId: string): Promise<any> {
        const response = await fetch('/api/rpc/command/get-file', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
            },
            credentials: 'include',
            body: JSON.stringify({ id: fileId })
        });

        if (!response.ok) {
            throw new Error(`Erreur HTTP: ${response.status}`);
        }

        return await response.json();
    }
}