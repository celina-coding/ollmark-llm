import { ExecutionResult } from "../types";

/**
 * Exécute dynamiquement du code utilisateur dans le contexte Penpot.
 */
export class CodeExecutor {
    /**
     * Exécute le code fourni.
     *
     * @param code Code JavaScript asynchrone à exécuter.
     * @returns Résultat d'exécution.
     */
    async execute(code: string): Promise<ExecutionResult> {
        try {
            const asyncFunction = new Function(
                'penpot',
                `return (async () => { ${code} })();`
            );

            await asyncFunction(penpot);
            return { success: true };
        } catch (error) {
            console.error('Erreur lors de l’exécution du code:', error);
            return {
                success: false,
                error: error instanceof Error
                    ? error.message
                    : 'Erreur inconnue'
            };
        }
    }
}