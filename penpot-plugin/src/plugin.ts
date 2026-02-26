import { PluginOrchestrator } from './orchestration/PluginOrchestrator';
import { ExecuteCodeTaskHandler } from './handlers/code/ExecuteCodeTaskHandler';
import { PluginTaskRequest } from './common/types';

/**
 * **Point d'entrée principal du runtime du plugin Penpot.**
 * 
 * Ce module s'exécute dans le contexte sandbox du plugin Penpot
 * et gère le cycle de vie du plugin côté runtime.
 * 
 * **Responsabilités** :
 * - Configuration du mode multi-utilisateur
 * - Initialisation de l'orchestrateur de tâches
 * - Enregistrement des handlers de tâches
 * - Ouverture de l'interface utilisateur
 * - Gestion des messages UI ↔ Runtime
 * - Gestion des événements Penpot (theme change)
 * 
 * **Architecture** :
 * ```
 * plugin.ts
 *    ↓
 * PluginOrchestrator
 *    ↓
 * ├── TaskHandlerRegistry
 * ├── TaskExecutor
 * ├── PenpotUIResponseSender
 * └── Task Handlers
 *     └── ExecuteCodeTaskHandler
 * ```
 * 
 * **Communication** :
 * ```
 * UI (iframe) ←→ penpot.ui.onMessage ←→ Plugin Runtime
 * ```
 * 
 * @module plugin
 */


// ============================================================================
// UUID Generation
// ============================================================================

/**
 * Génère un UUID v4 sans dépendre de crypto.randomUUID().
 * Version inline pour le sandbox Penpot qui ne supporte pas les imports externes.
 */
function generatePluginUUID(): string {
    // Le sandbox Penpot n'a pas accès à crypto, on utilise Math.random()
    return 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, (c) => {
        const r = Math.random() * 16 | 0;
        const v = c === 'x' ? r : (r & 0x3 | 0x8);
        return v.toString(16);
    });
}

// ============================================================================
// Configuration
// ============================================================================

/**
 * Variable de build-time pour le mode multi-utilisateur.
 * Injectée par le bundler lors de la compilation.
 * 
 * @constant {boolean}
 */
declare const IS_MULTI_USER_MODE: boolean;

/**
 * Indicateur du mode multi-utilisateur.
 * 
 * **Modes** :
 * - `true` : Mode multi-utilisateur (collaboration)
 * - `false` : Mode mono-utilisateur (usage personnel)
 * 
 * **Sources** :
 * - Build-time : Variable `IS_MULTI_USER_MODE`
 * - Fallback : `false` (mono-utilisateur par défaut)
 * 
 * @constant {boolean}
 */
const isMultiUserMode = typeof IS_MULTI_USER_MODE !== "undefined" ? IS_MULTI_USER_MODE : false;

const userToken = generatePluginUUID();

// Logging de démarrage
console.log("[Plugin] Starting Penpot AI Plugin");
console.log("[Plugin] Multi-user mode:", isMultiUserMode);
console.log('[Plugin] User token:', userToken);

// ============================================================================
// Initialisation de l'Orchestrateur
// ============================================================================

/**
 * Instance de l'orchestrateur principal du plugin.
 * 
 * Coordonne :
 * - Registry des handlers
 * - Executor de tâches
 * - Response sender
 * 
 * @type {PluginOrchestrator}
 */
const orchestrator = new PluginOrchestrator();

// ============================================================================
// Enregistrement des Handlers
// ============================================================================

/**
 * Enregistre tous les handlers de tâches disponibles.
 * 
 * **Handlers actuels** :
 * - `ExecuteCodeTaskHandler` : Exécution de code JavaScript
 * 
 * **Extension future** :
 * ```typescript
 * orchestrator.initialize([
 *   new ExecuteCodeTaskHandler(),
 *   new GenerateImageTaskHandler(),
 *   new AnalyzeShapeTaskHandler(),
 *   new ExportDataTaskHandler(),
 * ]);
 * ```
 */
orchestrator.initialize([
    new ExecuteCodeTaskHandler(),
]);

