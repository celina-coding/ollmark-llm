import { ApiRequestPreview, JsonPreview, MetricsDisplay, PageInfoDisplay, StatusDisplay, ValidationDisplay, ViewManager } from "./components";
import { AppController } from "./controllers/AppController";
import { ApiService, IApiService, IPenpotMessenger, PenpotMessenger, TransitParser } from "./services";
import "./style.css";

// Configuration
const API_BASE_URL = "http://localhost:8080/api/penpot";

// Initialisation du thème
const searchParams = new URLSearchParams(window.location.search);
document.body.dataset.theme = searchParams.get("theme") ?? "light";

// Récupération des éléments DOM
const elements = {
    promptInput: document.getElementById("prompt-input") as HTMLTextAreaElement,
    modificationInput: document.getElementById("modification-input") as HTMLTextAreaElement,
    generateBtn: document.getElementById("generate-btn") as HTMLButtonElement,
    executeBtn: document.getElementById("execute-btn") as HTMLButtonElement,
    clearBtn: document.getElementById("clear-btn") as HTMLButtonElement,
    exportBtn: document.getElementById("export-btn") as HTMLButtonElement,
    codeOutput: document.getElementById("code-output") as HTMLPreElement,
    strategySelect: document.getElementById("strategy-select") as HTMLSelectElement,
    includeValidationCreation: document.getElementById("include-validation-creation") as HTMLInputElement,
    cleanCodeCreation: document.getElementById("clean-code-creation") as HTMLInputElement,
    includeValidationModification: document.getElementById("include-validation-modification") as HTMLInputElement,
    cleanCodeModification: document.getElementById("clean-code-modification") as HTMLInputElement,
};

// Éléments pour les vues
const creationView = document.getElementById("creation-view") as HTMLElement;
const modificationView = document.getElementById("modification-view") as HTMLElement;

// Éléments pour les previews
const jsonPreviewSection = document.getElementById("json-preview-section") as HTMLElement;
const jsonPreviewContent = document.getElementById("json-preview-content") as HTMLPreElement;
const jsonObjectsCount = document.getElementById("json-objects-count") as HTMLElement;

const apiRequestSection = document.getElementById("api-request-section") as HTMLElement;
const apiRequestContent = document.getElementById("api-request-content") as HTMLPreElement;

// Autres éléments
const statusDiv = document.getElementById("status") as HTMLDivElement;
const validationDiv = document.getElementById("validation") as HTMLDivElement;
const metricsDiv = document.getElementById("metrics") as HTMLDivElement;
const pageInfoDiv = document.getElementById("page-info") as HTMLDivElement;

// Initialisation des services et composants
const apiService: IApiService = new ApiService(API_BASE_URL);
const messenger: IPenpotMessenger = new PenpotMessenger();
const transitParser = new TransitParser();
const statusDisplay = new StatusDisplay(statusDiv);
const validationDisplay = new ValidationDisplay(validationDiv);
const metricsDisplay = new MetricsDisplay(metricsDiv);
const pageInfoDisplay = new PageInfoDisplay(pageInfoDiv);
const viewManager = new ViewManager(creationView, modificationView);
const jsonPreview = new JsonPreview(jsonPreviewSection, jsonPreviewContent, jsonObjectsCount);
const apiRequestPreview = new ApiRequestPreview(apiRequestSection, apiRequestContent);

// Initialisation du contrôleur principal
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