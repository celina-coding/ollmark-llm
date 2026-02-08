import { ITaskHandler } from '../interfaces/ITaskHandler';

/**
 * Gère l'enregistrement et la récupération des handlers de tâches.
 * 
 * Responsabilités :
 * - Enregistrer les handlers disponibles
 * - Rechercher le handler approprié pour une tâche
 * - Fournir des informations sur les handlers enregistrés
 * 
 * @example
 * const registry = new TaskHandlerRegistry();
 * registry.register(new ExecuteCodeTaskHandler());
 * registry.register(new GenerateImageTaskHandler());
 * 
 * const handler = registry.getHandler("executeCode");
 * if (handler) {
 *   await handler.handle(params);
 * }
 */
export class TaskHandlerRegistry {
    /** Map interne : taskType → ITaskHandler */
    private handlers: Map<string, ITaskHandler> = new Map();

    /**
     * Enregistre un nouveau gestionnaire de tâches.
     * 
     * Si un handler existe déjà pour ce type, il est écrasé
     * avec un avertissement console.
     * 
     * @param handler - Handler à enregistrer
     * 
     * @example
     * registry.register(new MyCustomHandler());
     */
    register(handler: ITaskHandler): void {
        if (this.handlers.has(handler.taskType)) {
            console.warn(`Handler for task type '${handler.taskType}' already registered. Overwriting.`);
        }
        this.handlers.set(handler.taskType, handler);
        console.log(`Registered handler for task type: ${handler.taskType}`);
    }

    /**
     * Enregistre plusieurs handlers en une seule opération.
     * 
     * Utile pour l'initialisation en masse.
     * 
     * @param handlers - Array de handlers à enregistrer
     * 
     * @example
     * registry.registerAll([
     *   new CodeHandler(),
     *   new ImageHandler(),
     *   new DataHandler()
     * ]);
     */
    registerAll(handlers: ITaskHandler[]): void {
        handlers.forEach(handler => this.register(handler));
    }

    /**
     * Récupère le handler approprié pour un type de tâche.
     * 
     * @param taskType - Type de tâche recherché
     * @returns Handler correspondant ou undefined si non trouvé
     * 
     * @example
     * const handler = registry.getHandler("executeCode");
     * if (handler) {
     *   const result = await handler.handle(params);
     * } else {
     *   console.error("No handler found");
     * }
     */
    getHandler(taskType: string): ITaskHandler | undefined {
        return this.handlers.get(taskType);
    }

    /**
     * Vérifie si un handler existe pour un type de tâche.
     * 
     * @param taskType - Type de tâche à vérifier
     * @returns true si un handler est enregistré
     * 
     * @example
     * if (registry.hasHandler("executeCode")) {
     *   // Procéder avec confiance
     * }
     */
    hasHandler(taskType: string): boolean {
        return this.handlers.has(taskType);
    }

    /**
     * Récupère tous les types de tâches enregistrés.
     * 
     * Utile pour :
     * - Afficher les capacités du système
     * - Générer des messages d'erreur informatifs
     * - Documentation dynamique
     * 
     * @returns Array des types de tâches disponibles
     * 
     * @example
     * const available = registry.getRegisteredTypes();
     * console.log(`Available tasks: ${available.join(', ')}`);
     * // Output: "Available tasks: executeCode, generateImage, analyzeData"
     */
    getRegisteredTypes(): string[] {
        return Array.from(this.handlers.keys());
    }

    /**
     * Vide le registry de tous les handlers.
     * 
     * Principalement utilisé pour les tests unitaires
     * pour garantir un état propre entre les tests.
     * 
     * @example
     * afterEach(() => {
     *   registry.clear();
     * });
     */
    clear(): void {
        this.handlers.clear();
    }
}