console.log("[Plugin] Registered handlers:", orchestrator.getRegisteredHandlers());

// ============================================================================
// Ouverture de l'Interface Utilisateur
// ============================================================================

/**
 * Ouvre l'interface utilisateur du plugin dans une iframe.
 * 
 * **Paramètres UI** :
 * - `theme` : Thème Penpot courant (dark/light)
 * - `multiUser` : Mode multi-utilisateur
 * 
 * **Dimensions** :
 * - Largeur : 500px
 * - Hauteur : 800px
 * 
 * **URL générée** :
 * `?theme=dark&multiUser=false&userToken=xxx`
 * 
 * @see https://doc.plugins.penpot.app/ Documentation API Penpot
 */
penpot.ui.open(
    "Penpot AI Plugin",
    `?theme=${penpot.theme}&multiUser=${isMultiUserMode}&userToken=${userToken}`,
    { width: 500, height: 800 }
);

// ============================================================================
// Gestion des Messages de l'UI
// ============================================================================

/**
 * Gestionnaire de messages provenant de l'UI.
 * 
 * **Flux de traitement** :
 * ```
 * UI → penpot.ui.sendMessage() → onMessage → Type Guard → handleTaskRequest
 * ```
 * 
 * **Types de messages** :
 * - `PluginTaskRequest` : Requête de tâche à exécuter
 * - Autres : Messages non reconnus (loggés)
 * 
 * **Gestion d'erreurs** :
 * - Erreurs capturées et loggées
 * - N'empêchent pas le traitement des messages suivants
 * 
 * @param {PluginTaskRequest | any} message - Message de l'UI
 * 
 * @example
 * ```typescript
 * // Message valide
 * {
 *   id: 'req-123',
 *   task: 'executeCode',
 *   params: { code: 'return 42;' }
 * }
 * 
 * // Logs:
 * // [Plugin] Received message: { id: 'req-123', ... }
 * // [Plugin] Processing task request
 * // [PluginOrchestrator] Received task request: executeCode
 * // ...
 * ```
 */
penpot.ui.onMessage<PluginTaskRequest | any>((message) => {
    console.log("[Plugin] Received message:", message);

    // Type guard : Vérifie si c'est une requête de tâche
    if (isTaskRequest(message)) {
        console.log("[Plugin] Processing task request");

        orchestrator.handleTaskRequest(message).catch((error) => {
            console.error("[Plugin] Unhandled error:", error);
        });
    } else {
        console.warn("[Plugin] Unknown message type");
    }
});

// ============================================================================
// Gestion des Événements Penpot
// ============================================================================

/**
 * Gestionnaire de changement de thème Penpot.
 * 
 * **Propagation** :
 * Transmet le changement de thème à l'UI pour mise à jour visuelle.
 * 
 * **Thèmes** :
 * - `"dark"` : Thème sombre
 * - `"light"` : Thème clair
 * 
 * **Message envoyé** :
 * ```json
 * {
 *   "source": "penpot",
 *   "type": "themechange",
 *   "theme": "dark"
 * }
 * ```
 * 
 * @param {string} theme - Nouveau thème ('dark' ou 'light')
 */
penpot.on("themechange", (theme) => {
    penpot.ui.sendMessage({ source: 'penpot', type: 'themechange', theme });
});

// ============================================================================
// Type Guards
// ============================================================================

/**
 * **Type Guard** : Vérifie si un message est une requête de tâche valide.
 * 
 * **Critères de validation** :
 * - Type objet non-null
 * - Propriété `id` de type string
 * - Propriété `task` de type string
 * - Propriété `params` présente
 * 
 * **Usage** :
 * Permet à TypeScript de typer correctement le message
 * après validation.
 * 
 * @param message - Message à valider
 * @returns `true` si le message est une PluginTaskRequest valide
 */
function isTaskRequest(message: any): message is PluginTaskRequest {
    return (
        typeof message === "object" &&
        message !== null &&
        typeof message.id === "string" &&
        typeof message.task === "string" &&
        "params" in message
    );
}