import { CodeGenerationResponse } from "../types";

/**
 * Affiche les métriques associées à une génération de code.
 */
export class MetricsDisplay {
    constructor(
        /**
         * Élément DOM cible pour les métriques.
         */
        private readonly element: HTMLDivElement
    ) {}

    /**
     * Affiche les métriques issues de la réponse de génération.
     *
     * @param response Réponse du moteur de génération.
     */
    display(response: CodeGenerationResponse): void {
        const metrics = [
            `⏱️ Temps de génération: ${response.generationTimeMs}ms`,
            `📏 Longueur du code: ${response.codeLength} caractères`,
            `🎯 Stratégie: ${response.strategy}`,
        ];

        if (response.promptTokensEstimate) {
            metrics.push(`🔤 Tokens estimés: ${response.promptTokensEstimate}`);
        }

        this.element.innerHTML =
            '<div class="metrics">' + metrics.join(' | ') + '</div>';
    }

    /**
     * Supprime l'affichage des métriques.
     */
    clear(): void {
        this.element.innerHTML = '';
    }
}