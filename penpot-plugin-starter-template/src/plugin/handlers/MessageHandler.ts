import { CodeExecutor, PageExporter, StateMessenger } from "../services";
import { PenpotMessage } from "../types";

export interface IMessageCommand {
    execute(): Promise<void>;
}

export class ExecuteCodeCommand implements IMessageCommand {
    constructor(
        private codeExecutor: CodeExecutor,
        private stateMessenger: StateMessenger,
        private code: string
    ) {}

    async execute(): Promise<void> {
        const result = await this.codeExecutor.execute(this.code);
        this.stateMessenger.sendExecutionResult(result);
    }
}

export class RequestStateCommand implements IMessageCommand {
    constructor(private stateMessenger: StateMessenger) {}

    async execute(): Promise<void> {
        this.stateMessenger.sendCurrentState();
    }
}

export class ExportPageCommand implements IMessageCommand {
    constructor(
        private pageExporter: PageExporter,
        private stateMessenger: StateMessenger
    ) {}

    async execute(): Promise<void> {
        const result = this.pageExporter.export(penpot.currentPage);
        this.stateMessenger.sendPageExportResult(result);
    }
}

export class MessageHandler {
    private commands: Map<string, (message: PenpotMessage) => IMessageCommand>;

    constructor(
        private codeExecutor: CodeExecutor,
        private pageExporter: PageExporter,
        private stateMessenger: StateMessenger
    ) {
        this.commands = new Map();
        this.registerCommands();
    }

    private registerCommands(): void {
        this.commands.set('execute-code', (message) => 
            new ExecuteCodeCommand(this.codeExecutor, this.stateMessenger, message.code!)
        );

        this.commands.set('request-state', () => 
            new RequestStateCommand(this.stateMessenger)
        );

        this.commands.set('export-page', () => 
            new ExportPageCommand(this.pageExporter, this.stateMessenger)
        );
    }

    async handle(message: PenpotMessage): Promise<void> {
        const commandFactory = this.commands.get(message.type);

        if (commandFactory) {
            const command = commandFactory(message);
            await command.execute();
        }
    }
}