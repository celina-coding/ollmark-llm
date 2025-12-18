import { ExecutionResult, PageExportResult } from "../types";

export class StateMessenger {
    sendCurrentState(): void {
        try {
            if (penpot.currentFile?.id) {
                penpot.ui.sendMessage({
                    source: "penpot",
                    type: "file-id",
                    id: penpot.currentFile.id
                });
            }

            if (penpot.currentPage?.id) {
                penpot.ui.sendMessage({
                    source: "penpot",
                    type: "page-id",
                    id: penpot.currentPage.id
                });
            }
        } catch (e) {
            console.error('Erreur lors de l\'envoi de l\'état:', e);
        }
    }

    sendThemeChange(theme: string): void {
        penpot.ui.sendMessage({
            source: "penpot",
            type: "themechange",
            theme,
        });
    }

    sendExecutionResult(result: ExecutionResult): void {
        penpot.ui.sendMessage({
            type: 'execution-result',
            result
        });
    }

    sendPageExportResult(result: PageExportResult): void {
        penpot.ui.sendMessage({
            type: 'page-export-result',
            ...result
        });
    }
}