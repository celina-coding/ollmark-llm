export class ViewManager {
    constructor(
        private creationView: HTMLElement,
        private modificationView: HTMLElement
    ) {}

    showCreationView(): void {
        this.creationView.style.display = 'block';
        this.modificationView.style.display = 'none';
    }

    showModificationView(): void {
        this.creationView.style.display = 'none';
        this.modificationView.style.display = 'block';
    }
}