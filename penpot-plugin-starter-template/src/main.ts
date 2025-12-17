import "./style.css";

// Configuration de l'API
const API_BASE_URL = "http://localhost:8080/api/penpot";

// Types
interface CodeGenerationRequest {
  prompt: string;
  strategy: string;
  includeValidation: boolean;
  cleanCode: boolean;
}

interface ValidationError {
  type: string;
  message: string;
  line?: number;
  column?: number;
  severity: string;
}

interface CodeGenerationResponse {
  strategy: string;
  userPrompt: string;
  generatedCode: string;
  valid: boolean;
  validationErrors: ValidationError[];
  codeLength: number;
  generationTimeMs: number;
  enrichedPrompt?: string;
  rawResponse?: string;
  promptTokensEstimate?: number;
}

// Get the current theme from the URL
const searchParams = new URLSearchParams(window.location.search);
document.body.dataset.theme = searchParams.get("theme") ?? "light";

// État de l'application
let currentCode = "";
let isGenerating = false;

// Éléments du DOM
const promptInput = document.getElementById("prompt-input") as HTMLTextAreaElement;
const generateBtn = document.getElementById("generate-btn") as HTMLButtonElement;
const executeBtn = document.getElementById("execute-btn") as HTMLButtonElement;
const clearBtn = document.getElementById("clear-btn") as HTMLButtonElement;
const codeOutput = document.getElementById("code-output") as HTMLPreElement;
const statusDiv = document.getElementById("status") as HTMLDivElement;
const validationDiv = document.getElementById("validation") as HTMLDivElement;
const metricsDiv = document.getElementById("metrics") as HTMLDivElement;
const strategySelect = document.getElementById("strategy-select") as HTMLSelectElement;
const includeValidationCheckbox = document.getElementById("include-validation") as HTMLInputElement;
const cleanCodeCheckbox = document.getElementById("clean-code") as HTMLInputElement;

// Fonction pour afficher le statut
function showStatus(message: string, type: 'info' | 'success' | 'error' | 'warning') {
  statusDiv.textContent = message;
  statusDiv.className = `status status-${type}`;
  statusDiv.style.display = 'block';
}

// Fonction pour masquer le statut
function hideStatus() {
  statusDiv.style.display = 'none';
}

// Fonction pour afficher les erreurs de validation
function displayValidationErrors(errors: ValidationError[]) {
  if (errors.length === 0) {
    validationDiv.innerHTML = '<div class="validation-success">✓ Code valide - Aucune erreur détectée</div>';
    return;
  }

  const errorsByType = errors.reduce((acc, error) => {
    if (!acc[error.severity]) acc[error.severity] = [];
    acc[error.severity].push(error);
    return acc;
  }, {} as Record<string, ValidationError[]>);

  let html = '<div class="validation-errors"><h4>Erreurs de validation:</h4>';
  
  if (errorsByType.ERROR) {
    html += '<div class="error-group"><strong>Erreurs:</strong><ul>';
    errorsByType.ERROR.forEach(error => {
      const location = error.line ? ` (ligne ${error.line}${error.column ? `, col ${error.column}` : ''})` : '';
      html += `<li><span class="error-type">[${error.type}]</span> ${error.message}${location}</li>`;
    });
    html += '</ul></div>';
  }

  if (errorsByType.WARNING) {
    html += '<div class="warning-group"><strong>Avertissements:</strong><ul>';
    errorsByType.WARNING.forEach(error => {
      const location = error.line ? ` (ligne ${error.line}${error.column ? `, col ${error.column}` : ''})` : '';
      html += `<li><span class="error-type">[${error.type}]</span> ${error.message}${location}</li>`;
    });
    html += '</ul></div>';
  }

  html += '</div>';
  validationDiv.innerHTML = html;
}

// Fonction pour afficher les métriques
function displayMetrics(response: CodeGenerationResponse) {
  const metrics = [
    `⏱️ Temps de génération: ${response.generationTimeMs}ms`,
    `📏 Longueur du code: ${response.codeLength} caractères`,
    `🎯 Stratégie: ${response.strategy}`,
  ];

  if (response.promptTokensEstimate) {
    metrics.push(`🔤 Tokens estimés: ${response.promptTokensEstimate}`);
  }

  metricsDiv.innerHTML = '<div class="metrics">' + metrics.join(' | ') + '</div>';
}

