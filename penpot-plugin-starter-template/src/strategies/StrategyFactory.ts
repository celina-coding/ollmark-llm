import { CodeGenerationRequest } from "../types";

export interface IGenerationStrategy {
    validate(): { valid: boolean; message?: string };
    getPrompt(): string;
    getRequestBody(includeValidation: boolean, cleanCode: boolean): CodeGenerationRequest;
}

export class CreationStrategy implements IGenerationStrategy {
    constructor(private promptInput: HTMLTextAreaElement) {}

    validate(): { valid: boolean; message?: string } {
        const prompt = this.promptInput.value.trim();
        if (!prompt) {
            return { valid: false, message: "Veuillez entrer un prompt" };
        }
        return { valid: true };
    }

    getPrompt(): string {
        return this.promptInput.value.trim();
    }

    getRequestBody(includeValidation: boolean, cleanCode: boolean): CodeGenerationRequest {
        return {
            prompt: this.getPrompt(),
            strategy: 'creation',
            includeValidation,
            cleanCode
        };
    }
}

export class ModificationStrategy implements IGenerationStrategy {
    constructor(
        private modificationInput: HTMLTextAreaElement,
        private pageContext: any
    ) {}

    validate(): { valid: boolean; message?: string } {
        const prompt = this.modificationInput.value.trim();
        if (!prompt) {
            return { valid: false, message: "Veuillez entrer un prompt" };
        }

        if (!this.pageContext) {
            return { valid: false, message: "❌ Stratégie MODIFICATION: vous devez d'abord exporter la page" };
        }

        return { valid: true };
    }

    getPrompt(): string {
        return this.modificationInput.value.trim();
    }

    getRequestBody(includeValidation: boolean, cleanCode: boolean): CodeGenerationRequest {
        return {
            prompt: this.getPrompt(),
            strategy: 'modification',
            includeValidation,
            cleanCode,
            pageContext: this.pageContext
        };
    }
}

export class StrategyFactory {
    static create(
        strategyType: string,
        promptInput: HTMLTextAreaElement,
        modificationInput: HTMLTextAreaElement,
        pageContext: any
    ): IGenerationStrategy {
        if (strategyType === 'modification') {
            return new ModificationStrategy(modificationInput, pageContext);
        }
        return new CreationStrategy(promptInput);
    }
}