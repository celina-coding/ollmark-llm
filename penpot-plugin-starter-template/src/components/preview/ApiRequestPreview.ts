/**
 * Affiche un aperçu lisible du corps d'une requête API.
 *
 * Utilisé principalement à des fins de debug ou de transparence utilisateur.
 */
export class ApiRequestPreview {
    constructor(
        /**
         * Section DOM contenant l'aperçu.
         */
        private readonly sectionElement: HTMLElement,

        /**
         * Élément `<pre>` affichant le JSON formaté.
         */
        private readonly contentElement: HTMLPreElement
    ) {}

    /**
     * Affiche le corps de la requête formaté en JSON.
     *
     * @param requestBody Corps de la requête à afficher.
     */
    show(requestBody: unknown): void {
        this.contentElement.textContent =
            JSON.stringify(requestBody, null, 2);
        this.sectionElement.style.display = 'block';
    }

    /**
     * Masque l'aperçu de la requête.
     */
    hide(): void {
        this.sectionElement.style.display = 'none';
    }
}