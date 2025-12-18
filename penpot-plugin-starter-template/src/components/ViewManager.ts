/**
 * Gère l'affichage conditionnel des vues principales de l'UI.
 */
export class ViewManager {
    constructor(
        private readonly creationView: HTMLElement,
        private readonly modificationView: HTMLElement
    ) {}

    /**
     * Affiche la vue de création.
     */
    showCreationView(): void {
        this.creationView.style.display = 'block';
        this.modificationView.style.display = 'none';
    }

    /**
     * Affiche la vue de modification.
     */
    showModificationView(): void {
        this.creationView.style.display = 'none';
        this.modificationView.style.display = 'block';
    }
}