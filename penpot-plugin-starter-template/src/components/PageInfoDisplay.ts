export class PageInfoDisplay {
    constructor(private element: HTMLDivElement) {}

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
