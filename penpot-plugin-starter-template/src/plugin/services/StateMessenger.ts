import { ExecutionResult, PageExportResult } from "../types";

/**
 * Centralise l'envoi d'événements vers l'UI Penpot.
 */
export class StateMessenger {
    /**
     * Envoie l'état courant (fichier / page).
     */
    sendCurrentState(): void {
        if (penpot.currentFile?.id) {
            penpot.ui.sendMessage({
                source: "penpot",
                type: 'file-id',
                id: penpot.currentFile.id
            });
        }

        if (penpot.currentPage?.id) {
            penpot.ui.sendMessage({
                source: "penpot",
                type: 'page-id',
                id: penpot.currentPage.id
            });
        }
    }

    /**
     * Notifie un changement de thème.
     *
     * @param theme Thème actif.
     */
    sendThemeChange(theme: string): void {
        penpot.ui.sendMessage({
            source: "penpot",
            type: 'themechange',
            theme
        });
    }

    /**
     * Envoie le résultat d'exécution du code.
     *
     * @param result Résultat d'exécution.
     */
    sendExecutionResult(result: ExecutionResult): void {
        penpot.ui.sendMessage({
            type: 'execution-result',
            result
        });
    }

    /**
     * Envoie le résultat d'export de page.
     *
     * @param result Résultat d'export.
     */
    sendPageExportResult(result: PageExportResult): void {
        penpot.ui.sendMessage({
            type: 'page-export-result',
            ...result
        });
    }
}