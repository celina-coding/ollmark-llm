import { StatusType } from "../types";

export class StatusDisplay {
    constructor(private element: HTMLDivElement) {}

    show(message: string, type: StatusType): void {
        this.element.textContent = message;
        this.element.className = `status status-${type}`;
        this.element.style.display = 'block';
    }

    hide(): void {
        this.element.style.display = 'none';
    }
}