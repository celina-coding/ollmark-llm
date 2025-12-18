import { ApiRequestPreview, JsonPreview, MetricsDisplay, PageInfoDisplay, StatusDisplay, ValidationDisplay, ViewManager } from "../components";
import { IApiService, IPenpotMessenger, TransitParser } from "../services";
import { StrategyFactory } from "../strategies/StrategyFactory";
import { AppState } from "../types";

export class AppController {
    private state: AppState = {
        currentCode: "",
        isGenerating: false,
        currentPageContext: null,
        currentFileId: "",
        currentPageId: ""
    };

    constructor(
        private apiService: IApiService,
        private messenger: IPenpotMessenger,
        private transitParser: TransitParser,
        private statusDisplay: StatusDisplay,
        private validationDisplay: ValidationDisplay,
        private metricsDisplay: MetricsDisplay,
        private pageInfoDisplay: PageInfoDisplay,
        private viewManager: ViewManager,
        private jsonPreview: JsonPreview,
        private apiRequestPreview: ApiRequestPreview,
        private elements: {
            promptInput: HTMLTextAreaElement;
            modificationInput: HTMLTextAreaElement;
            generateBtn: HTMLButtonElement;
            executeBtn: HTMLButtonElement;
            clearBtn: HTMLButtonElement;
            exportBtn: HTMLButtonElement;
            codeOutput: HTMLPreElement;
            strategySelect: HTMLSelectElement;
            includeValidationCreation: HTMLInputElement;
            cleanCodeCreation: HTMLInputElement;
            includeValidationModification: HTMLInputElement;
            cleanCodeModification: HTMLInputElement;
        }
    ) {
        this.initializeEventListeners();
        this.initializeMessaging();
    }

    private initializeEventListeners(): void {
        this.elements.generateBtn.addEventListener("click", () => this.generateCode());
        this.elements.executeBtn.addEventListener("click", () => this.executeCode());
        this.elements.clearBtn.addEventListener("click", () => this.clearAll());
        this.elements.exportBtn.addEventListener("click", () => this.exportPageContext());
        this.elements.strategySelect.addEventListener("change", () => this.handleStrategyChange());

        const handleCtrlEnter = (e: KeyboardEvent) => {
            if (e.ctrlKey && e.key === "Enter") {
                e.preventDefault();
                this.generateCode();
            }
        };

        this.elements.promptInput.addEventListener("keydown", handleCtrlEnter);
        this.elements.modificationInput.addEventListener("keydown", handleCtrlEnter);
    }

    private initializeMessaging(): void {
        this.messenger.onMessage((event) => {
            if (event.data.source === "penpot") {
                this.handlePenpotMessage(event.data);
            } else if (event.data.type === "execution-result") {
                this.handleExecutionResult(event.data.result);
            }
        });

        this.messenger.requestState();
        this.handleStrategyChange();
    }

    private handlePenpotMessage(data: any): void {
        switch (data.type) {
            case "themechange":
                document.body.dataset.theme = data.theme;
                break;
            case "file-id":
                this.state.currentFileId = data.id;
                this.updatePageInfo();
                break;
            case "page-id":
                this.state.currentPageId = data.id;
                this.updatePageInfo();
                break;
        }
    }

    private handleExecutionResult(result: { success: boolean; error?: string }): void {
        this.elements.executeBtn.disabled = false;

        if (result.success) {
            this.statusDisplay.show("✓ Code exécuté avec succès dans Penpot!", "success");
        } else {
            this.statusDisplay.show(`❌ Erreur d'exécution: ${result.error}`, "error");
        }
    }

    private updatePageInfo(): void {
        this.pageInfoDisplay.update(
            this.state.currentFileId,
            this.state.currentPageId,
            !!this.state.currentPageContext
        );
    }

    async generateCode(): Promise<void> {
        const strategy = StrategyFactory.create(
            this.elements.strategySelect.value,
            this.elements.promptInput,
            this.elements.modificationInput,
            this.state.currentPageContext
        );

        const validation = strategy.validate();
        if (!validation.valid) {
            this.statusDisplay.show(validation.message!, "error");
            return;
        }

        if (this.state.isGenerating) return;

        this.setGeneratingState(true);

        try {
            const isModification = this.elements.strategySelect.value === 'modification';
            const includeValidation = isModification 
                ? this.elements.includeValidationModification.checked
                : this.elements.includeValidationCreation.checked;
            const cleanCode = isModification
                ? this.elements.cleanCodeModification.checked
                : this.elements.cleanCodeCreation.checked;

            const requestBody = strategy.getRequestBody(includeValidation, cleanCode);
            this.apiRequestPreview.show(requestBody);

            const data = await this.apiService.generateCode(requestBody);
            this.handleGenerationSuccess(data);

        } catch (error) {
            this.handleGenerationError(error);
        } finally {
            this.setGeneratingState(false);
        }
    }

    private setGeneratingState(isGenerating: boolean): void {
        this.state.isGenerating = isGenerating;
        this.elements.generateBtn.disabled = isGenerating;
        
        // Ne désactiver executeBtn que si on commence la génération
        // Sinon, laisser handleGenerationSuccess l'activer
        if (isGenerating) {
            this.elements.executeBtn.disabled = true;
            this.statusDisplay.show("Génération du code en cours...", "info");
            this.elements.codeOutput.textContent = "⏳ Génération...";
            this.validationDisplay.clear();
            this.metricsDisplay.clear();
        }
    }

