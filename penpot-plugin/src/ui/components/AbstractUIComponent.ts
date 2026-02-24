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
 * @abstract
 * @implements {IUIComponent}
 * ```
 */
export abstract class AbstractUIComponent implements IUIComponent {
    /** Référence à l'élément DOM principal géré par ce composant */
    protected element: HTMLElement | null = null;

    /**
     * Crée une instance du composant UI.
     * 
     * @param elementId - ID de l'élément DOM à gérer
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
     * @template T - Type HTMLElement spécifique (HTMLInputElement, etc.)
     * @returns L'élément casté au type T ou null
     */
    protected getElement<T extends HTMLElement>(): T | null {
        return this.element as T | null;
    }

    /**
     * Raccourci mutualisé pour récupérer un élément DOM secondaire typé.
     */
    protected getDOMElement<T extends HTMLElement>(id: string): T | null {
        return document.getElementById(id) as T | null;
    }
}