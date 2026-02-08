import { ITaskContext } from '../interfaces/ITaskHandler';
import { TaskHandlerRegistry } from '../registry/TaskHandlerRegistry';
import { PluginTaskRequest } from '../../common/types';

/**
 * Encapsule une requête d'exécution de tâche.
 * 
 * Regroupe la requête et son contexte d'exécution pour
 * permettre une manipulation uniforme des commandes.
 * 
 * @example
 * const command = new TaskExecutionCommand(request, context);
 * await executor.execute(command);
 */
export class TaskExecutionCommand {
    constructor(
        public readonly request: PluginTaskRequest,
        public readonly context: ITaskContext
    ) {}
}

/**
 * Orchestration de l'exécution des tâches.
 * 
 * Flux d'exécution :
 * 1. Récupère le handler approprié depuis le registry
 * 2. Valide que le handler existe
 * 3. Délègue l'exécution au handler
 * 4. Gère les erreurs et envoie les réponses
 * 
 * @example
 * const executor = new TaskExecutor(registry);
 * const command = new TaskExecutionCommand(request, context);
 * await executor.execute(command);
 */
export class TaskExecutor {
    /**
     * @param registry - Registry contenant tous les handlers disponibles
     */
    constructor(private readonly registry: TaskHandlerRegistry) {}

    /**
     * Exécute une commande de tâche.
     * 
     * Gestion d'erreurs robuste :
     * - Vérifie l'existence du handler
     * - Capture toutes les exceptions
     * - N'envoie de réponse que si nécessaire (évite les doublons)
     * 
     * @param command - Commande contenant la requête et le contexte
     * 
     * @throws Ne lance jamais d'exception - toutes les erreurs sont 
     *         envoyées via context.sendError()
     * 
     * @example
     * try {
     *   await executor.execute(command);
     * } catch (error) {
     *   // N'arrivera jamais - les erreurs sont gérées en interne
     * }
     */
    async execute(command: TaskExecutionCommand): Promise<void> {
        const { request, context } = command;

        console.log(`[TaskExecutor] Executing task: ${request.task}`);

        // Étape 1 : Recherche du handler
        const handler = this.registry.getHandler(request.task);

        if (!handler) {
            const availableTypes = this.registry.getRegisteredTypes();
            const errorMsg = `Unknown task type: ${request.task}. Available types: ${availableTypes.join(', ')}`;
            console.error(`[TaskExecutor] ${errorMsg}`);
            context.sendError(errorMsg);
            return;
        }

        try {
            console.log(`[TaskExecutor] Processing with handler: ${handler.taskType}`);

            // Étape 2 : Exécution du handler
            const result = await handler.handle(request.params);

            // Étape 3 : Envoi du résultat (seulement si pas déjà envoyé)
            if (!context.isResponseSent) context.sendSuccess(result);

            console.log(`[TaskExecutor] Task completed: ${context.requestId}`);
        } catch (error) {
            const errorMessage = error instanceof Error ? error.message : 'Unknown error';
            console.error(`[TaskExecutor] Task execution error:`, error);

            if (!context.isResponseSent) {
                context.sendError(`Execution error: ${errorMessage}`);
            }
        }
    }
}