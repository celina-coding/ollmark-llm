import { ITaskHandler } from '../interfaces/ITaskHandler';

/**
 * Classe abstraite de base pour les gestionnaires de tâches.
 * 
 * Flux d'exécution standard :
 * 1. Validation des paramètres (hook method)
 * 2. Exécution métier (abstract method)
 * 
 * @template TParams - Type des paramètres d'entrée
 * @template TResult - Type du résultat de sortie
 * 
 * @example
 * class MyTaskHandler extends AbstractTaskHandler<MyParams, MyResult> {
 *   readonly taskType = "myTask";
 *   
 *   protected validateParams(params: MyParams): void {
 *     if (!params.required) throw new Error("Missing required field");
 *   }
 *   
 *   protected async execute(params: MyParams): Promise<MyResult> {
 *     return { success: true };
 *   }
 * }
 */
export abstract class AbstractTaskHandler<TParams = any, TResult = any> 
    implements ITaskHandler<TParams, TResult> {

    /**
     * Identifiant unique du type de tâche géré.
     * Doit être défini par chaque sous-classe.
     * 
     * @example "executeCode", "generateImage", "analyzeShape"
     */
    abstract readonly taskType: string;

    /**
     * Détermine si ce handler peut gérer un type de tâche.
     * 
     * Peut être surchargée pour une logique de matching complexe
     * (ex: patterns, préfixes, catégories).
     * 
     * @param taskType - Type de tâche à vérifier
     * @returns true si ce handler peut traiter la tâche
     * 
     * @example
     * // Matching simple (par défaut)
     * canHandle("executeCode") // true si taskType === "executeCode"
     * 
     * // Matching complexe (à surcharger)
     * canHandle(taskType: string): boolean {
     *   return taskType.startsWith("image.");
     * }
     */
    canHandle(taskType: string): boolean {
        return this.taskType === taskType;
    }

    /**
     * Flux principal d'exécution d'une tâche.
     * 
     * Orchestration :
     * 1. Validation des paramètres (hook)
     * 2. Exécution métier (abstract)
     * 
     * Ne pas surcharger cette méthode - surcharger execute() à la place.
     * 
     * @param params - Paramètres de la tâche
     * @returns Résultat de l'exécution
     * @throws Error si validation échoue ou exécution échoue
     * 
     * @example
     * // Utilisé par TaskExecutor
     * const result = await handler.handle(params);
     */
    async handle(params: TParams): Promise<TResult> {
        // Hook 1 : Pré-validation
        this.validateParams(params);

        // Cœur métier
        return await this.execute(params);
    }

    /**
     * Validation des paramètres avant exécution.
     * 
     * Implémentation par défaut : aucune validation.
     * Surcharger pour ajouter des vérifications spécifiques.
     * 
     * @param params - Paramètres à valider
     * @throws Error si les paramètres sont invalides
     * 
     * @protected
     * 
     * @example
     * protected validateParams(params: MyParams): void {
     *   if (!params.code) {
     *     throw new Error("Code parameter is required");
     *   }
     *   if (params.timeout < 0) {
     *     throw new Error("Timeout must be positive");
     *   }
     * }
     */
    protected validateParams(params: TParams): void {
        // Implémentation par défaut : pas de validation
        // Les sous-classes peuvent surcharger pour ajouter leurs vérifications
    }

    /**
     * Logique métier d'exécution de la tâche.
     * 
     * DOIT être implémentée par toutes les sous-classes concrètes.
     * Contient la logique spécifique de traitement de la tâche.
     * 
     * @param params - Paramètres validés de la tâche
     * @returns Résultat de l'exécution
     * @throws Error en cas d'échec d'exécution
     * 
     * @protected
     * @abstract
     * 
     * @example
     * protected async execute(params: CodeParams): Promise<CodeResult> {
     *   const output = await this.compiler.compile(params.code);
     *   return { compiled: output };
     * }
     */
    protected abstract execute(params: TParams): Promise<TResult>;
}