import { ConsoleCapture } from './ConsoleCapture';
import { PenpotUtils } from '../../utils/PenpotUtils';

/**
 * Environnement d'exécution pour le code JavaScript.
 * 
 * Responsabilité unique : Gérer le contexte d'exécution et la sécurité.
 * 
 * Fournit :
 * - Accès contrôlé à l'API Penpot
 * - Console personnalisée avec capture
 * - Utilitaires Penpot
 * - Isolation du code utilisateur
 * 
 * @example
 * const env = new CodeExecutionEnvironment(consoleCapture);
 * const result = await env.execute("return penpot.root.children.length");
 */
export class CodeExecutionEnvironment {
    /** Contexte d'exécution avec APIs disponibles */
    private readonly context: Record<string, any>;

    constructor(consoleCapture: ConsoleCapture) {
        this.context = {
            penpot: penpot,              // API Penpot
            storage: {},                 // Stockage temporaire
            console: consoleCapture,     // Console avec capture
            penpotUtils: PenpotUtils     // Utilitaires personnalisés
        };
    }

    /**
     * Exécute du code dans le contexte isolé.
     * 
     * @param code - Code JavaScript à exécuter
     * @returns Résultat de l'exécution
     * @throws Error si le code contient des erreurs
     * 
     * @example
     * await env.execute("console.log('Hello'); return 42;");
     * // Logs: "[LOG] Hello"
     * // Returns: 42
     */
    async execute(code: string): Promise<any> {
        const fn = new Function(
            ...Object.keys(this.context),
            `return (async () => { ${code} })();`
        );
        return await fn(...Object.values(this.context));
    }

    /**
     * Récupère le contexte actuel (pour inspection/test).
     * Retourne une copie en lecture seule.
     */
    getContext(): Readonly<Record<string, any>> {
        return { ...this.context };
    }

    /**
     * Met à jour le stockage dans le contexte.
     * Permet de persister des données entre exécutions.
     */
    updateStorage(storage: Record<string, any>): void {
        this.context.storage = storage;
    }
}