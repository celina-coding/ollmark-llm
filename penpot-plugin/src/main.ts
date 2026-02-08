import "./style.css";
import { UIOrchestrator } from './ui/orchestration/UIOrchestrator';

/**
 * **Point d'entrée principal de l'interface utilisateur du plugin Penpot.**
 * 
 * Ce module initialise et démarre l'ensemble du système UI lorsque
 * la page est chargée dans l'iframe du plugin.
 * 
 * **Responsabilités** :
 * - Configuration des URLs de connexion
 * - Initialisation de l'orchestrateur UI
 * - Gestion du cycle de vie de l'application
 * - Nettoyage lors de la fermeture
 * 
 * **Architecture** :
 * ```
 * main.ts
 *    ↓
 * UIOrchestrator
 *    ↓
 * ├── Components (Logger, Status, Chat)
 * ├── Services (WebSocket, API)
 * ├── Managers (Conversation)
 * └── Bridges (PluginMessage)
 * ```
 * 
 * **Flux de démarrage** :
 * 1. Chargement de la page HTML
 * 2. Exécution de main.ts
 * 3. Configuration des URLs
 * 4. Bootstrap de l'application
 * 5. Création de l'orchestrateur
 * 6. Initialisation du système UI
 * 7. Connexion WebSocket automatique
 * 8. Application prête
 * 
 * @module main
 */

// ============================================================================
// Configuration
// ============================================================================

/**
 * Variable de build-time pour l'URL WebSocket.
 * Injectée par le bundler (Vite/Webpack) lors de la compilation.
 * 
 * @constant {string}
 */
declare const PENPOT_WEBSOCKET_URL: string;

/**
 * URL du WebSocket pour la communication temps-réel.
 * 
 * **Sources** :
 * - Build-time : Variable `PENPOT_WEBSOCKET_URL` du bundler
 * - Fallback : `ws://localhost:4401/plugin` pour développement local
 * 
 * **Format** : `ws://host:port/plugin` ou `wss://host:port/plugin`
 * 
 * @constant {string}
 * 
 * @example
 * ```typescript
 * // Development
 * // WS_URL = "ws://localhost:4401/plugin"
 * 
 * // Production
 * // WS_URL = "wss://api.example.com/plugin"
 * ```
 */
const WS_URL = typeof PENPOT_WEBSOCKET_URL !== 'undefined' 
    ? PENPOT_WEBSOCKET_URL 
    : "ws://localhost:4401/plugin";

/**
 * URL de base pour les appels API HTTP.
 * 
 * **Dérivation** : Construite à partir de `WS_URL`
 * - Remplace `ws://` par `http://`
 * - Remplace `wss://` par `https://`
 * - Retire le suffixe `/plugin`
 * 
 * **Format** : `http://host:port` ou `https://host:port`
 * 
 * @constant {string}
 * 
 * @example
 * ```typescript
 * // Si WS_URL = "ws://localhost:4401/plugin"
 * // Alors API_BASE_URL = "http://localhost:4401"
 * 
 * // Si WS_URL = "wss://api.example.com/plugin"
 * // Alors API_BASE_URL = "https://api.example.com"
 * ```
 */
const API_BASE_URL = WS_URL.replace(/^ws/, "http").replace(/\/plugin$/, "");

// ============================================================================
// Application Bootstrap
// ============================================================================

/**
 * Fonction de démarrage de l'application.
 * 
 * **Processus** :
 * 1. **Création** : Instancie l'orchestrateur UI avec les URLs
 * 2. **Initialisation** : Démarre tous les composants et services
 * 3. **Lifecycle** : Configure le nettoyage lors de la fermeture
 * 4. **Logging** : Confirme l'initialisation
 * 
 * **Appelée automatiquement** au chargement du module.
 * 
 * @function bootstrap
 * 
 * @example
 * ```typescript
 * // Séquence de démarrage :
 * 
 * // 1. Création de l'orchestrateur
 * const orchestrator = new UIOrchestrator(WS_URL, API_BASE_URL);
 * 
 * // 2. Initialisation (configure tout)
 * orchestrator.initialize();
 * // → Components.initialize()
 * // → wireEventHandlers()
 * // → setupStatusMonitoring()
 * // → messageBridge.initialize()
 * // → webSocketService.connect()
 * 
 * // 3. Configuration cleanup
 * window.addEventListener('beforeunload', () => {
 *   orchestrator.destroy();
 * });
 * 
 * // 4. Log confirmation
 * console.log('[Main] Penpot Plugin UI initialized');
 * ```
 * 
 * @example
 * ```typescript
 * // Logs de démarrage typiques :
 * // [Logger] Initializing...
 * // [StatusDisplay] Initializing...
 * // [ChatDisplay] Initializing...
 * // [WebSocket] Connexion WebSocket: ws://localhost:4401/plugin
 * // [WebSocket] ✓ WebSocket connecté
 * // [API] Création nouvelle conversation...
 * // [API] ✓ Conversation créée: conv_abc123def456
 * // [Chat] Conversation prête. ID: conv_abc1...
 * // [Main] Penpot Plugin UI initialized
 * ```
 */
function bootstrap(): void {
    const orchestrator = new UIOrchestrator(WS_URL, API_BASE_URL);
    orchestrator.initialize();

    window.addEventListener('beforeunload', () => {
        orchestrator.destroy();
    });

    console.log('[Main] Penpot Plugin UI initialized');
}

// ============================================================================
// Démarrage de l'application
// ============================================================================

/**
 * Démarre l'application immédiatement au chargement du module.
 * 
 * L'exécution se fait au niveau du module, garantissant que
 * l'application démarre dès que le script est chargé.
 */
bootstrap();