/**
 * Point d'entrée principal de l'application AI Code Generator pour Penpot
 * 
 * Ce fichier initialise tous les services, composants d'affichage et le contrôleur principal.
 * Il configure également le thème de l'application et récupère les éléments DOM nécessaires.
 * 
 * @module main
 */

import { 
    ApiRequestPreview, 
    JsonPreview, 
    MetricsDisplay, 
    PageInfoDisplay, 
    StatusDisplay, 
    ValidationDisplay, 
    ViewManager 
} from "./components";
import { AppController } from "./controllers/AppController";
import { 
    ApiService, 
    IApiService, 
    IPenpotMessenger, 
    PenpotMessenger, 
    TransitParser 
} from "./services";
import "./style.css";

/** URL de base de l'API backend pour la génération de code */
const API_BASE_URL = "http://localhost:8080/api/penpot";

// ============================================================================
// Initialisation du thème
// ============================================================================

const searchParams = new URLSearchParams(window.location.search);
document.body.dataset.theme = searchParams.get("theme") ?? "light";

// ============================================================================
// Types
// ============================================================================

/**
 * Interface décrivant tous les éléments DOM requis par l'application
 */
interface AppElements {
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

// ============================================================================
// Récupération des éléments DOM
// ============================================================================

/**
 * Récupère un élément DOM et vérifie son existence
 * @param id - L'identifiant de l'élément
 * @param type - Le type attendu de l'élément (pour le message d'erreur)
 * @throws {Error} Si l'élément n'existe pas
 */
function getRequiredElement<T extends HTMLElement>(id: string, type: string): T {
    const element = document.getElementById(id) as T | null;
    if (!element) {
        throw new Error(`Élément requis non trouvé: ${id} (${type})`);
    }
    return element;
}

/** Éléments de contrôle et d'affichage principaux */
const elements: AppElements = {
    promptInput: getRequiredElement<HTMLTextAreaElement>("prompt-input", "textarea"),
    modificationInput: getRequiredElement<HTMLTextAreaElement>("modification-input", "textarea"),
    generateBtn: getRequiredElement<HTMLButtonElement>("generate-btn", "button"),
    executeBtn: getRequiredElement<HTMLButtonElement>("execute-btn", "button"),
    clearBtn: getRequiredElement<HTMLButtonElement>("clear-btn", "button"),
    exportBtn: getRequiredElement<HTMLButtonElement>("export-btn", "button"),
    codeOutput: getRequiredElement<HTMLPreElement>("code-output", "pre"),
    strategySelect: getRequiredElement<HTMLSelectElement>("strategy-select", "select"),
    includeValidationCreation: getRequiredElement<HTMLInputElement>("include-validation-creation", "input"),
    cleanCodeCreation: getRequiredElement<HTMLInputElement>("clean-code-creation", "input"),
    includeValidationModification: getRequiredElement<HTMLInputElement>("include-validation-modification", "input"),
    cleanCodeModification: getRequiredElement<HTMLInputElement>("clean-code-modification", "input"),
};

/** Éléments pour les vues création et modification */
const viewElements = {
    creation: getRequiredElement<HTMLElement>("creation-view", "div"),
    modification: getRequiredElement<HTMLElement>("modification-view", "div"),
};

/** Éléments pour les previews JSON et requête API */
const previewElements = {
    jsonSection: getRequiredElement<HTMLElement>("json-preview-section", "details"),
    jsonContent: getRequiredElement<HTMLPreElement>("json-preview-content", "pre"),
    jsonCount: getRequiredElement<HTMLElement>("json-objects-count", "span"),
    apiRequestSection: getRequiredElement<HTMLElement>("api-request-section", "details"),
    apiRequestContent: getRequiredElement<HTMLPreElement>("api-request-content", "pre"),
};

/** Éléments pour l'affichage du statut et des informations */
const displayElements = {
    status: getRequiredElement<HTMLDivElement>("status", "div"),
    validation: getRequiredElement<HTMLDivElement>("validation", "div"),
    metrics: getRequiredElement<HTMLDivElement>("metrics", "div"),
    pageInfo: getRequiredElement<HTMLDivElement>("page-info", "div"),
};

// ============================================================================
// Initialisation des services
// ============================================================================

/** Service de communication avec l'API backend */
const apiService: IApiService = new ApiService(API_BASE_URL);

/** Service de messagerie avec le plugin Penpot parent */
const messenger: IPenpotMessenger = new PenpotMessenger();

/** Parser pour convertir le format Transit de Penpot en JSON standard */
const transitParser = new TransitParser();

// ============================================================================
// Initialisation des composants d'affichage
// ============================================================================

/** Composant d'affichage du statut des opérations */
const statusDisplay = new StatusDisplay(displayElements.status);

/** Composant d'affichage des erreurs de validation */
const validationDisplay = new ValidationDisplay(displayElements.validation);

/** Composant d'affichage des métriques de génération */
const metricsDisplay = new MetricsDisplay(displayElements.metrics);

/** Composant d'affichage des informations de page */
const pageInfoDisplay = new PageInfoDisplay(displayElements.pageInfo);

/** Gestionnaire des vues création/modification */
const viewManager = new ViewManager(viewElements.creation, viewElements.modification);

/** Composant de preview du JSON exporté */
const jsonPreview = new JsonPreview(
    previewElements.jsonSection,
    previewElements.jsonContent,
    previewElements.jsonCount
);

/** Composant de preview des requêtes API */
const apiRequestPreview = new ApiRequestPreview(
    previewElements.apiRequestSection,
    previewElements.apiRequestContent
);

/**
 * Initialise le contrôleur principal de l'application
 * 
 * Le contrôleur orchestre tous les services et composants pour:
 * - Générer du code Penpot via IA
 * - Exporter le contexte de page
 * - Exécuter le code généré dans Penpot
 * - Gérer les interactions utilisateur
 */
new AppController(
    apiService,
    messenger,
    transitParser,
    statusDisplay,
    validationDisplay,
    metricsDisplay,
    pageInfoDisplay,
    viewManager,
    jsonPreview,
    apiRequestPreview,
    elements
);