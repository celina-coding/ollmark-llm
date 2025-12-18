import { MessageHandler } from "./handlers";
import { CodeExecutor, PageExporter, StateMessenger } from "./services"
import { PenpotMessage } from "./types";

/**
 * Point d'entrée principal du plugin Penpot.
 *
 * Ordonne l'initialisation, la communication UI
 * et la gestion des événements globaux.
 */
export class PluginController {
    private readonly messageHandler: MessageHandler;
    private readonly stateMessenger: StateMessenger;

    constructor() {
        const codeExecutor = new CodeExecutor();
        const pageExporter = new PageExporter();
        this.stateMessenger = new StateMessenger();

        this.messageHandler = new MessageHandler(
            codeExecutor,
            pageExporter,
            this.stateMessenger
        );

        this.initialize();
    }

    private initialize(): void {
        this.openUI();
        this.setupMessageListener();
        this.setupEventListeners();
        this.sendInitialState();
    }

    private openUI(): void {
        penpot.ui.open(
            'AI Code Generator',
            `?theme=${penpot.theme}`,
            { width: 600, height: 750 }
        );
    }

    private setupMessageListener(): void {
        penpot.ui.onMessage<PenpotMessage>(message =>
            this.messageHandler.handle(message)
        );
    }

    private setupEventListeners(): void {
        penpot.on('themechange', theme =>
            this.stateMessenger.sendThemeChange(theme)
        );

        penpot.on('filechange', () =>
            this.stateMessenger.sendCurrentState()
        );

        penpot.on('pagechange', () =>
            this.stateMessenger.sendCurrentState()
        );
    }

    private sendInitialState(): void {
        setTimeout(() =>
            this.stateMessenger.sendCurrentState(), 100
        );
    }
}