import { StatusType } from "../types";

/**
 * Gère l'affichage des messages de statut utilisateur.
 */
export class StatusDisplay {
    constructor(
        /**
         * Élément DOM cible pour le statut.
         */
        private readonly element: HTMLDivElement
    ) {}

    /**
     * Affiche un message de statut.
     *
     * @param message Message à afficher.
     * @param type Type de statut (info, succès, erreur, avertissement).
     */
    show(message: string, type: StatusType): void {
        this.element.textContent = message;
        this.element.className = `status status-${type}`;
        this.element.style.display = 'block';
    }

    /**
     * Masque le message de statut.
     */
    hide(): void {
        this.element.style.display = 'none';
    }
}