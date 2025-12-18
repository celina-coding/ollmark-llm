import { CodeGenerationResponse } from "../types";

export class MetricsDisplay {
    constructor(private element: HTMLDivElement) {}

    display(response: CodeGenerationResponse): void {
        const metrics = [
            `⏱️ Temps de génération: ${response.generationTimeMs}ms`,
            `📏 Longueur du code: ${response.codeLength} caractères`,
            `🎯 Stratégie: ${response.strategy}`,
        ];

        if (response.promptTokensEstimate) {
            metrics.push(`🔤 Tokens estimés: ${response.promptTokensEstimate}`);
        }

        this.element.innerHTML = '<div class="metrics">' + metrics.join(' | ') + '</div>';
    }

    clear(): void {
        this.element.innerHTML = '';
    }
}