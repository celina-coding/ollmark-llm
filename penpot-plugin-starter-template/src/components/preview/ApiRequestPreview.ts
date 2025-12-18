export class ApiRequestPreview {
    constructor(
        private sectionElement: HTMLElement,
        private contentElement: HTMLPreElement
    ) {}

    show(requestBody: any): void {
        const formatted = JSON.stringify(requestBody, null, 2);
        this.contentElement.textContent = formatted;
        this.sectionElement.style.display = 'block';
    }

    hide(): void {
        this.sectionElement.style.display = 'none';
    }
}