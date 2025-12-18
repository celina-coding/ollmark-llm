export class JsonPreview {
    constructor(
        private sectionElement: HTMLElement,
        private contentElement: HTMLPreElement,
        private countBadge: HTMLElement
    ) {}

    show(data: any): void {
        const formatted = JSON.stringify(data, null, 2);
        this.contentElement.textContent = formatted;

        const objectsCount = data?.objects ? Object.keys(data.objects).length : 0;
        this.countBadge.textContent = `${objectsCount} objet${objectsCount > 1 ? 's' : ''}`;

        this.sectionElement.style.display = 'block';
    }

    hide(): void {
        this.sectionElement.style.display = 'none';
    }
}