import { AbstractTaskHandler } from '../../core/handlers/AbstractTaskHandler';
import { ExecuteCodeTaskParams, ExecuteCodeTaskResultData } from '../../common/types';
import { ConsoleCapture } from '../utils/ConsoleCapture';
import { CodeExecutionEnvironment } from '../utils/CodeExecutionEnvironment';

/**
 * Handler pour l'exécution de code JavaScript.
 * 
 * Fonctionnalités :
 * - Validation des paramètres de code
 * - Exécution dans un environnement isolé
 * - Capture des logs console
 * - Gestion des erreurs d'exécution
 * 
 * @extends AbstractTaskHandler<ExecuteCodeTaskParams, ExecuteCodeTaskResultData>
 * 
 * @example
 * const handler = new ExecuteCodeTaskHandler();
 * const result = await handler.handle({
 *   code: "const x = 5; return x * 2;"
 * });
 * // result = { result: 10, log: "[LOG] ..." }
 */
export class ExecuteCodeTaskHandler extends AbstractTaskHandler<
    ExecuteCodeTaskParams,
    ExecuteCodeTaskResultData<any>
> {
    /** Type de tâche géré par ce handler */
    readonly taskType = "executeCode";

    /** Service de capture des logs console */
    private readonly consoleCapture: ConsoleCapture;

    /** Environnement d'exécution isolé */
    private readonly executionEnvironment: CodeExecutionEnvironment;

    constructor() {
        super();
        this.consoleCapture = new ConsoleCapture();
        this.executionEnvironment = new CodeExecutionEnvironment(this.consoleCapture);
    }

    /**
     * Valide que le paramètre 'code' est fourni.
     * 
     * @param params - Paramètres de la tâche
     * @throws Error si le code est manquant ou vide
     * 
     * @protected
     * @override
     */
    protected validateParams(params: ExecuteCodeTaskParams): void {
        if (!params || !params.code) {
            throw new Error("executeCode task requires 'code' parameter");
        }
    }

    /**
     * Exécute le code JavaScript dans l'environnement isolé.
     * 
     * Processus :
     * 1. Réinitialisation des logs
     * 2. Exécution du code
     * 3. Capture du résultat et des logs
     * 4. Gestion des erreurs
     * 
     * @param params - Paramètres contenant le code à exécuter
     * @returns Résultat et logs d'exécution
     * @throws Error propagée depuis l'environnement d'exécution
     * 
     * @protected
     * @override
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
            throw error; // Propagée au TaskExecutor
        }
    }
}