// Fonction pour générer le code
async function generateCode() {
  const prompt = promptInput.value.trim();
  
  if (!prompt) {
    showStatus("Veuillez entrer un prompt", "error");
    return;
  }

  if (isGenerating) {
    return;
  }

  isGenerating = true;
  generateBtn.disabled = true;
  executeBtn.disabled = true;
  showStatus("Génération du code en cours...", "info");
  codeOutput.textContent = "⏳ Génération...";
  validationDiv.innerHTML = "";
  metricsDiv.innerHTML = "";

  const requestBody: CodeGenerationRequest = {
    prompt,
    strategy: strategySelect.value,
    includeValidation: includeValidationCheckbox.checked,
    cleanCode: cleanCodeCheckbox.checked
  };

  try {
    const response = await fetch(`${API_BASE_URL}/generate`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify(requestBody)
    });

    if (!response.ok) {
      const errorData = await response.json().catch(() => null);
      throw new Error(errorData?.message || `Erreur HTTP: ${response.status}`);
    }

    const data: CodeGenerationResponse = await response.json();
    currentCode = data.generatedCode;
    codeOutput.textContent = currentCode;

    // Afficher les métriques
    displayMetrics(data);

    // Afficher les erreurs de validation
    if (data.validationErrors && data.validationErrors.length > 0) {
      displayValidationErrors(data.validationErrors);
      
      if (data.valid) {
        showStatus("✓ Code généré avec succès (avec avertissements)", "warning");
      } else {
        showStatus("⚠️ Code généré mais invalide - Vérifiez les erreurs", "error");
      }
    } else {
      validationDiv.innerHTML = '<div class="validation-success">✓ Code valide - Aucune erreur détectée</div>';
      showStatus("✓ Code généré avec succès", "success");
    }

    executeBtn.disabled = false;

  } catch (error) {
    console.error('Erreur lors de la génération:', error);
    showStatus(`❌ Erreur: ${error instanceof Error ? error.message : 'Erreur inconnue'}`, "error");
    codeOutput.textContent = "// Erreur lors de la génération du code";
  } finally {
    isGenerating = false;
    generateBtn.disabled = false;
  }
}

// Fonction pour exécuter le code dans Penpot
function executeCode() {
  if (!currentCode) {
    showStatus("Aucun code à exécuter", "error");
    return;
  }

  executeBtn.disabled = true;
  showStatus("Exécution du code dans Penpot...", "info");

  // Envoyer le code au plugin.ts pour exécution
  parent.postMessage({
    type: 'execute-code',
    code: currentCode
  }, "*");
}

// Fonction pour effacer
function clearAll() {
  promptInput.value = "";
  codeOutput.textContent = "// Le code généré apparaîtra ici";
  currentCode = "";
  validationDiv.innerHTML = "";
  metricsDiv.innerHTML = "";
  hideStatus();
  executeBtn.disabled = true;
}

// Écouteurs d'événements
generateBtn.addEventListener("click", generateCode);
executeBtn.addEventListener("click", executeCode);
clearBtn.addEventListener("click", clearAll);

// Permettre Ctrl+Enter pour générer
promptInput.addEventListener("keydown", (e) => {
  if (e.ctrlKey && e.key === "Enter") {
    e.preventDefault();
    generateCode();
  }
});

// Écouter les messages du plugin.ts
window.addEventListener("message", (event) => {
  if (event.data.source === "penpot") {
    if (event.data.type === "themechange") {
      document.body.dataset.theme = event.data.theme;
    }
  } else if (event.data.type === "execution-result") {
    executeBtn.disabled = false;
    
    if (event.data.result.success) {
      showStatus("✓ Code exécuté avec succès dans Penpot!", "success");
    } else {
      showStatus(`❌ Erreur d'exécution: ${event.data.result.error}`, "error");
    }
  }
});
