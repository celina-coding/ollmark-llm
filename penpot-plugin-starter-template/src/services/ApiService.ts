import { CodeGenerationRequest, CodeGenerationResponse } from "../types";

/**
 * Contrat d'accès aux services backend de génération et d'export.
 */
export interface IApiService {
    /**
     * Déclenche une génération de code.
     *
     * @param request Requête de génération.
     * @returns Réponse complète de génération.
     * @throws Erreur réseau ou HTTP.
     */
    generateCode(
        request: CodeGenerationRequest
    ): Promise<CodeGenerationResponse>;

    /**
     * Exporte le contexte brut d'un fichier Penpot.
     *
     * @param fileId Identifiant du fichier.
     * @returns Contexte de page exporté.
     * @throws Erreur réseau ou HTTP.
     */
    exportPageContext(fileId: string): Promise<unknown>;
}

/**
 * Implémentation HTTP de l'API de génération de code.
 */
export class ApiService implements IApiService {
    constructor(
        /**
         * URL racine de l'API backend.
         */
        private readonly baseUrl: string
    ) {}

    async generateCode(
        request: CodeGenerationRequest
    ): Promise<CodeGenerationResponse> {
        const response = await fetch(`${this.baseUrl}/generate`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(request)
        });

        if (!response.ok) {
            const errorData = await response.json().catch(() => null);
            throw new Error(
                errorData?.message ?? `Erreur HTTP: ${response.status}`
            );
        }

        return response.json();
    }

    async exportPageContext(fileId: string): Promise<unknown> {
        const response = await fetch('/api/rpc/command/get-file', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            credentials: 'include',
            body: JSON.stringify({ id: fileId })
        });

        if (!response.ok) {
            throw new Error(`Erreur HTTP: ${response.status}`);
        }

        return response.json();
    }
}