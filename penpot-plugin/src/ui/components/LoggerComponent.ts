import { AbstractUIComponent } from './AbstractUIComponent';
import { ILogger } from '../interfaces/IUIComponents';

/**
 * Composant d'affichage des logs de l'application.
 * 
 * **Élément UI géré** :
 * - `<pre>` element : Console de logs avec scroll automatique
 * 
 * **Fonctionnalités** :
 * - Ajout de messages avec timestamp automatique
 * - Auto-scroll vers le dernier log
 * - Effacement complet
 * - Format préservé (espaces, retours ligne)
 * 
 * **Structure HTML typique** :
 * ```html
 * <pre id="logs" class="log-console"></pre>
 * ```
 * 
 * **Styles CSS typiques** :
 * ```css
 * .log-console {
 *   font-family: 'Courier New', monospace;
 *   font-size: 12px;
 *   background: #1e1e1e;
 *   color: #d4d4d4;
 *   padding: 12px;
 *   height: 300px;
 *   overflow-y: auto;
 *   border-radius: 4px;
 *   white-space: pre-wrap;
 *   word-wrap: break-word;
 * }
 * ```
 * 
 * @extends AbstractUIComponent
 * @implements {ILogger}
 * 
 * @example
 * ```typescript
 * // Initialisation
 * const logger = new LoggerComponent('logs');
 * logger.initialize();
 * 
 * // Utilisation basique
 * logger.log('Application démarrée');
 * logger.log('Connexion WebSocket...');
 * logger.log('✓ WebSocket connecté');
 * 
 * // Affichage :
 * // [14:32:01] Application démarrée
 * // [14:32:02] Connexion WebSocket...
 * // [14:32:03] ✓ WebSocket connecté
 * 
 * // Logs multi-lignes
 * logger.log(`Résultat d'exécution:
 * {
 *   "success": true,
 *   "value": 42
 * }`);
 * 
 * // Effacement
 * logger.clear();
 * ```
 * 
 * @example
 * ```typescript
 * // Injection dans les services
 * const wsService = new WebSocketService(url, logger);
 * const apiService = new ApiService(baseUrl, logger);
 * 
 * // Les services logguent automatiquement
 * wsService.connect();
 * // Logger affiche : [14:32:05] Connexion WebSocket: ws://...
 * ```
 */
export class LoggerComponent extends AbstractUIComponent implements ILogger {
    /** Élément <pre> pour l'affichage des logs */
    private logElement: HTMLPreElement | null = null;

    /**
     * Crée une instance du composant logger.
     * 
     * @param elementId - ID de l'élément <pre> (défaut: 'logs')
     * 
     * @example
     * ```typescript
     * // Avec ID par défaut
     * const logger = new LoggerComponent();
     * 
     * // Avec ID personnalisé
     * const logger = new LoggerComponent('myLogs');
     * ```
     */
    constructor(elementId: string = 'logs') {
        super(elementId);
    }

    /**
     * Configure la référence à l'élément de log.
     * 
     * Utilise le cast générique pour typer correctement comme HTMLPreElement.
     * 
     * @protected
     * @override
     */
    protected setupElement(): void {
        this.logElement = this.getElement<HTMLPreElement>();
    }

    /**
     * Ajoute un message au log avec horodatage automatique.
     * 
     * **Format** : `[HH:MM:SS] message\n`
     * 
     * **Comportements** :
     * - Horodatage automatique (heure locale)
     * - Ajout d'un retour à la ligne
     * - Auto-scroll vers le bas
     * - Préservation du formatage (espaces, indentations)
     * 
     * **Protection** : Ne fait rien si l'élément n'existe pas.
     * 
     * @param message - Message à logger
     * 
     * @example
     * ```typescript
     * // Logs simples
     * logger.log('Démarrage de l\'application');
     * logger.log('Configuration chargée');
     * 
     * // Logs avec symboles
     * logger.log('✓ Opération réussie');
     * logger.log('✗ Opération échouée');
     * logger.log('⚠ Avertissement');
     * logger.log('ℹ Information');
     * 
     * // Logs avec données structurées
     * logger.log(`Résultat:
     * ${JSON.stringify(data, null, 2)}`);
     * 
     * // Logs multi-lignes
     * logger.log(`
     * ====================================
     * Rapport d'exécution
     * ====================================
     * Statut: Succès
     * Durée: 1.5s
     * Résultat: 42
     * ====================================
     * `);
     * ```
     */
    log(message: string): void {
        if (!this.logElement) return;
        const timestamp = new Date().toLocaleTimeString();
        this.logElement.textContent += `[${timestamp}] ${message}\n`;
        this.logElement.scrollTop = this.logElement.scrollHeight;
    }

    /**
     * Efface tous les logs affichés.
     * 
     * **Usage** :
     * - Nettoyage de la console
     * - Début d'une nouvelle session
     * - Bouton "Clear logs" dans l'UI
     * 
     * @example
     * ```typescript
     * // Bouton de nettoyage
     * clearButton.addEventListener('click', () => {
     *   logger.clear();
     *   logger.log('Logs effacés');
     * });
     * 
     * // Nettoyage automatique
     * setInterval(() => {
     *   if (logger.logElement.textContent.length > 100000) {
     *     logger.clear();
     *     logger.log('Logs auto-nettoyés (taille limite atteinte)');
     *   }
     * }, 60000);
     * ```
     */
    clear(): void {
        if (this.logElement) {
            this.logElement.textContent = '';
        }
    }
}