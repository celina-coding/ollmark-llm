import { TaskHandlerRegistry } from '../core/registry/TaskHandlerRegistry';
import { TaskExecutor } from '../core/executor/TaskExecutor';
import { TaskContext } from '../core/context/TaskContext';
import { PenpotUIResponseSender } from '../core/response/ResponseSender';
import { TaskExecutionCommand } from '../core/executor/TaskExecutor';
import { PluginTaskRequest } from '../common/types';
import { ITaskHandler } from '../core/interfaces/ITaskHandler';

/**
 * Orchestrateur principal du plugin Penpot.
 * 
 * **Flux de traitement** :
 * 1. Réception d'une requête (via `handleTaskRequest`)
 * 2. Création du contexte d'exécution
 * 3. Création de la commande d'exécution
 * 4. Délégation à l'executor
 * 5. Réponse automatique via le sender
 * 
 * @example
 * ```typescript
 * // Initialisation du plugin
 * const orchestrator = new PluginOrchestrator();
 * 
 * // Enregistrement des handlers
 * orchestrator.initialize([
 *   new ExecuteCodeTaskHandler(),
 *   new GenerateImageTaskHandler(),
 *   new AnalyzeShapeTaskHandler()
 * ]);
 * 
 * // Vérification des handlers enregistrés
 * console.log('Available tasks:', orchestrator.getRegisteredHandlers());
 * // Output: ['executeCode', 'generateImage', 'analyzeShape']
 * 
 * // Traitement d'une requête
 * const request: PluginTaskRequest = {
 *   id: 'req-123',
 *   task: 'executeCode',
 *   params: { code: 'return 42;' }
 * };
 * 
 * await orchestrator.handleTaskRequest(request);
 * // Réponse envoyée automatiquement à l'UI
 * ```
 * 
 * @see TaskHandlerRegistry Pour la gestion des handlers
 * @see TaskExecutor Pour l'exécution des tâches
 * @see PenpotUIResponseSender Pour l'envoi des réponses
 */
export class PluginOrchestrator {
    /** Registry contenant tous les handlers de tâches disponibles */
    private readonly registry: TaskHandlerRegistry;

    /** Executor responsable de l'exécution des tâches */
    private readonly executor: TaskExecutor;

    /** Strategy d'envoi de réponses vers l'UI Penpot */
    private readonly responseSender: PenpotUIResponseSender;

    /**
     * Crée une instance de l'orchestrateur du plugin.
     * 
     * **Initialisation des composants** :
     * - Crée un registry vide (handlers ajoutés via `initialize()`)
     * - Crée un executor lié au registry
     * - Crée un response sender pour l'UI Penpot
     * 
     * @example
     * ```typescript
     * const orchestrator = new PluginOrchestrator();
     * // Orchestrator prêt, mais sans handlers
     * // Appeler initialize() pour enregistrer les handlers
     * ```
     */
    constructor() {
        this.registry = new TaskHandlerRegistry();
        this.executor = new TaskExecutor(this.registry);
        this.responseSender = new PenpotUIResponseSender();
    }

    /**
     * Initialise le plugin en enregistrant les handlers de tâches.
     * 
     * **Doit être appelé une fois** après la construction, avant
     * de traiter des requêtes.
     * 
     * **Processus** :
     * 1. Log les handlers à enregistrer
     * 2. Enregistre tous les handlers dans le registry
     * 3. Les handlers deviennent disponibles pour l'exécution
     * 
     * @param handlers - Array de handlers à enregistrer
     * 
     * @example
     * ```typescript
     * import { ExecuteCodeTaskHandler } from './handlers/code/ExecuteCodeTaskHandler';
     * import { CustomTaskHandler } from './handlers/custom/CustomTaskHandler';
     * 
     * const orchestrator = new PluginOrchestrator();
     * 
     * orchestrator.initialize([
     *   new ExecuteCodeTaskHandler(),
     *   new CustomTaskHandler()
     * ]);
     * 
     * // Logs:
     * // [PluginOrchestrator] Initializing with handlers: ['executeCode', 'customTask']
     * // Registered handler for task type: executeCode
     * // Registered handler for task type: customTask
     * ```
     * 
     * @see TaskHandlerRegistry.registerAll
     */
    initialize(handlers: ITaskHandler[]): void {
        console.log("[PluginOrchestrator] Initializing with handlers:", handlers.map(h => h.taskType));
        this.registry.registerAll(handlers);
    }

