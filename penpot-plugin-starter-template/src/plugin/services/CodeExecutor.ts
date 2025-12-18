import { ExecutionResult } from "../types";

export class CodeExecutor {
    async execute(code: string): Promise<ExecutionResult> {
        try {
            const asyncFunction = new Function('penpot', `
                return (async () => {
                    ${code}
                })();
            `);

            await asyncFunction(penpot);

            return { success: true };
        } catch (error) {
            console.error('Erreur lors de l\'exécution du code:', error);
            return { 
                success: false, 
                error: error instanceof Error ? error.message : 'Erreur inconnue' 
            };
        }
    }
}