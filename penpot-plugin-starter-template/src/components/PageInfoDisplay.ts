/**
 * Affiche les informations liées à la page courante et à son contexte.
 */
export class PageInfoDisplay {
    constructor(
        /**
         * Élément DOM racine de l'affichage.
         */
        private readonly element: HTMLDivElement
    ) {}

    /**
     * Met à jour l'affichage des informations de page.
     *
     * @param fileId Identifiant du fichier courant.
     * @param pageId Identifiant de la page courante.
     * @param hasContext Indique si un contexte de page est disponible.
     */
    update(fileId: string, pageId: string, hasContext: boolean): void {
        const content = this.element.querySelector('.info-content');
        if (!content) return;

        if (!fileId && !pageId && !hasContext) {
            content.innerHTML = `
                <div class="info-item loading">
                    <span class="status-icon">⏳</span>
                    <span>En attente d'export...</span>
                </div>
            `;
            return;
        }

        let html = '';

        if (fileId) {
            html += `
                <div class="info-item success">
                    <span class="status-icon">📁</span>
                    <span>File ID: <code>${fileId}</code></span>
                </div>
            `;
        }

        if (pageId) {
            html += `
                <div class="info-item success">
                    <span class="status-icon">📄</span>
                    <span>Page ID: <code>${pageId}</code></span>
                </div>
            `;
        }

        if (hasContext) {
            html += `
                <div class="info-item success">
                    <span class="status-icon">✅</span>
                    <span>Contexte exporté avec succès</span>
                </div>
            `;
        }

        content.innerHTML = html;
    }
}