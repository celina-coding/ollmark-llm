import { ApiRequestPreview, JsonPreview, MetricsDisplay, PageInfoDisplay, StatusDisplay, ValidationDisplay, ViewManager } from "../components";
import { IApiService, IPenpotMessenger, TransitParser } from "../services";
import { StrategyFactory } from "../strategies/StrategyFactory";
import { AppState } from "../types";

/**
 * Contrôleur principal de l'application AI Code Generator
 * 
 * Responsabilités:
 * - Orchestrer la génération de code via l'API
 * - Gérer l'export et le parsing du contexte de page Penpot
 * - Coordonner les interactions entre les services et les composants d'affichage
 * - Gérer le cycle de vie de l'application (génération, exécution, nettoyage)
 * 
 * @example
 * ```typescript
 * const controller = new AppController(
 *   apiService,
 *   messenger,
 *   transitParser,
 *   statusDisplay,
 *   validationDisplay,
 *   metricsDisplay,
 *   pageInfoDisplay,
 *   viewManager,
 *   jsonPreview,
 *   apiRequestPreview,
 *   elements
 * );
 * ```
 */
export class AppController {
    /**
     * État actuel de l'application
     * 
     * Contient:
     * - Le code généré en attente d'exécution
     * - Le flag de génération en cours
     * - Le contexte de page exporté (pour la stratégie MODIFICATION)
     * - Les identifiants du fichier et de la page courants
     */
    private state: AppState = {
        currentCode: "",
        isGenerating: false,
        currentPageContext: null,
        currentFileId: "",
        currentPageId: ""
    };

    /**
     * Initialise le contrôleur et injecte toutes les dépendances
     * 
     * @param apiService - Service de communication avec l'API backend
     * @param messenger - Service de messagerie avec le plugin Penpot parent
     * @param transitParser - Parser pour convertir le format Transit en JSON
     * @param statusDisplay - Composant d'affichage du statut
     * @param validationDisplay - Composant d'affichage des erreurs de validation
     * @param metricsDisplay - Composant d'affichage des métriques
     * @param pageInfoDisplay - Composant d'affichage des infos de page
     * @param viewManager - Gestionnaire des vues (création/modification)
     * @param jsonPreview - Composant de preview du JSON exporté
     * @param apiRequestPreview - Composant de preview des requêtes API
     * @param elements - Références aux éléments DOM de l'interface
     */
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

    /**
     * Configure tous les écouteurs d'événements de l'interface
     * 
     * Gère:
     * - Les clics sur les boutons (générer, exécuter, effacer, exporter)
     * - Le changement de stratégie (création/modification)
     * - Les raccourcis clavier (Ctrl+Enter pour générer)
     */
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

    /**
     * Configure la communication avec le plugin Penpot parent
     * 
     * Écoute les messages pour:
     * - Les changements de thème
     * - Les changements de fichier/page
     * - Les résultats d'exécution du code
     * 
     * Demande l'état initial au démarrage
     */
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

    /**
     * Traite les messages reçus du plugin Penpot parent
     * 
     * @param data - Données du message contenant le type et les informations
     */
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

    /**
     * Génère du code Penpot via l'API en utilisant la stratégie sélectionnée
     * 
     * Flux:
     * 1. Valide la stratégie (création ou modification)
     * 2. Prépare la requête avec les options (validation, nettoyage)
     * 3. Envoie la requête à l'API
     * 4. Affiche le code généré et les métriques
     * 5. Active le bouton d'exécution si succès
     * 
     * @throws {Error} Si la génération échoue ou si la validation est invalide
     */
    async generateCode(): Promise<void> {
        const strategy = StrategyFactory.create(
            this.elements.strategySelect.value as "creation" | "modification",
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

    /**
     * Exporte le contexte de la page Penpot courante
     * 
     * Processus:
     * 1. Récupère les données du fichier via l'API RPC Penpot
     * 2. Parse le format Transit en JSON standard
     * 3. Extrait les données de la page courante
     * 4. Normalise au format attendu par l'API de génération
     * 5. Stocke le contexte et active la stratégie MODIFICATION
     * 
     * @throws {Error} Si l'export échoue ou si le format est invalide
     */
    async exportPageContext(): Promise<void> {
        if (!this.state.currentFileId) {
            this.statusDisplay.show("❌ Aucun fichier Penpot détecté. Ouvrez un fichier d'abord.", "error");
            return;
        }

        this.elements.exportBtn.disabled = true;
        this.statusDisplay.show("🔄 Export du contexte de la page en cours...", "info");

        try {
            const rawResponse = await this.fetchFileData(this.state.currentFileId);
            const parsedResponse = this.transitParser.parse(rawResponse);

            if (!parsedResponse?.data) throw new Error("Structure de fichier invalide");

            const pagesIndex = parsedResponse.data.pagesIndex || parsedResponse.data['pages-index'];
            if (!pagesIndex) throw new Error("Index des pages non trouvé");

            const pageDataRaw = this.findPageData(pagesIndex, parsedResponse.data);
            if (!pageDataRaw) throw new Error("Impossible de trouver les données de la page");

            const pageData = this.transitParser.parse(pageDataRaw);
            const normalizedPage = this.transitParser.normalizePage(pageData);
            if (!normalizedPage.id || !normalizedPage.name) {
                throw new Error("Données de page invalides après normalisation");
            }

            this.state.currentPageContext = normalizedPage;
            this.jsonPreview.show(normalizedPage);
            this.statusDisplay.show("✓ Contexte de la page exporté avec succès", "success");
            this.updatePageInfo();

            if (this.elements.strategySelect.value === "modification") {
                this.elements.generateBtn.disabled = false;
            }
        } catch (error) {
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

    /**
     * Récupère les données du fichier Penpot via l'API RPC
     * 
     * @param fileId - Identifiant du fichier Penpot
     * @returns Promesse contenant les données brutes au format Transit
     * @throws {Error} Si la requête échoue ou retourne une erreur HTTP
     */
    private async fetchFileData(fileId: string): Promise<any> {
        const response = await fetch('/api/rpc/command/get-file', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            credentials: 'include',
            body: JSON.stringify({ id: fileId })
        });

        if (!response.ok) throw new Error(`Erreur HTTP: ${response.status}`);
        return response.json();
    }

    /**
     * Recherche les données de la page courante dans l'index des pages
     * 
     * Stratégie de recherche:
     * 1. Essaie avec currentPageId si disponible
     * 2. Essaie avec la première page de la liste
     * 3. Prend la première clé disponible en dernier recours
     * 
     * @param pagesIndex - Index des pages du fichier
     * @param data - Données complètes du fichier
     * @returns Les données brutes de la page ou null si introuvable
     */
    private findPageData(pagesIndex: any, data: any): any {
        if (this.state.currentPageId && pagesIndex[this.state.currentPageId]) {
            return pagesIndex[this.state.currentPageId];
        }

        const pages = data.pages || [];
        if (pages.length > 0 && pagesIndex[pages[0]]) {
            return pagesIndex[pages[0]];
        }

        const firstKey = Object.keys(pagesIndex)[0];
        return firstKey ? pagesIndex[firstKey] : null;
    }
}