    private handleGenerationSuccess(data: any): void {
        this.state.currentCode = data.generatedCode;
        this.elements.codeOutput.textContent = this.state.currentCode;
        this.metricsDisplay.display(data);

        if (data.validationErrors && data.validationErrors.length > 0) {
            this.validationDisplay.displayErrors(data.validationErrors);
            this.statusDisplay.show(
                data.valid 
                    ? "✓ Code généré avec succès (avec avertissements)" 
                    : "⚠️ Code généré mais invalide - Vérifiez les erreurs",
                data.valid ? "warning" : "error"
            );
        } else {
            this.validationDisplay.displayErrors([]);
            this.statusDisplay.show("✓ Code généré avec succès", "success");
        }

        this.elements.executeBtn.disabled = false;
    }

    private handleGenerationError(error: unknown): void {
        console.error('Erreur lors de la génération:', error);
        this.statusDisplay.show(
            `❌ Erreur: ${error instanceof Error ? error.message : 'Erreur inconnue'}`,
            "error"
        );
        this.elements.codeOutput.textContent = "// Erreur lors de la génération du code";
    }

    private executeCode(): void {
        if (!this.state.currentCode) {
            this.statusDisplay.show("Aucun code à exécuter", "error");
            return;
        }

        this.elements.executeBtn.disabled = true;
        this.statusDisplay.show("Exécution du code dans Penpot...", "info");
        this.messenger.executeCode(this.state.currentCode);
    }

    private clearAll(): void {
        this.elements.promptInput.value = "";
        this.elements.modificationInput.value = "";
        this.elements.codeOutput.textContent = "// Le code généré apparaîtra ici";
        this.state.currentCode = "";
        this.validationDisplay.clear();
        this.metricsDisplay.clear();
        this.statusDisplay.hide();
        this.elements.executeBtn.disabled = true;
    }

    private handleStrategyChange(): void {
        const strategy = this.elements.strategySelect.value;

        if (strategy === "modification") {
            this.viewManager.showModificationView();

            if (!this.state.currentPageContext) {
                this.elements.generateBtn.disabled = true;
                this.statusDisplay.show("ℹ️ Pour la stratégie MODIFICATION, exportez d'abord la page", "info");
            } else {
                this.elements.generateBtn.disabled = false;
            }
        } else {
            this.viewManager.showCreationView();
            this.elements.generateBtn.disabled = false;
            this.statusDisplay.hide();
            this.jsonPreview.hide();
        }

        this.apiRequestPreview.hide();
    }

    async exportPageContext(): Promise<void> {
        if (!this.state.currentFileId) {
            this.statusDisplay.show("❌ Aucun fichier Penpot détecté. Ouvrez un fichier d'abord.", "error");
            return;
        }

        this.elements.exportBtn.disabled = true;
        this.statusDisplay.show("🔄 Export du contexte de la page en cours...", "info");

        try {
            // 1. Récupérer les données du fichier via l'API RPC Penpot
            const rawResponse = await this.fetchFileData(this.state.currentFileId);

            // 2. Parser la réponse Transit
            const parsedResponse = this.transitParser.parse(rawResponse);
            
            if (!parsedResponse?.data) {
                throw new Error("Structure de fichier invalide");
            }

            // 3. Extraire l'index des pages
            const pagesIndex = parsedResponse.data.pagesIndex || parsedResponse.data['pages-index'];
            
            if (!pagesIndex) {
                throw new Error("Index des pages non trouvé");
            }

            // 4. Trouver et parser la page courante
            const pageDataRaw = this.findPageData(pagesIndex, parsedResponse.data);
            
            if (!pageDataRaw) {
                throw new Error("Impossible de trouver les données de la page");
            }

            // 5. Parser et normaliser la page
            const pageData = this.transitParser.parse(pageDataRaw);
            const normalizedPage = this.transitParser.normalizePage(pageData);

            if (!normalizedPage.id || !normalizedPage.name) {
                throw new Error("Données de page invalides après normalisation");
            }

            // 6. Stocker et afficher
            this.state.currentPageContext = normalizedPage;
            const objectsCount = normalizedPage.objects ? Object.keys(normalizedPage.objects).length : 0;

            this.jsonPreview.show(normalizedPage);
            this.statusDisplay.show("✓ Contexte de la page exporté avec succès", "success");
            
            console.log('Contexte exporté:', {
                id: normalizedPage.id,
                name: normalizedPage.name,
                objectsCount
            });

            this.updatePageInfo();
            
            if (this.elements.strategySelect.value === "modification") {
                this.elements.generateBtn.disabled = false;
            }
        } catch (error) {
            console.error('Erreur lors de l\'export:', error);
            this.statusDisplay.show(
                `❌ Erreur d'export: ${error instanceof Error ? error.message : 'Erreur inconnue'}`,
                "error"
            );
            this.state.currentPageContext = null;
            this.jsonPreview.hide();
        } finally {
            this.elements.exportBtn.disabled = false;
        }
    }

    private async fetchFileData(fileId: string): Promise<any> {
        const response = await fetch('/api/rpc/command/get-file', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            credentials: 'include',
            body: JSON.stringify({ id: fileId })
        });

        if (!response.ok) {
            throw new Error(`Erreur HTTP: ${response.status}`);
        }

        return response.json();
    }

    private findPageData(pagesIndex: any, data: any): any {
        // Essayer avec le currentPageId
        if (this.state.currentPageId && pagesIndex[this.state.currentPageId]) {
            return pagesIndex[this.state.currentPageId];
        }

        // Essayer avec la première page de la liste
        const pages = data.pages || [];
        if (pages.length > 0 && pagesIndex[pages[0]]) {
            return pagesIndex[pages[0]];
        }

        // En dernier recours, prendre la première clé disponible
        const firstKey = Object.keys(pagesIndex)[0];
        return firstKey ? pagesIndex[firstKey] : null;
    }
}