/**
 * Affiche un aperçu JSON générique avec indicateur de contenu.
 */
export class JsonPreview {
    constructor(
        private readonly sectionElement: HTMLElement,
        private readonly contentElement: HTMLPreElement,
        private readonly countBadge: HTMLElement
    ) {}

    /**
     * Affiche les données JSON formatées et le nombre d'objets détectés.
     *
     * @param data Données à afficher.
     */
    show(data: { objects?: Record<string, unknown> } | unknown): void {
        this.contentElement.textContent =
            JSON.stringify(data, null, 2);

        const objectsCount =
            typeof data === 'object' && data && 'objects' in data && data.objects
                ? Object.keys(data.objects as Record<string, unknown>).length
                : 0;

        this.countBadge.textContent =
            `${objectsCount} objet${objectsCount > 1 ? 's' : ''}`;

        this.sectionElement.style.display = 'block';
    }

    /**
     * Masque l'aperçu JSON.
     */
    hide(): void {
        this.sectionElement.style.display = 'none';
    }
}