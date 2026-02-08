import { IUIComponent } from '../interfaces/IUIComponents';

/**
 * Classe abstraite de base pour tous les composants UI.
 * 
 * **Cycle de vie d'un composant** :
 * ```
 * Construction → initialize() → Opération → destroy()
 *                    ↓
 *        ┌───────────┴───────────┐
 *        ↓                       ↓
 *   setupElement()      attachEventListeners()
 * ```
 * 
 * **Hook Methods (à surcharger)** :
 * - `setupElement()` : Configuration spécifique du composant
 * - `attachEventListeners()` : Ajout d'event listeners
 * - `removeEventListeners()` : Nettoyage des event listeners
 * 
 * **Avantages** :
 * - Cohérence dans le cycle de vie des composants
 * - Réduction de code dupliqué
 * - Gestion d'erreur centralisée (élément non trouvé)
 * - Accès sécurisé au DOM
 * 
 * @abstract
 * @implements {IUIComponent}
 * 
 * @example
 * ```typescript
 * // Créer un composant personnalisé
 * class CustomComponent extends AbstractUIComponent {
 *   private button: HTMLButtonElement | null = null;
 *   
 *   constructor() {
 *     super('myButton');
 *   }
 *   
 *   protected setupElement(): void {
 *     this.button = this.getElement<HTMLButtonElement>();
 *     if (this.button) {
 *       this.button.style.color = 'blue';
 *     }
 *   }
 *   
 *   protected attachEventListeners(): void {
 *     this.button?.addEventListener('click', this.handleClick);
 *   }
 *   
 *   protected removeEventListeners(): void {
 *     this.button?.removeEventListener('click', this.handleClick);
 *   }
 *   
 *   private handleClick = () => {
 *     console.log('Button clicked!');
 *   };
 * }
 * 
 * // Utilisation
 * const component = new CustomComponent();
 * component.initialize(); // Configure tout automatiquement
 * // ...
 * component.destroy();    // Nettoie tout automatiquement
 * ```
 */
export abstract class AbstractUIComponent implements IUIComponent {
    /** Référence à l'élément DOM principal géré par ce composant */
    protected element: HTMLElement | null = null;

    /**
     * Crée une instance du composant UI.
     * 
     * @param elementId - ID de l'élément DOM à gérer
     * 
     * @example
     * ```typescript
     * class MyComponent extends AbstractUIComponent {
     *   constructor() {
     *     super('myElementId');
     *   }
     * }
     * ```
     */
    constructor(public readonly elementId: string) {}

    /**
     * Séquence d'initialisation du composant.
     * 
     * **Processus d'initialisation** :
     * 1. **Récupération DOM** : Trouve l'élément par ID
     * 2. **Validation** : Vérifie que l'élément existe
     * 3. **Setup** : Appelle `setupElement()` (hook method)
     * 4. **Events** : Appelle `attachEventListeners()` (hook method)
     * 
     * **Gestion d'erreur** :
     * - Si l'élément n'est pas trouvé, log un avertissement
     * - Retourne sans lever d'exception (fail gracefully)
     * - Permet au composant de continuer (peut être conditionnel)
     * 
     * **Ne pas surcharger** cette méthode. Surcharger les hook methods
     * `setupElement()` et `attachEventListeners()` à la place.
     * 
     * @example
     * ```typescript
     * const component = new LoggerComponent('logs');
     * component.initialize();
     * 
     * // Si élément trouvé :
     * // - this.element = <HTMLPreElement>
     * // - setupElement() appelé
     * // - attachEventListeners() appelé
     * 
     * // Si élément non trouvé :
     * // - Console: "[LoggerComponent] Element not found: logs"
     * // - this.element = null
     * // - Pas d'exception levée
     * ```
     */
    initialize(): void {
        this.element = document.getElementById(this.elementId);

        if (!this.element) {
            console.warn(`[${this.constructor.name}] Element not found: ${this.elementId}`);
            return;
        }

        this.setupElement();
        this.attachEventListeners();
    }

