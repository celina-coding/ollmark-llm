export interface CodeGenerationRequest {
    prompt: string;
    strategy: string;
    includeValidation: boolean;
    cleanCode: boolean;
    pageContext?: any;
}

export interface ValidationError {
    type: string;
    message: string;
    line?: number;
    column?: number;
    severity: string;
}

export interface CodeGenerationResponse {
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

export type StatusType = 'info' | 'success' | 'error' | 'warning';

export interface AppState {
    currentCode: string;
    isGenerating: boolean;
    currentPageContext: any;
    currentFileId: string;
    currentPageId: string;
}