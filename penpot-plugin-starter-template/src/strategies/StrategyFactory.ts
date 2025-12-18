import { CodeGenerationRequest, ValidationResult } from "../types";

/**
 * Contrat commun à toutes les stratégies de génération de code.
 *
 * Une stratégie est responsable de :
 * - valider les entrées utilisateur
 * - fournir le prompt final
 * - construire la requête envoyée au moteur de génération
 */
export interface IGenerationStrategy {
    /**
     * Valide les entrées nécessaires à la stratégie.
     *
     * @returns Résultat de validation avec message explicatif en cas d'échec.
     */
    validate(): ValidationResult;

    /**
     * Retourne le prompt utilisateur nettoyé et prêt à être envoyé.
     *
     * @returns Prompt final de génération.
     */
    getPrompt(): string;

    /**
     * Construit le corps de la requête de génération.
     *
     * @param includeValidation Indique si la validation doit être incluse dans la génération.
     * @param cleanCode Indique si le code généré doit être nettoyé/formatté.
     * @returns Requête prête à être envoyée à l'API de génération.
     */
    getRequestBody(
        includeValidation: boolean,
        cleanCode: boolean
    ): CodeGenerationRequest;
}

/**
 * Stratégie de génération basée sur la création de code à partir d'un prompt libre.
 *
 * Utilisée lorsqu'aucun contexte de page existante n'est nécessaire.
 */
export class CreationStrategy implements IGenerationStrategy {
    constructor(
        /**
         * Champ de saisie contenant le prompt utilisateur.
         */
        private readonly promptInput: HTMLTextAreaElement
    ) {}

    validate(): ValidationResult {
        const prompt = this.promptInput.value.trim();

        if (!prompt) {
            return { valid: false, message: "Veuillez entrer un prompt" };
        }

        return { valid: true };
    }

    getPrompt(): string {
        return this.promptInput.value.trim();
    }

    getRequestBody(
        includeValidation: boolean,
        cleanCode: boolean
    ): CodeGenerationRequest {
        return {
            prompt: this.getPrompt(),
            strategy: "creation",
            includeValidation,
            cleanCode
        };
    }
}

/**
 * Stratégie de génération basée sur la modification d'un code existant.
 *
 * Nécessite un contexte de page préalablement exporté.
 */
export class ModificationStrategy implements IGenerationStrategy {
    constructor(
        /**
         * Champ de saisie contenant le prompt de modification.
         */
        private readonly modificationInput: HTMLTextAreaElement,

        /**
         * Contexte de la page existante utilisé pour guider la modification.
         */
        private readonly pageContext: any
    ) {}

    validate(): ValidationResult {
        const prompt = this.modificationInput.value.trim();

        if (!prompt) {
            return { valid: false, message: "Veuillez entrer un prompt" };
        }

        if (!this.pageContext) {
            return {
                valid: false,
                message: "Vous devez d'abord exporter la page"
            };
        }

        return { valid: true };
    }

    getPrompt(): string {
        return this.modificationInput.value.trim();
    }

    getRequestBody(
        includeValidation: boolean,
        cleanCode: boolean
    ): CodeGenerationRequest {
        return {
            prompt: this.getPrompt(),
            strategy: "modification",
            includeValidation,
            cleanCode,
            pageContext: this.pageContext
        };
    }
}

/**
 * Fabrique responsable de l'instanciation des stratégies de génération.
 *
 * Centralise la logique de sélection afin d'éviter des conditions
 * dispersées dans le reste de l'application.
 */
export class StrategyFactory {

    /**
     * Crée la stratégie de génération appropriée selon le type demandé.
     *
     * @param strategyType Type de stratégie à instancier.
     * @param promptInput Champ de saisie pour la création.
     * @param modificationInput Champ de saisie pour la modification.
     * @param pageContext Contexte de page nécessaire à la modification.
     *
     * @returns Stratégie de génération prête à l'emploi.
     */
    static create(
        strategyType: "creation" | "modification",
        promptInput: HTMLTextAreaElement,
        modificationInput: HTMLTextAreaElement,
        pageContext: any
    ): IGenerationStrategy {
        if (strategyType === "modification") {
            return new ModificationStrategy(modificationInput, pageContext);
        }

        return new CreationStrategy(promptInput);
    }
}