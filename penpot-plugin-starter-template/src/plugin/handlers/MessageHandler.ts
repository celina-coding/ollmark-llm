import { CodeExecutor, PageExporter, StateMessenger } from "../services";
import { PenpotMessage } from "../types";

/**
 * Commande déclenchée suite à un message Penpot.
 */
export interface IMessageCommand {
    /**
     * Exécute la commande associée.
     */
    execute(): Promise<void>;
}

/**
 * Commande d'exécution de code utilisateur.
 */
export class ExecuteCodeCommand implements IMessageCommand {
    constructor(
        private readonly codeExecutor: CodeExecutor,
        private readonly stateMessenger: StateMessenger,
        private readonly code: string
    ) {}

    async execute(): Promise<void> {
        const result = await this.codeExecutor.execute(this.code);
        this.stateMessenger.sendExecutionResult(result);
    }
}

/**
 * Commande demandant l'état courant du plugin.
 */
export class RequestStateCommand implements IMessageCommand {
    constructor(
        private readonly stateMessenger: StateMessenger
    ) {}

    async execute(): Promise<void> {
        this.stateMessenger.sendCurrentState();
    }
}

/**
 * Commande d'export de la page courante.
 */
export class ExportPageCommand implements IMessageCommand {
    constructor(
        private readonly pageExporter: PageExporter,
        private readonly stateMessenger: StateMessenger
    ) {}

    async execute(): Promise<void> {
        const result = this.pageExporter.export(penpot.currentPage);
        this.stateMessenger.sendPageExportResult(result);
    }
}

/**
 * Route les messages Penpot vers les commandes appropriées.
 */
export class MessageHandler {
    private readonly commands = new Map<
        string,
        (message: PenpotMessage) => IMessageCommand
    >();

    constructor(
        private readonly codeExecutor: CodeExecutor,
        private readonly pageExporter: PageExporter,
        private readonly stateMessenger: StateMessenger
    ) {
        this.registerCommands();
    }

    /**
     * Associe chaque type de message à sa commande.
     */
    private registerCommands(): void {
        this.commands.set('execute-code', message =>
            new ExecuteCodeCommand(
                this.codeExecutor,
                this.stateMessenger,
                message.code!
            )
        );

        this.commands.set('request-state', () =>
            new RequestStateCommand(this.stateMessenger)
        );

        this.commands.set('export-page', () =>
            new ExportPageCommand(
                this.pageExporter,
                this.stateMessenger
            )
        );
    }

    /**
     * Traite un message entrant depuis l'UI Penpot.
     *
     * @param message Message reçu.
     */
    async handle(message: PenpotMessage): Promise<void> {
        const commandFactory = this.commands.get(message.type);
        if (commandFactory) {
            await commandFactory(message).execute();
        }
    }
}