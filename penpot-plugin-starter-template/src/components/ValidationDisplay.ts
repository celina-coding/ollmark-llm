import { ValidationError } from "../types";

/**
 * Responsable de l'affichage des erreurs de validation du code.
 */
export class ValidationDisplay {
    constructor(
        /**
         * Élément DOM cible pour l'affichage.
         */
        private readonly element: HTMLDivElement
    ) {}

    /**
     * Affiche les erreurs de validation ou un message de succès.
     *
     * @param errors Liste des erreurs de validation détectées.
     */
    displayErrors(errors: ValidationError[]): void {
        if (errors.length === 0) {
            this.element.innerHTML =
                '<div class="validation-success">✓ Code valide - Aucune erreur détectée</div>';
            return;
        }

        const errorsByType = this.groupErrorsBySeverity(errors);
        let html = '<div class="validation-errors"><h4>Erreurs de validation:</h4>';

        if (errorsByType.ERROR) {
            html += this.renderErrorGroup('Erreurs', errorsByType.ERROR);
        }

        if (errorsByType.WARNING) {
            html += this.renderErrorGroup('Avertissements', errorsByType.WARNING, true);
        }

        html += '</div>';
        this.element.innerHTML = html;
    }

    /**
     * Supprime tout affichage de validation.
     */
    clear(): void {
        this.element.innerHTML = '';
    }

    private groupErrorsBySeverity(
        errors: ValidationError[]
    ): Record<string, ValidationError[]> {
        return errors.reduce((acc, error) => {
            if (!acc[error.severity]) acc[error.severity] = [];
            acc[error.severity].push(error);
            return acc;
        }, {} as Record<string, ValidationError[]>);
    }

    private renderErrorGroup(
        title: string,
        errors: ValidationError[],
        isWarning = false
    ): string {
        const className = isWarning ? 'warning-group' : 'error-group';
        let html = `<div class="${className}"><strong>${title}:</strong><ul>`;

        errors.forEach(error => {
            const location = error.line
                ? ` (ligne ${error.line}${error.column ? `, col ${error.column}` : ''})`
                : '';
            html += `<li><span class="error-type">[${error.type}]</span> ${error.message}${location}</li>`;
        });

        html += '</ul></div>';
        return html;
    }
}