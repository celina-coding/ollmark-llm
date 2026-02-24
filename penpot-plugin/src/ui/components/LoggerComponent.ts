import { AbstractUIComponent } from './AbstractUIComponent';
import { ILogger } from '../../common/Logger';

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
 * @extends AbstractUIComponent
 * @implements {ILogger}
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
     */
    clear(): void {
        if (this.logElement) this.logElement.textContent = '';
    }
}