    /**
     * Séquence de destruction du composant.
     * 
     * **Processus de nettoyage** :
     * 1. **Events** : Appelle `removeEventListeners()` (hook method)
     * 2. **Cleanup** : Réinitialise la référence DOM à null
     * 
     * **Importance** :
     * - Évite les fuites mémoire (event listeners)
     * - Libère les références DOM
     * - Prépare le composant pour garbage collection
     * 
     * **Doit être appelé** :
     * - Lors de la fermeture de l'application
     * - Lors du démontage du composant
     * - Lors du changement de page/vue
     * 
     * @example
     * ```typescript
     * // Nettoyage lors de la fermeture
     * window.addEventListener('beforeunload', () => {
     *   component.destroy();
     * });
     * 
     * // Nettoyage lors du changement de vue
     * function switchView() {
     *   oldComponent.destroy();
     *   newComponent.initialize();
     * }
     * ```
     */
    destroy(): void {
        this.removeEventListeners();
        this.element = null;
    }

/**
     * Configuration spécifique du composant.
     * 
     * Appelé après que `this.element` est défini et validé.
     * 
     * **Usage typique** :
     * - Récupérer des références à des sous-éléments
     * - Configurer l'état initial du DOM
     * - Initialiser des propriétés basées sur le DOM
     * 
     * **Implémentation par défaut** : Aucune opération.
     * 
     * @protected
     * 
     * @example
     * ```typescript
     * protected setupElement(): void {
     *   // Récupérer sous-éléments
     *   this.button = this.element?.querySelector('.btn');
     *   this.input = this.element?.querySelector('input');
     *   
     *   // Configuration initiale
     *   if (this.input) {
     *     this.input.placeholder = 'Enter text...';
     *   }
     * }
     * ```
     */
    protected setupElement(): void {
        // Implémentation par défaut : rien
        // Les sous-classes peuvent surcharger pour ajouter leur logique
    }

    /**
     * Attachement des event listeners.
     * 
     * Appelé après `setupElement()`.
     * 
     * **Usage typique** :
     * - Ajouter des listeners sur les éléments
     * - Configurer des observers (MutationObserver, etc.)
     * - S'abonner à des événements globaux
     * 
     * **Important** : Utiliser des fonctions fléchées ou `.bind()`
     * pour préserver le contexte `this` et permettre le nettoyage.
     * 
     * **Implémentation par défaut** : Aucune opération.
     * 
     * @protected
     * 
     * @example
     * ```typescript
     * // BON : Fonction fléchée (permet removeEventListener)
     * protected attachEventListeners(): void {
     *   this.button?.addEventListener('click', this.handleClick);
     * }
     * 
     * private handleClick = () => {
     *   console.log('Clicked!');
     * };
     * 
     * // BON : bind() (permet removeEventListener)
     * protected attachEventListeners(): void {
     *   this.boundHandler = this.handleClick.bind(this);
     *   this.button?.addEventListener('click', this.boundHandler);
     * }
     * 
     * // MAUVAIS : Fonction anonyme (impossible à remove)
     * protected attachEventListeners(): void {
     *   this.button?.addEventListener('click', () => {
     *     console.log('Clicked!');
     *   });
     * }
     * ```
     */
    protected attachEventListeners(): void {
        // Implémentation par défaut : rien
        // Les sous-classes peuvent surcharger pour ajouter leurs listeners
    }

    /**
     * Nettoyage des event listeners.
     * 
     * Appelé par `destroy()` avant la libération des références.
     * 
     * **Critical** : DOIT retirer tous les listeners ajoutés
     * dans `attachEventListeners()` pour éviter les fuites mémoire.
     * 
     * **Usage typique** :
     * - Retirer tous les event listeners
     * - Déconnecter les observers
     * - Se désabonner des événements globaux
     * 
     * **Implémentation par défaut** : Aucune opération.
     * 
     * @protected
     * 
     * @example
     * ```typescript
     * protected removeEventListeners(): void {
     *   // Retirer listeners sur éléments locaux
     *   this.button?.removeEventListener('click', this.handleClick);
     *   this.input?.removeEventListener('input', this.handleInput);
     *   
     *   // Retirer listeners globaux
     *   window.removeEventListener('resize', this.handleResize);
     *   
     *   // Déconnecter observers
     *   this.mutationObserver?.disconnect();
     * }
     * ```
     */
    protected removeEventListeners(): void {
        // Implémentation par défaut : rien
        // Les sous-classes peuvent surcharger pour nettoyer leurs listeners
    }

    /**
     * Accès sécurisé et typé à l'élément principal.
     * 
     * Permet un cast typé pour un accès facile aux propriétés
     * spécifiques du type d'élément.
     * 
     * **Retourne** `null` si l'élément n'existe pas ou n'est pas initialisé.
     * 
     * @template T - Type HTMLElement spécifique (HTMLInputElement, etc.)
     * @returns L'élément casté au type T ou null
     * 
     * @protected
     * 
     * @example
     * ```typescript
     * // Sans typage générique
     * const element = this.getElement();
     * element?.textContent = 'Hello'; // OK
     * element?.value = 'test';        // Erreur : value n'existe pas sur HTMLElement
     * 
     * // Avec typage générique
     * const input = this.getElement<HTMLInputElement>();
     * input?.value = 'test';          // OK
     * input?.placeholder = 'Enter';   // OK
     * 
     * // Différents types
     * const pre = this.getElement<HTMLPreElement>();
     * const button = this.getElement<HTMLButtonElement>();
     * const select = this.getElement<HTMLSelectElement>();
     * ```
     */
    protected getElement<T extends HTMLElement>(): T | null {
        return this.element as T | null;
    }
}