    /**
     * Traite une requête de tâche entrante.
     * 
     * **Point d'entrée principal** pour toutes les requêtes de tâches
     * provenant de l'UI ou du WebSocket.
     * 
     * **Flux de traitement** :
     * ```
     * Request → Context → Command → Executor → Handler → Response
     * ```
     * 
     * **Processus détaillé** :
     * 1. **Log** : Enregistre la réception de la requête
     * 2. **Context** : Crée un contexte d'exécution avec:
     *    - ID de requête (pour tracking)
     *    - Type de tâche (pour routing)
     *    - Response sender (pour envoi de réponse)
     * 3. **Command** : Encapsule requête + contexte
     * 4. **Execute** : Délègue au TaskExecutor qui:
     *    - Trouve le handler approprié
     *    - Exécute la tâche
     *    - Envoie la réponse via le context
     * 
     * **Gestion d'erreurs** :
     * - Les erreurs sont gérées par le TaskExecutor
     * - Le context garantit qu'une réponse est toujours envoyée
     * - Aucune exception ne remonte (méthode async void-like)
     * 
     * @param request - Requête de tâche à traiter
     * @returns Promise résolue quand le traitement est terminé
     * 
     * @example
     * ```typescript
     * // Requête d'exécution de code
     * const codeRequest: PluginTaskRequest = {
     *   id: 'req-abc-123',
     *   task: 'executeCode',
     *   params: {
     *     code: 'console.log("Hello"); return 42;'
     *   }
     * };
     * 
     * await orchestrator.handleTaskRequest(codeRequest);
     * 
     * // Logs:
     * // [PluginOrchestrator] Received task request: executeCode
     * // [TaskExecutor] Executing task: executeCode
     * // [TaskExecutor] Processing with handler: executeCode
     * // [ExecuteCodeTaskHandler] Starting code execution
     * // [ExecuteCodeTaskHandler] Code execution result: 42
     * // [TaskExecutor] Task completed: req-abc-123
     * // Sent task response: { type: 'task-response', response: { ... } }
     * ```
     * 
     * @example
     * ```typescript
     * // Requête avec handler inexistant
     * const invalidRequest: PluginTaskRequest = {
     *   id: 'req-xyz-456',
     *   task: 'unknownTask',
     *   params: {}
     * };
     * 
     * await orchestrator.handleTaskRequest(invalidRequest);
     * 
     * // Logs:
     * // [PluginOrchestrator] Received task request: unknownTask
     * // [TaskExecutor] Executing task: unknownTask
     * // [TaskExecutor] Unknown task type: unknownTask. Available types: executeCode, ...
     * // Sent task response: { 
     * //   type: 'task-response', 
     * //   response: { 
     * //     id: 'req-xyz-456',
     * //     success: false,
     * //     error: 'Unknown task type: unknownTask. Available types: executeCode, ...'
     * //   } 
     * // }
     * ```
     * 
     * @see TaskContext Pour la gestion du contexte d'exécution
     * @see TaskExecutionCommand Pour l'encapsulation de la requête
     * @see TaskExecutor.execute Pour le processus d'exécution
     */
    async handleTaskRequest(request: PluginTaskRequest): Promise<void> {
        console.log("[PluginOrchestrator] Received task request:", request.task);

        const context = new TaskContext(
            request.id,
            request.task,
            this.responseSender
        );

        const command = new TaskExecutionCommand(request, context);
        await this.executor.execute(command);
    }

    /**
     * Récupère la liste des types de tâches enregistrés.
     * 
     * **Utilité** :
     * - Introspection du système
     * - Génération de documentation
     * - Messages d'erreur informatifs
     * - UI dynamique des capacités
     * 
     * @returns Array des types de tâches disponibles
     * 
     * @example
     * ```typescript
     * const handlers = orchestrator.getRegisteredHandlers();
     * console.log('Plugin capabilities:', handlers);
     * // Output: ['executeCode', 'generateImage', 'analyzeShape']
     * 
     * // Utilisation pour validation
     * const taskType = 'executeCode';
     * if (handlers.includes(taskType)) {
     *   console.log(`Task '${taskType}' is supported`);
     * } else {
     *   console.error(`Task '${taskType}' is not supported`);
     *   console.log('Available tasks:', handlers.join(', '));
     * }
     * ```
     * 
     * @see TaskHandlerRegistry.getRegisteredTypes
     */
    getRegisteredHandlers(): string[] {
        return this.registry.getRegisteredTypes();
    }
}